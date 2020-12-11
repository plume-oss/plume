package io.github.plume.oss.drivers

import org.apache.commons.configuration.BaseConfiguration
import java.io.File

/**
 * The driver used to connect to an in-memory TinkerGraph instance.
 */
class TinkerGraphDriver : GremlinDriver() {

    /**
     * Add or update a [BaseConfiguration] key-value pair.
     *
     * @param key the key of the property.
     * @param value the value of the property.
     */
    fun addConfig(key: String, value: String) = this.config.setProperty(key, value)

    /**
     * Export the currently connected graph to the given path. The extension of the file should be included and may only
     * be .xml, .json, or .kryo. If a graph file already exists it will be overwritten.
     *
     * @param filePath the file path to export to.
     */
    fun exportGraph(filePath: String) {
        require(connected) { "The driver is not connected to any graph and therefore cannot export anything!" }
        require(isSupportedExtension(filePath)) {
            "Unsupported graph extension! Supported types are GraphML," +
                    " GraphSON, and Gryo."
        }
        openTx()
        g.io<Any>(filePath).write().iterate()
        closeTx()
    }

    /**
     * Imports a .xml, .json, or .kryo TinkerGraph file into the currently connected graph.
     *
     * @param filePath the file path to import from.
     */
    fun importGraph(filePath: String) {
        require(connected) { "The driver is not connected to any graph and therefore cannot import anything!" }
        require(isSupportedExtension(filePath)) {
            "Unsupported graph extension! Supported types are GraphML," +
                    " GraphSON, and Gryo."
        }
        require(File(filePath).exists()) { "No existing serialized graph file was found at $filePath" }
        openTx()
        g.io<Any>(filePath).read().iterate()
        closeTx()
    }

    /**
     * Determines if the extension of the given file path is supported by TinkerGraph I/O.
     *
     * @param filePath the file path to check.
     */
    private fun isSupportedExtension(filePath: String): Boolean =
            run {
                val ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase()
                "xml" == ext || "json" == ext || "kryo" == ext
            }
}