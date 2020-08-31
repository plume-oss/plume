package za.ac.sun.plume.graph

import soot.SootMethod
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.models.PlumeVertex

interface IGraphBuilder {
    /**
     * Builds the graph implementing the interface.
     *
     * @param mtd The [SootMethod] in order to obtain method head information from.
     * @param graph The [BriefUnitGraph] of a method body to build the graph off of.
     * @param sootToPlume A pointer to the map that keeps track of the Soot object to its respective [PlumeVertex].
     */
    fun build(mtd: SootMethod, graph: BriefUnitGraph, sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>)
}