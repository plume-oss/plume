package za.ac.sun.plume.graph

import soot.SootMethod
import soot.toolkits.graph.BriefUnitGraph

interface IGraphBuilder {
    /**
     * Builds the graph implementing the interface.
     *
     * @param mtd The [SootMethod] in order to obtain method head information from.
     * @param graph The [BriefUnitGraph] of a method body to build the graph off of.
     */
    fun build(mtd: SootMethod, graph: BriefUnitGraph)
}