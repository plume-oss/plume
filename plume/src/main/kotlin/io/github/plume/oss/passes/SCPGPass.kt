package io.github.plume.oss.passes

import io.github.plume.oss.GlobalCache
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.DiffGraphUtil
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.passes.DiffGraph
import io.shiftleft.passes.ParallelCpgPass
import io.shiftleft.semanticcpg.passes.containsedges.ContainsEdgePass
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Config
import overflowdb.Graph
import scala.jdk.CollectionConverters
import kotlin.streams.toList
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * Runs passes from [io.shiftleft.dataflowengineoss.passes] over method bodies.
 */
class SCPGPass(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(SCPGPass::javaClass)

    /**
     * Calls SCPG passes. Converges all methods into a local OverflowDB graph instance. This is done concurrently.
     */
    fun runPass() {
        runBlocking {
            val bufferedChannel = Channel<DeltaGraph>()
            val g = Graph.open(Config.withDefaults(), NodeFactories.allAsJava(), EdgeFactories.allAsJava())
            // Producer
            GlobalCache.methodBodies.values.forEach { mg -> launch { bufferedChannel.send(mg) } }
            // Single consumer
            launch {
                repeat(GlobalCache.methodBodies.size) { bufferedChannel.receive().toOverflowDb(g) }
                bufferedChannel.close()
            }.join() // Suspend until the channel is fully consumed.
            // Run passes
            launch {
                Cpg.apply(g).use { cpg ->
                    val methods = g.nodes(NodeTypes.METHOD).asSequence().toList()
                    // TODO: Make own contains edge pass for methods
                    runParallelPass(methods.filterIsInstance<AstNode>(), ContainsEdgePass(cpg))
                    runParallelPass(methods.filterIsInstance<Method>(), ReachingDefPass(cpg))
                }
            }
        }
    }

    /**
     * Pass the parts in a parallel stream while catching any exceptions and logging them.
     */
    private fun <T> runParallelPass(parts: List<T>, pass: ParallelCpgPass<T>) {
        parts.parallelStream()
            .map { part ->
                mutableListOf<DiffGraph>().apply {
                    runCatching { pass.runOnPart(part) }
                        .onFailure { e -> logger.warn("Exception encountered while running parallel pass.", e) }
                        .getOrNull()
                        ?.let { dfs ->
                            CollectionConverters.IteratorHasAsJava(dfs)
                                .asJava()
                                .asSequence()
                                .toCollection(this)
                        }
                }
            }
            .sequential().toList().flatten()
            .forEach { DiffGraphUtil.processDiffGraph(driver, it) }
    }


}