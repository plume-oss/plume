package io.github.plume.oss.passes

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.DiffGraphUtil
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import io.shiftleft.codepropertygraph.generated.nodes.Factories
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.passes.DiffGraph
import io.shiftleft.passes.ParallelCpgPass
import io.shiftleft.semanticcpg.passes.containsedges.ContainsEdgePass
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Config
import overflowdb.Edge
import overflowdb.Graph
import scala.jdk.CollectionConverters
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.streams.toList

/**
 * Runs passes from [io.shiftleft.dataflowengineoss.passes] over method bodies.
 */
class SCPGPass(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(SCPGPass::javaClass)

    /**
     * Calls SCPG passes. Converges all methods into a local OverflowDB graph instance. This is done in parallel.
     */
    fun runPass() {
        val methodNames = ConcurrentLinkedDeque(
            driver.getPropertyFromVertices<String>(
                NodeKeyNames.FULL_NAME,
                NodeTypes.METHOD
            )
        )
        val numberOfJobs = methodNames.size
        val oldSummaries = ConcurrentLinkedDeque<Edge>()
        runBlocking {
            // We use a semaphore to avoid spamming the database with too many requests
            val sharedCounterLock = Semaphore(10)
            val bufferedChannel = Channel<Graph>(10)
            val g = Graph.open(
                Config.withDefaults(),
                Factories.allAsJava(),
                io.shiftleft.codepropertygraph.generated.edges.Factories.allAsJava()
            )
            // Producers
            1.rangeTo(numberOfJobs).map {
                async {
                    if (methodNames.isNotEmpty()) {
                        val mName = methodNames.poll()
                        try {
                            sharedCounterLock.acquire()
                            val mg = driver.getMethod(mName, true)
                            bufferedChannel.send(mg)
                        } catch (e: Exception) {
                            logger.warn("Exception while retrieving method body during SCPG phase.", e)
                            methodNames.add(mName)
                        } finally {
                            sharedCounterLock.release()
                        }
                    }
                }
            }
            // Single consumer
            launch {
                repeat(numberOfJobs) {
                    bufferedChannel.receive().use { o -> mergeGraphs(g, o).toCollection(oldSummaries) }
                }
                bufferedChannel.close()
            }.join() // Suspend until the channel is fully consumed.
            // Delete old calculations that don't carry over
            oldSummaries.forEach { e ->
                driver.deleteEdge(
                    VertexMapper.mapToVertex(e.outNode()),
                    VertexMapper.mapToVertex(e.inNode()),
                    e.label()
                )
            }
            // Run passes
            launch {
                val cpg = Cpg.apply(g)
                val methods = g.nodes(NodeTypes.METHOD).asSequence().toList()
                // TODO: Make own contains edge pass for methods
                runParallelPass(methods.filterIsInstance<AstNode>(), ContainsEdgePass(cpg))
                runParallelPass(methods.filterIsInstance<Method>(), ReachingDefPass(cpg))
            }
        }
    }

    @Synchronized
    private fun mergeGraphs(tgt: Graph, o: Graph): List<Edge> {
        val esToRemove = mutableListOf<Edge>()
        o.nodes().asSequence()
            .forEach { n ->
                if (tgt.node(n.id()) == null)
                    tgt.addNode(n.id(), n.label())
                        .let { tn -> n.propertyMap().forEach { (t, u) -> tn.setProperty(t, u) } }
            }
        o.edges().asSequence().filterNot { it.label() == EdgeTypes.REACHING_DEF }
            .forEach { e ->
                val src = tgt.node(e.outNode().id())
                val dst = tgt.node(e.inNode().id())
                if (!src.out(e.label()).asSequence().contains(dst)) src.addEdge(e.label(), dst)
            }
        // Remove previously calculated reaching defs as these change on an interprocedural context
        o.edges().asSequence().filter { it.label() == EdgeTypes.REACHING_DEF }.toCollection(esToRemove)
        return esToRemove
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