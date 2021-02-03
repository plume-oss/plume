package io.github.plume.oss.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource

/**
 * The driver used to connect to a remote JanusGraph instance.
 */
class JanusGraphDriver : GremlinDriver() {
    private val logger = LogManager.getLogger(JanusGraphDriver::class.java)

    companion object {
        /**
         * The configuration key to set the remote-graph.properties path.
         * See [JanusGraph Documentation](https://docs.janusgraph.org/connecting/java/) for what to set the value to.
         *
         * @see remoteConfig
         */
        const val REMOTE_CONFIG = "remote.config"
    }

    /**
     * Connects to the graph database with the given configuration. Set [REMOTE_CONFIG] to the path of the
     * remote-graph.properties configuration file. See [JanusGraph Documentation](https://docs.janusgraph.org/connecting/java/).
     *
     * @see remoteConfig
     * @throws IllegalArgumentException if the graph database is already connected to or if the remote config path is
     * not set.
     */
    override fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        require(config.containsKey(REMOTE_CONFIG)) { "Remote config path not set! See the config field in JanusGraphDriver with key REMOTE_CONFIG." }
        // Test that the connection works and then close again
        super.g = AnonymousTraversalSource.traversal().withRemote(config.getString(REMOTE_CONFIG))
        connected = true
    }

    override fun close() {
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            // The graphs are immutable so we can't call close() from graph
            g.close()
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        } finally {
            connected = false
        }
    }

    /**
     * Sets the path of the remote-graph.properties configuration file.
     * See [JanusGraph Documentation](https://docs.janusgraph.org/connecting/java/).
     *
     * @param remoteConfigPath the path to remote-graph.properties.
     */
    fun remoteConfig(remoteConfigPath: String) = apply { config.setProperty(REMOTE_CONFIG, remoteConfigPath) }

}