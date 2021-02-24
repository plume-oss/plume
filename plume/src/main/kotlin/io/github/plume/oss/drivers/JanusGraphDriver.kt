package io.github.plume.oss.drivers

import io.github.plume.oss.util.ExtractorConst
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.driver.Client
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource

/**
 * The driver used to connect to a remote JanusGraph instance.
 */
class JanusGraphDriver : GremlinDriver(), ISchemaSafeDriver {
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

    override fun buildSchema() {
        val propFileConfig = PropertiesConfiguration(config.getString(REMOTE_CONFIG))
        val cluster = Cluster.open(propFileConfig.getString("gremlin.remote.driver.clusterFile"))
        val client = cluster.connect<Client>()
        val results = client.submit(buildSchemaPayload())
        println(results.toList())
        client.close()
        cluster.close()
    }

    fun buildSchemaPayload(): String {
        val schema = StringBuilder("""
            graph = JanusGraphFactory.open('/etc/opt/janusgraph/janusgraph.properties')
            mgmt = graph.openManagement()
        """.trimIndent())
        schema.append("\n// Vertex labels\n")
        NodeTypes.ALL.forEach { schema.append("mgmt.containsVertexLabel('$it') ?: mgmt.makeVertexLabel('$it').make()\n")  }
        schema.append("// Edge labels\n")
        EdgeTypes.ALL.forEach { schema.append("mgmt.containsEdgeLabel('$it') ?: mgmt.makeEdgeLabel('$it').make()\n")  }
        schema.append("// Properties\n")
        NodeKeyNames.ALL.forEach { k ->
            schema.append("mgmt.containsPropertyKey('$k') ?: mgmt.makePropertyKey('$k')")
            when {
                ExtractorConst.BOOLEAN_TYPES.contains(k) -> schema.append(".dataType(Boolean.class)")
                ExtractorConst.INT_TYPES.contains(k) -> schema.append(".dataType(Integer.class)")
                else -> schema.append(".dataType(String.class)")
            }
            schema.append(".cardinality(org.janusgraph.core.Cardinality.SINGLE).make()\n")
        }
        schema.append("""
            // Indexes
            mgmt.getGraphIndex("byFullName") != null ?: mgmt.buildIndex("byFullName", Vertex.class).addKey(mgmt.getPropertyKey($FULL_NAME)).buildCompositeIndex()
            mgmt.getGraphIndex("bySignature") != null ?: mgmt.buildIndex("bySignature", Vertex.class).addKey(mgmt.getPropertyKey($SIGNATURE)).buildCompositeIndex()
            mgmt.getGraphIndex("byHash") != null ?: mgmt.buildIndex("byHash", Vertex.class).addKey(mgmt.getPropertyKey($HASH)).buildCompositeIndex()
            mgmt.commit()
        """.trimIndent())
        return schema.toString()
    }

}