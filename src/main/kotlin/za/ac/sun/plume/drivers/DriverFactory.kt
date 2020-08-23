package za.ac.sun.plume.drivers

object DriverFactory {
    @JvmStatic
    fun makeDriver(driverType: GraphDatabase): IDriver {
        return when(driverType) {
            GraphDatabase.TINKER_GRAPH -> TinkerGraphDriver()
            GraphDatabase.JANUS_GRAPH -> JanusGraphDriver()
            GraphDatabase.TIGER_GRAPH -> TigerGraphDriver()
        }
    }
}

enum class GraphDatabase {
    TINKER_GRAPH,
    JANUS_GRAPH,
    TIGER_GRAPH
}