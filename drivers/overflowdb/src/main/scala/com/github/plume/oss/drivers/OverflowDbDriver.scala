package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.drivers.OverflowDbDriver.newOverflowGraph
import com.github.plume.oss.util.BatchedUpdateUtil
import com.github.plume.oss.util.BatchedUpdateUtil.*
import io.shiftleft.codepropertygraph.cpgloading.CpgLoader
import io.shiftleft.codepropertygraph.generated.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.commons.text.StringEscapeUtils
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.DiffOrBuilder
import overflowdb.traversal.jIteratortoTraversal
import overflowdb.{BatchedUpdate, Config, DetachedNodeData, Edge, Node}

import java.io.{FileOutputStream, OutputStreamWriter, File as JFile}
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsScala}
import scala.util.*

/** Driver to create an OverflowDB database file.
  * @param storageLocation
  *   where the database will serialize to and deserialize from.
  * @param heapPercentageThreshold
  *   the percentage of the JVM heap from when the database will begin swapping to disk.
  * @param serializationStatsEnabled
  *   enables saving of serialization statistics.
  */
final case class OverflowDbDriver(
  storageLocation: Option[String] = Option(JFile.createTempFile("plume-", ".odb").getAbsolutePath),
  heapPercentageThreshold: Int = 80,
  serializationStatsEnabled: Boolean = false
) extends IDriver {

  private val logger = LoggerFactory.getLogger(classOf[OverflowDbDriver])

  private val odbConfig = Config
    .withDefaults()
    .withHeapPercentageThreshold(heapPercentageThreshold)
  storageLocation match {
    case Some(path) => odbConfig.withStorageLocation(path)
    case None       => odbConfig.disableOverflow()
  }
  if (serializationStatsEnabled) odbConfig.withSerializationStatsEnabled()

  /** A direct pointer to the code property graph object.
    */
  val cpg: Cpg =
    PlumeStatistics.time(PlumeStatistics.TIME_OPEN_DRIVER, { newOverflowGraph(odbConfig) })

  CpgLoader.createIndexes(cpg)

  override def isConnected: Boolean = !cpg.graph.isClosed

  override def close(): Unit = PlumeStatistics.time(
    PlumeStatistics.TIME_CLOSE_DRIVER, {
      Try(cpg.close()) match {
        case Success(_) =>
        case Failure(e) =>
          logger.warn("Exception thrown while attempting to close graph.", e)
      }
    }
  )

  override def clear(): Unit = {
    cpg.graph.nodes.asScala.foreach(safeRemove)
  }

  override def exists(nodeId: Long): Boolean = cpg.graph.node(nodeId) != null

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    cpg.graph.node(srcId).out(edge).asScala.exists { dst => dst.id() == dstId }

  override def bulkTx(dg: DiffOrBuilder): Int = {
    BatchedUpdate.applyDiff(cpg.graph, dg)
    dg.size()
  }

  private def safeRemove(n: Node): Unit = Try(if (n != null) {
    n.inE().forEachRemaining(_.remove())
    n.outE().forEachRemaining(_.remove())
    n.remove()
  }) match {
    case Failure(e) if cpg.graph.node(n.id()) != null =>
      logger.warn(s"Unable to delete node due to error '${e.getMessage}': [${n.id()}] ${n.label()}")
    case Failure(e) =>
      logger.warn(s"Exception '${e.getMessage}' occurred while attempting to delete node: [${n.id()}] ${n.label()}")
    case _ =>
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] =
    cpg.graph
      .nodes(nodeType)
      .asScala
      .map { n =>
        keys.map { k =>
          k -> n.propertiesMap().getOrDefault(k, null)
        }.toMap + ("id" -> n.id())
      }
      .toList

  /** Serializes the graph in the OverflowDB instance to the
    * [[http://graphml.graphdrawing.org/specification/dtd.html GraphML]] format to the given OutputStreamWriter. This
    * format is supported by [[https://tinkerpop.apache.org/docs/current/reference/#graphml TinkerGraph]] and
    * [[http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#graphml Cytoscape]].
    * @param exportPath
    *   the path to write the GraphML representation of the graph to.
    */
  def exportAsGraphML(exportPath: java.io.File): Unit = {
    val g = cpg.graph
    Using.resource(new OutputStreamWriter(new FileOutputStream(exportPath))) { osw =>
      // Write header
      osw.write("<?xml version=\"1.0\" ?>")
      osw.write("<graphml ")
      osw.write("xmlns=\"http://graphml.graphdrawing.org/xmlns\" ")
      osw.write("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ")
      osw.write(
        "xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd\">"
      )
      // Write keys
      osw.write("<key id=\"labelV\" for=\"node\" attr.name=\"labelV\" attr.type=\"string\"></key>")
      osw.write("<key id=\"labelE\" for=\"edge\" attr.name=\"labelE\" attr.type=\"string\"></key>")
      g.nodes()
        .flatMap(_.propertiesMap().asScala)
        .map { case (k: String, v: Object) =>
          k -> (v match {
            case _: java.lang.Integer => "int"
            case _: java.lang.Boolean => "boolean"
            case _                    => "string"
          })
        }
        .foreach { case (k: String, t: String) =>
          osw.write("<key ")
          osw.write("id=\"" + k + "\" ")
          osw.write("for=\"node\" ")
          osw.write("attr.name=\"" + k + "\" ")
          osw.write("attr.type=\"" + t + "\">")
          osw.write("</key>")
        }
      // Write graph
      osw.write("<graph id=\"G\" edgedefault=\"directed\">")
      // Write vertices
      g.nodes().foreach { (n: Node) =>
        osw.write("<node id=\"" + n.id + "\">")
        osw.write("<data key=\"labelV\">" + n.label() + "</data>")
        serializeLists(n.propertiesMap().asScala.toMap).foreach { case (k, v) =>
          osw.write("<data key=\"" + k + "\">" + StringEscapeUtils.escapeXml11(v.toString) + "</data>")
        }
        osw.write("</node>")
      }
      // Write edges
      g.edges().zipWithIndex.foreach { case (e: Edge, i: Int) =>
        osw.write("<edge id=\"" + i + "\" ")
        osw.write("source=\"" + e.outNode().id() + "\" ")
        osw.write("target=\"" + e.inNode().id() + "\">")
        osw.write("<data key=\"labelE\">" + e.label() + "</data>")
        osw.write("</edge>")
      }
      // Close graph tags
      osw.write("</graph>")
      osw.write("</graphml>")
    }
  }

}

object OverflowDbDriver {
  def newOverflowGraph(odbConfig: Config = Config.withDefaults()): Cpg = Cpg.withConfig(odbConfig)
}
