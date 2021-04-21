/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.drivers

import io.github.plume.oss.metrics.DriverTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import org.apache.commons.configuration.BaseConfiguration
import java.io.File

/**
 * The driver used to connect to an in-memory TinkerGraph instance.
 */
class TinkerGraphDriver internal constructor() : GremlinDriver() {

    override fun connect(): TinkerGraphDriver = super.connect() as TinkerGraphDriver

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
        PlumeTimer.measure(DriverTimeKey.DISCONNECT_SERIALIZE) { g.io<Any>(filePath).write().iterate() }
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
        PlumeTimer.measure(DriverTimeKey.CONNECT_DESERIALIZE) { g.io<Any>(filePath).read().iterate() }
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