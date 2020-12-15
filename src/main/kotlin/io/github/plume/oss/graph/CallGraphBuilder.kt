package io.github.plume.oss.graph

import org.apache.logging.log4j.LogManager
import soot.Scene
import soot.Unit
import soot.jimple.IdentityStmt
import soot.jimple.toolkits.callgraph.Edge
import soot.toolkits.graph.BriefUnitGraph
import io.github.plume.oss.Extractor
import io.github.plume.oss.Extractor.Companion.getSootAssociation
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.vertices.MethodVertex
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.SootToPlumeUtil.constructPhantom

/**
 * The [IGraphBuilder] that constructs the interprocedural call edges.
 *
 * @param driver The driver to build the call edges with.
 */
class CallGraphBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(CallGraphBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun buildMethodBody(graph: BriefUnitGraph) {
        val mtd = graph.body.method
        logger.debug("Building call graph edges for ${mtd.declaration}")
        // If this was an updated method, connect call graphs
        getSootAssociation(mtd)?.filterIsInstance<MethodVertex>()?.first()?.let { reconnectPriorCallGraphEdges(it) }
        this.graph = graph
        // Connect all units to their successors
        this.graph.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
    }

    private fun projectUnit(unit: Unit) {
        val cg = Scene.v().callGraph
        val edges = cg.edgesOutOf(unit) as Iterator<Edge>
        edges.forEach { e: Edge ->
            getSootAssociation(unit)?.firstOrNull()?.let { srcPlumeVertex ->
                val tgtPlumeVertex = getSootAssociation(e.tgt.method())?.firstOrNull()
                        ?: constructPhantom(e.tgt.method(), driver)
                driver.addEdge(srcPlumeVertex, tgtPlumeVertex, EdgeLabel.CALL)
            }
        }
    }

    private fun reconnectPriorCallGraphEdges(mtdV: MethodVertex) {
        Extractor.getIncomingCallGraphEdges(mtdV)?.let { incomingVs ->
            if (incomingVs.isNotEmpty()) {
                logger.debug("Saved call graph edges found - reconnecting incoming call graph edges")
                incomingVs.forEach { inV -> driver.addEdge(inV, mtdV, EdgeLabel.CALL) }
            } else {
                logger.debug("No previous call graph edges were found")
            }
        }
    }

}