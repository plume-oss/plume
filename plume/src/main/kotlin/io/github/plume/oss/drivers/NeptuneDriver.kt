package io.github.plume.oss.drivers

import io.github.plume.oss.domain.mappers.VertexMapper.vertexToMap
import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex


/**
 * The driver used to connect to a remote Amazon Neptune instance.
 */
class NeptuneDriver : GremlinDriver() {
    private val logger = LogManager.getLogger(NeptuneDriver::class.java)

    private val builder: Cluster.Builder = Cluster.build()
    private lateinit var cluster: Cluster

    init {
        builder.port(DEFAULT_PORT).enableSsl(true)
    }

    /**
     * Add one or more the addresses of a Gremlin Servers to the list of servers a Client will try to contact to send
     * requests to. The address should be parseable by InetAddress.getByName(String). That's the only validation
     * performed at this point. No connection to the host is attempted.
     *
     * @param addresses the address(es) of Gremlin Servers to contact.
     */
    fun addHostnames(vararg addresses: String): NeptuneDriver = apply { builder.addContactPoints(*addresses) }

    /**
     * Set the port for the Neptune Gremlin server. Default port number is 8182.
     *
     * @param port the port number e.g. 8182
     */
    fun port(port: Int): NeptuneDriver = apply { builder.port(port) }

    /**
     * Sets the certificate to use by the [Cluster].
     *
     * @param keyChainFile The X.509 certificate chain file in PEM format.
     */
    @Suppress("DEPRECATION")
    fun keyCertChainFile(keyChainFile: String): NeptuneDriver = apply { builder.keyCertChainFile(keyChainFile) }

    /**
     * Connects to the graph database with the given configuration.
     * See [Amazon Documentation](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-gremlin-java.html).
     *
     * @throws IllegalArgumentException if the graph database is already connected.
     */
    override fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        cluster = builder.create()
        super.g = traversal().withRemote(DriverRemoteConnection.using(cluster))
        graph = g.graph
        connected = true
    }

    /**
     * Attempts to close the graph database connection and resources.
     *
     * @throws IllegalArgumentException if one attempts to close an already closed graph.
     */
    override fun close() {
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            cluster.close()
            connected = false
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        }
    }

    override fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> =
        g.V().has(v.build().label(), "id", v.id())

    /**
     * Given a [PlumeVertex], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v the [PlumeVertex] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    override fun createVertex(v: NewNodeBuilder): Vertex {
        // TODO could use NewNode.properties() here
        val propertyMap = prepareVertexProperties(v)
        var traversalPointer = g.addV(v.build().label()).property("id", v.id())
        for ((key, value) in propertyMap) traversalPointer = traversalPointer.property(key, value)
        return traversalPointer.next()
    }

    companion object {
        /**
         * Default port number a remote Gremlin server.
         */
        private const val DEFAULT_PORT = 8182
    }
}