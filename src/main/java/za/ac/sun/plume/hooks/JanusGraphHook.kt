package za.ac.sun.plume.hooks

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.propertiesToMap
import za.ac.sun.plume.domain.models.GraPLVertex
import java.io.File

class JanusGraphHook private constructor(builder: Builder) : GremlinHook(builder.graph) {

    private val logger = LogManager.getLogger(JanusGraphHook::class.java)
    private val supportsTransactions: Boolean
    private val conf: String
    private var tx: Transaction? = null

    override fun startTransaction() {
        if (supportsTransactions) {
            logger.debug("Supports tx")
            if (tx == null || !tx!!.isOpen) {
                logger.debug("Created new tx")
                try {
                    tx = AnonymousTraversalSource.traversal().withRemote(conf).tx()
                } catch (e: Exception) {
                    logger.error("Unable to create transaction!")
                }
            }
        } else {
            logger.debug("Does not support tx")
        }
        try {
            super.setTraversalSource(AnonymousTraversalSource.traversal().withRemote(conf))
        } catch (e: Exception) {
            logger.error("Unable to create transaction!")
        }
    }

    override fun endTransaction() {
        if (supportsTransactions) {
            var success = false
            var failures = 0
            val waitTime = 5000
            do {
                try {
                    if (tx == null) return
                    if (!tx!!.isOpen) return
                    tx!!.commit()
                    success = true
                } catch (e: IllegalStateException) {
                    failures++
                    if (failures > 3) {
                        logger.error("Failed to commit transaction $failures time(s). Aborting...")
                        return
                    } else {
                        logger.warn("Failed to commit transaction $failures time(s). Backing off and retrying...")
                        try {
                            Thread.sleep(waitTime.toLong())
                        } catch (ignored: Exception) {
                        }
                    }
                }
            } while (!success)
        } else {
            super.endTransaction()
        }
    }

    /**
     * Given a [GraPLVertex], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this hook's [Graph].
     *
     * @param gv the [GraPLVertex] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    override fun createTinkerPopVertex(gv: GraPLVertex): Vertex {
        val propertyMap = propertiesToMap(gv)
        // Get the implementing class label parameter
        val label = propertyMap.remove("label") as String?
        // Get the implementing classes fields and values
        var traversalPointer = g.addV(label)
        for ((key, value) in propertyMap) traversalPointer = traversalPointer.property(key, value)
        return traversalPointer.next()
    }

    data class Builder(
            var conf: String,
            var graphDir: String? = null
    ) : GremlinHookBuilder {
        var graph: Graph? = null

        constructor(conf: String) : this(conf, null)

        override fun useExistingGraph(graphDir: String): IHookBuilder {
            require(isValidExportPath(graphDir)) {
                "Unsupported graph extension! Supported types are GraphML," +
                        " GraphSON, and Gryo."
            }
            require(File(graphDir).exists()) { "No existing serialized graph file was found at $graphDir" }
            this.graphDir = graphDir
            return this
        }

        @Throws(Exception::class)
        override fun build(): JanusGraphHook {
            graph = AnonymousTraversalSource.traversal().withRemote(conf).graph
            return JanusGraphHook(this)
        }
    }

    init {
        conf = builder.conf
        if (builder.graphDir != null) graph.traversal().io<Any>(builder.graphDir).read().iterate()
        supportsTransactions = graph.features().graph().supportsTransactions()
    }
}