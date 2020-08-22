package za.ac.sun.plume.drivers

class DriverFactory {
    fun makeDriver(driverType: GraphDatabase): IDriver {
        return when(driverType) {
            GraphDatabase.TINKER_GRAPH -> TinkerGraphDriver()
            GraphDatabase.JANUS_GRAPH -> TODO()
            GraphDatabase.TIGER_GRAPH -> TODO()
        }
    }
}

enum class GraphDatabase {
    TINKER_GRAPH,
    JANUS_GRAPH,
    TIGER_GRAPH
}