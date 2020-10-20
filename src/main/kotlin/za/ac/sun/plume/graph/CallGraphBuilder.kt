package za.ac.sun.plume.graph

import org.apache.logging.log4j.LogManager
import soot.Scene
import soot.SootMethod
import soot.Unit
import soot.jimple.IdentityStmt
import soot.jimple.toolkits.callgraph.Edge
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.MethodVertex
import za.ac.sun.plume.drivers.IDriver

/**
 * The [IGraphBuilder] that constructs the interprocedural call edges.
 *
 * @param driver The driver to build the call edges with.
 * @param sootToPlume A pointer to the map that keeps track of the Soot object to its respective [PlumeVertex].
 */
class CallGraphBuilder(
        private val driver: IDriver,
        private val sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>
) : IGraphBuilder {
    private val logger = LogManager.getLogger(CallGraphBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun buildMethodBody(graph: BriefUnitGraph): MethodVertex {
        val mtd = graph.body.method
        logger.debug("Building call graph edges for ${mtd.declaration}")
        this.graph = graph
        // Connect all units to their successors
        this.graph.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
        return sootToPlume[mtd]?.first { it is MethodVertex } as MethodVertex
    }

    private fun projectUnit(it: Unit) {
        val cg = Scene.v().callGraph
        val edges = cg.edgesOutOf(it) as Iterator<Edge>
        edges.forEach { e: Edge ->
            val srcPlumeVertex = sootToPlume[it]?.first()
            val tgtPlumeVertex = sootToPlume[e.tgt.method()]?.first()
            if (srcPlumeVertex != null && tgtPlumeVertex != null) driver.addEdge(srcPlumeVertex, tgtPlumeVertex, EdgeLabel.REF)
        }
    }

}