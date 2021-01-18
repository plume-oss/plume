package io.github.plume.oss.drivers

import com.steelbridgelabs.oss.neo4j.structure.Neo4JGraph
import com.steelbridgelabs.oss.neo4j.structure.providers.Neo4JNativeElementIdProvider
import io.github.plume.oss.domain.exceptions.PlumeTransactionException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.neo4j.driver.AuthTokens
import org.neo4j.driver.Driver
import org.neo4j.driver.GraphDatabase


/**
 * The driver used to connect to a remote Neo4j instance.
 */
class Neo4jDriver : GremlinDriver() {
    private val logger = LogManager.getLogger(Neo4jDriver::class.java)

    private lateinit var tx: Transaction
    private lateinit var driver: Driver

    /**
     * The Neo4j server database name.
     * @see DEFAULT_DATABASE
     */
    var database: String = DEFAULT_DATABASE
        private set

    /**
     * The Neo4j server username.
     * @see DEFAULT_USERNAME
     */
    var username: String = DEFAULT_USERNAME
        private set

    /**
     * The Neo4j server password.
     * @see DEFAULT_PASSWORD
     */
    var password: String = DEFAULT_PASSWORD
        private set

    /**
     * The Neo4j server hostname.
     * @see DEFAULT_HOSTNAME
     */
    var hostname: String = DEFAULT_HOSTNAME
        private set

    /**
     * The Neo4j server port.
     * @see DEFAULT_PORT
     */
    var port: Int = DEFAULT_PORT
        private set

    /**
     * Set the database name for the Neo4j server.
     *
     * @param value the database name e.g. "graph.db", "neo4j"
     */
    fun database(value: String) = apply { database = value }

    /**
     * Set the username for the Neo4j server.
     *
     * @param value the username e.g. "neo4j_user"
     */
    fun username(value: String) = apply { username = value }

    /**
     * Set the password for the Neo4j server.
     *
     * @param value the password e.g. "neo4j123"
     */
    fun password(value: String) = apply { password = value }

    /**
     * Set the hostname for the Neo4j server.
     *
     * @param value the hostname e.g. 127.0.0.1, www.neoserver.com, etc.
     */
    fun hostname(value: String) = apply { hostname = value }

    /**
     * Set the port for the Neo4j server.
     *
     * @param value the port number e.g. 7687
     */
    fun port(value: Int) = apply { port = value }

    override fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        driver = GraphDatabase.driver("bolt://$hostname:$port", AuthTokens.basic(username, password))
        val vertexIdProvider = Neo4JNativeElementIdProvider()
        val edgeIdProvider = Neo4JNativeElementIdProvider()
        graph = Neo4JGraph(driver, database, vertexIdProvider, edgeIdProvider)
        connected = true
    }

    override fun close() {
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            driver.close()
            super.close()
            connected = false
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        }
    }

    override fun openTx() {
        super.openTx()
        try {
            logger.debug("Creating new tx")
            super.setTraversalSource(graph.traversal())
            tx = g.tx()
        } catch (e: Exception) {
            transactionOpen = false
            logger.error(e)
            throw PlumeTransactionException("Unable to create Neo4j transaction!")
        }
    }

    override fun closeTx() {
        try {
            tx.commit()
            tx.close()
        } catch (e: Exception) {
            logger.error(e)
            throw PlumeTransactionException("Unable to close Neo4j transaction!")
        } finally {
            transactionOpen = false
        }
    }

    override fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> =
        g.V().has(v.build().label(), "id", v.id())

    override fun maxOrder(): Int =
        try {
            if (!transactionOpen) openTx()
            if (g.V().has("order").hasNext())
                (g.V().has("order").order().by("order", Order.desc).limit(1).values<Any>("order")
                    .next() as Long).toInt()
            else 0
        } finally {
            if (transactionOpen) closeTx()
        }

    override fun createVertex(v: NewNodeBuilder): Vertex =
        try {
            // TODO could use NewNode.properties() here
            if (!transactionOpen) openTx()
            val propertyMap = VertexMapper.vertexToMap(v).apply { remove("label") }
            // Get the implementing classes fields and values
            g.graph.addVertex(T.label, v.build().label(), "id", v.id()).apply {
                propertyMap.forEach { (key: String?, value: Any?) ->
                    this.property(
                        key,
                        if (value is Int) value.toLong() else value
                    )
                }
            }
        } finally {
            if (transactionOpen) closeTx()
        }

    companion object {
        /**
         * Default username for the Neo4j server.
         */
        private const val DEFAULT_USERNAME = "neo4j"

        /**
         * Default password for the Neo4j server.
         */
        private const val DEFAULT_PASSWORD = "neo4j"

        /**
         * Default hostname for the Neo4j server.
         */
        private const val DEFAULT_HOSTNAME = "localhost"

        /**
         * Default database name for the Neo4j server.
         */
        private const val DEFAULT_DATABASE = "neo4j"

        /**
         * Default port number a remote Bolt server.
         */
        private const val DEFAULT_PORT = 7687
    }
}