package za.ac.sun.plume.hooks

interface GremlinHookBuilder : IHookBuilder {
    fun useExistingGraph(graphDir: String): IHookBuilder
}