package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import za.ac.sun.plume.domain.exceptions.PlumeTransactionException
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.vertexToMap
import za.ac.sun.plume.domain.models.PlumeVertex

/**
 * The driver used to connect to a remote Amazon Neptune instance.
 */
class NeptuneDriver : GremlinDriver() {
    private val logger = LogManager.getLogger(NeptuneDriver::class.java)

    private var supportsTransactions: Boolean = false
    private lateinit var tx: Transaction
    private val builder: Cluster.Builder = Cluster.build()
    private lateinit var cluster: Cluster
    var transactionRetryTime = 5000
    var maxRetries = 3

    init {
        builder.port(DEFAULT_PORT)
    }

    fun addHostnames(vararg addresses: String): NeptuneDriver = apply { builder.addContactPoints(*addresses) }

    /**
     * Set the port for the Neptune Gremlin server. Default port number is 8182.
     *
     * @param port the port number e.g. 8182
     */
    fun port(port: Int): NeptuneDriver = apply { builder.port(port) }

    /**
     * Enables connectivity over SSL - note that the server should be configured with SSL turned on for this setting to
     * work properly.
     *
     * @param value set to true if using HTTPS and false if using HTTP.
     */
    fun enableSsl(value: Boolean): NeptuneDriver = apply { builder.enableSsl(value) }

    /**
     * Sets the certificate to use by the [Cluster].
     *
     * @param keyChainFile The file location of the private key in JKS or PKCS#12 format.
     */
    fun keyCertChainFile(keyChainFile: String): NeptuneDriver = apply { builder.keyStore(keyChainFile) }

    /**
     * Connects to the graph database with the given configuration.
     * See [Amazon Documentation](https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-gremlin-java.html).
     *
     * @throws IllegalArgumentException if the graph database is already connected.
     */
    override fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        cluster = builder.create()
        graph = traversal().withRemote(DriverRemoteConnection.using(cluster)).graph
        connected = true
        supportsTransactions = graph.features().graph().supportsTransactions()
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

    /**
     * Starts a new traversal and opens a transaction if the database supports transactions.
     *
     * @throws IllegalArgumentException if there is an already open transaction.
     * @throws PlumeTransactionException if unable to create a remote traversal.
     */
    override fun openTx() {
        require(!transactionOpen) { "Please close the current transaction before creating a new one." }
        if (supportsTransactions && !tx.isOpen) {
            logger.debug("Created new tx")
            try {
                tx = traversal().withRemote(DriverRemoteConnection.using(cluster)).tx()
            } catch (e: Exception) {
                throw PlumeTransactionException("Unable to create Neptune transaction!")
            }
        }
        try {
            super.setTraversalSource(traversal().withRemote(DriverRemoteConnection.using(cluster)))
        } catch (e: Exception) {
            throw PlumeTransactionException("Unable to create Neptune transaction!")
        }
    }

    /**
     * Closes the current traversal and ends the current transaction if the database supports transactions. If the
     * transaction fails, it will retry [maxRetries] times after [transactionRetryTime] milliseconds between attempts.
     *
     * @throws IllegalArgumentException if the transaction is already closed.
     * @throws PlumeTransactionException if the transaction has failed over [maxRetries] amount of times.
     */
    override fun closeTx() {
        require(transactionOpen) { "There is no transaction currently open!" }
        if (supportsTransactions) {
            var success = false
            var failures = 0
            do {
                if (!tx.isOpen) return
                try {
                    tx.commit()
                    success = true
                } catch (e: IllegalStateException) {
                    if (++failures > maxRetries) {
                        throw PlumeTransactionException("Failed to commit transaction $failures time(s). Aborting...")
                    } else {
                        logger.warn("Failed to commit transaction $failures time(s). Backing off and retrying...")
                        try {
                            Thread.sleep(transactionRetryTime.toLong())
                        } catch (ignored: Exception) {
                        }
                    }
                }
            } while (!success)
        } else {
            super.closeTx()
        }
    }

    override fun findVertexTraversal(v: PlumeVertex): GraphTraversal<Vertex, Vertex> =
            g.V().has(v.javaClass.getDeclaredField("LABEL").get(v).toString(), "id", v.hashCode().toString())

    /**
     * Given a [PlumeVertex], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v the [PlumeVertex] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    override fun createVertex(v: PlumeVertex): Vertex {
        val propertyMap = vertexToMap(v)
        // Get the implementing class label parameter
        val label = propertyMap.remove("label") as String?
        // Get the implementing classes fields and values
        var traversalPointer = g.addV(label).property("id", v.hashCode().toString())
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