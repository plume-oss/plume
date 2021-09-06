/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.passes

import io.github.plume.oss.Traversals
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.DiffGraphUtil
import io.github.plume.oss.util.ProgressBarUtil
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.passes.DiffGraph
import io.shiftleft.passes.ParallelCpgPass
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Config
import overflowdb.Graph
import scala.jdk.CollectionConverters
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * Runs passes from [io.shiftleft.dataflowengineoss.passes] over method bodies.
 */
class DataFlowPass(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(DataFlowPass::javaClass)

    /**
     * Calls data flow passes. Converges all methods into a local OverflowDB graph instance. This is done concurrently.
     */
    fun runPass() {
        runBlocking {
            val bufferedChannel = Channel<DeltaGraph>()
            val g = Graph.open(Config.withDefaults(), NodeFactories.allAsJava(), EdgeFactories.allAsJava())
            // Producer
            PlumeStorage.methodCpgs.values.forEach { mg -> launch { bufferedChannel.send(mg) } }
            // Single consumer
            launch {
                repeat(PlumeStorage.methodCpgs.size) { bufferedChannel.receive().toOverflowDb(g) }
                bufferedChannel.close()
            }.join() // Suspend until the channel is fully consumed.
            // Run passes
            launch {
                Cpg.apply(g).use { cpg ->
                    val methods = g.nodes(NodeTypes.METHOD).asSequence().toList()
                    val maxDefinitions = Traversals.getMaxNumberOfDefinitionsFromAMethod(g)
                    runParallelPass(methods.filterIsInstance<Method>(), ReachingDefPass(cpg, maxDefinitions))
                }
            }
        }
    }

    /**
     * Pass the parts in a parallel stream while catching any exceptions and logging them.
     */
    private fun <T> runParallelPass(parts: List<T>, pass: ParallelCpgPass<T>) {
        val channel = Channel<List<DeltaGraph>>()
        try {
            runBlocking {
                // Producer
                launch {
                    parts.parallelStream().map { part ->
                        val out = mutableListOf<DiffGraph>()
                        val dfs = runCatching { pass.runOnPart(part) }
                                .onFailure { e -> logger.warn("Exception encountered while running parallel pass.", e) }
                                .getOrNull()
                        if (dfs != null)
                            CollectionConverters.IteratorHasAsJava(dfs)
                                    .asJava()
                                    .asSequence()
                                    .toCollection(out)
                        out.map(DiffGraphUtil::toDeltaGraph).toList()
                    }.forEach { dgs -> launch { channel.send(dgs) } }
                }
                // Consumer
                ProgressBarUtil.runInsideProgressBar(
                        logger.level,
                        "Data Flow Pass",
                        parts.size.toLong()
                ) { pb ->
                    repeat(parts.size) {
                        runBlocking {
                            val dgs = channel.receive()
                            dgs.forEach(driver::bulkTransaction)
                            pb?.step()
                        }
                    }
                }
            }
        } finally {
            channel.close()
        }
    }


}