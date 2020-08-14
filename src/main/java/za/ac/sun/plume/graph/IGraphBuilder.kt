package za.ac.sun.plume.graph

import soot.SootClass

interface IGraphBuilder {
    /**
     * Builds the graph implementing the interface.
     *
     * @param cls The Soot class to build off of.
     */
    fun build(cls: SootClass)
}