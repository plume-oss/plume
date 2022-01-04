package io.github.plume.oss.drivers

import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import scala.util.Using

/** The driver used to connect to an in-memory TinkerGraph instance.
  */
class TinkerGraphDriver extends GremlinDriver {

  override protected val logger: Logger = LoggerFactory.getLogger(classOf[TinkerGraphDriver])

  /** Add or update a [[org.apache.commons.configuration.BaseConfiguration]] key-value pair.
    *
    * @param key the key of the property.
    * @param value the value of the property.
    */
  def addConfig(key: String, value: String): TinkerGraphDriver = {
    this.config.setProperty(key, value); this
  }

  /** Export the currently connected graph to the given path. The extension of the file should be included and may only
    * be .xml, .json, or .kryo. If a graph file already exists it will be overwritten.
    *
    * @param filePath the file path to export to.
    */
  def exportGraph(filePath: String): Unit = {
    if (!isConnected) {
      throw new RuntimeException(
        "The driver is not connected to any graph and therefore cannot export anything!"
      )
    }
    if (!isSupportedExtension(filePath)) {
      throw new RuntimeException(
        "Unsupported graph extension! Supported types are GraphML, GraphSON, and Gryo."
      )
    }
    Using.resource(this.graph.traversal()) { g =>
      g.io[Any](filePath).write().iterate()
    }
  }

  /** Imports a .xml, .json, or .kryo TinkerGraph file into the currently connected graph.
    *
    * @param filePath the file path to import from.
    */
  def importGraph(filePath: String): Unit = {
    if (!isConnected) {
      throw new RuntimeException(
        "The driver is not connected to any graph and therefore cannot import anything!"
      )
    }
    if (!isSupportedExtension(filePath)) {
      throw new RuntimeException(
        "Unsupported graph extension! Supported types are GraphML, GraphSON, and Gryo."
      )
    }
    if (!new File(filePath).exists) {
      throw new RuntimeException(s"No existing serialized graph file was found at $filePath")
    }
    Using.resource(this.graph.traversal()) { g =>
      g.io[Any](filePath).read().iterate()
    }
  }

  /** Determines if the extension of the given file path is supported by TinkerGraph I/O.
    *
    * @param filePath the file path to check.
    */
  private def isSupportedExtension(filePath: String): Boolean = {
    val ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase()
    "xml" == ext || "json" == ext || "kryo" == ext
  }
}
