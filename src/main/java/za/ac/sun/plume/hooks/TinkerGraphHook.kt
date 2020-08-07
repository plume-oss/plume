package za.ac.sun.plume.hooks

import org.apache.commons.configuration.BaseConfiguration
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import java.io.File

class TinkerGraphHook private constructor(builder: Builder) : GremlinHook(TinkerGraph.open(builder.conf)) {

    fun exportCurrentGraph(exportDir: String) {
        require(isValidExportPath(exportDir)) {
            "Unsupported graph extension! Supported types are GraphML," +
                    " GraphSON, and Gryo."
        }
        startTransaction()
        g.io<Any>(exportDir).write().iterate()
        endTransaction()
    }

    data class Builder(
            var graphDir: String? = null
    ) : GremlinHookBuilder {
        var conf: BaseConfiguration

        constructor() : this(null)

        override fun useExistingGraph(graphDir: String): Builder {
            require(isValidExportPath(graphDir)) {
                "Unsupported graph extension! Supported types are GraphML," +
                        " GraphSON, and Gryo."
            }
            require(File(graphDir).exists()) { "No existing serialized graph file was found at $graphDir" }
            this.graphDir = graphDir
            return this
        }

        fun conf(conf: BaseConfiguration) = apply { this.conf = conf }

        override fun build() = TinkerGraphHook(this)

        init {
            conf = BaseConfiguration()
            conf.setProperty("gremlin.graph", "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph")
        }
    }

    init {
        if (builder.graphDir != null) super.graph.traversal().io<Any>(builder.graphDir).read().iterate()
    }
}