package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Transaction
import org.apache.tinkerpop.gremlin.structure.Vertex
import za.ac.sun.plume.domain.exceptions.PlumeTransactionException
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.propertiesToMap
import za.ac.sun.plume.domain.models.PlumeVertex
import java.lang.IllegalArgumentException

/**
 * The driver used to connect to a remote JanusGraph instance.
 */
class JanusGraphDriver : GremlinDriver() {
    private val logger = LogManager.getLogger(JanusGraphDriver::class.java)

    private var supportsTransactions: Boolean = false
    private lateinit var tx: Transaction
    var transactionRetryTime = 5000
    var maxRetries = 3

    companion object {
        /**
         * The configuration key to set the remote-graph.properties path.
         * See [JanusGraph Documentation](https://docs.janusgraph.org/connecting/java/) for what to set the value to.
         */
        const val REMOTE_CONFIG = "remote.config"
    }

    /**
     * Connects to the graph database with the given configuration. Set [REMOTE_CONFIG] to the path of the
     * remote-graph.properties configuration file. See [JanusGraph Documentation](https://docs.janusgraph.org/connecting/java/).
     *
     * @throws IllegalArgumentException if the graph database is already connected to or if the remote config path is
     * not set.
     */
    override fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        require(config.containsKey(REMOTE_CONFIG)) { "Remote config path not set! See the config field in JanusGraphDriver with key REMOTE_CONFIG." }
        graph = AnonymousTraversalSource.traversal().withRemote(config.getString(REMOTE_CONFIG)).graph
        connected = true
        supportsTransactions = graph.features().graph().supportsTransactions()
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
                tx = AnonymousTraversalSource.traversal().withRemote(config.getString(REMOTE_CONFIG)).tx()
            } catch (e: Exception) {
                throw PlumeTransactionException("Unable to create JanusGraph transaction!")
            }
        }
        try {
            super.setTraversalSource(AnonymousTraversalSource.traversal().withRemote(config.getString(REMOTE_CONFIG)))
        } catch (e: Exception) {
            throw PlumeTransactionException("Unable to create JanusGraph transaction!")
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

    /**
     * Given a [PlumeVertex], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v the [PlumeVertex] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    override fun createVertex(v: PlumeVertex): Vertex {
        val propertyMap = propertiesToMap(v)
        // Get the implementing class label parameter
        val label = propertyMap.remove("label") as String?
        // Get the implementing classes fields and values
        var traversalPointer = g.addV(label)
        for ((key, value) in propertyMap) traversalPointer = traversalPointer.property(key, value)
        return traversalPointer.next()
    }
}