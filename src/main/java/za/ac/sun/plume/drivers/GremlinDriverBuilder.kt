package za.ac.sun.plume.drivers

interface GremlinDriverBuilder : IDriverBuilder {
    fun useExistingGraph(graphDir: String): IDriverBuilder
}