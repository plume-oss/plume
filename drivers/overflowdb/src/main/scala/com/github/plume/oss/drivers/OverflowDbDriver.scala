package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.domain._
import com.github.plume.oss.drivers.OverflowDbDriver.newOverflowGraph
import com.github.plume.oss.util.BatchedUpdateUtil._
import io.joern.dataflowengineoss.language.toExtendedCfgNode
import io.joern.dataflowengineoss.queryengine._
import io.joern.dataflowengineoss.semanticsloader.{Parser, Semantics}
import io.shiftleft.codepropertygraph.generated._
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.codepropertygraph.{Cpg => CPG}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.apache.commons.lang.StringEscapeUtils
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.AppliedDiff
import overflowdb.traversal.{Traversal, jIteratortoTraversal}
import overflowdb.{BatchedUpdate, Config, DetachedNodeData, Edge, Node}

import java.io.{FileOutputStream, OutputStreamWriter, File => JFile}
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable
import scala.io.{BufferedSource, Source}
import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsScala}
import scala.util._

/** Driver to create an OverflowDB database file.
  * @param storageLocation where the database will serialize to and deserialize from.
  * @param heapPercentageThreshold the percentage of the JVM heap from when the database will begin swapping to disk.
  * @param serializationStatsEnabled enables saving of serialization statistics.
  */
final case class OverflowDbDriver(
    storageLocation: Option[String] = Option(
      JFile.createTempFile("plume-", ".odb").getAbsolutePath
    ),
    heapPercentageThreshold: Int = 80,
    serializationStatsEnabled: Boolean = false,
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

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Do node operations first
    dg.diffGraph.iterator.foreach {
      case Change.RemoveNode(nodeId) =>
        safeRemove(cpg.graph.node(nodeId))
      case Change.RemoveNodeProperty(nodeId, propertyKey) =>
        cpg.graph.node(nodeId).removeProperty(propertyKey)
      case Change.CreateNode(node) =>
        val newNode = cpg.graph.addNode(dg.nodeToGraphId(node), node.label)
        node.properties.foreach { case (k, v) => newNode.setProperty(k, v) }
      case Change.SetNodeProperty(node, key, value) =>
        cpg.graph.node(node.id()).setProperty(key, value)
      case _ => // do nothing
    }
    // Now that all nodes are in, connect/remove edges
    dg.diffGraph.iterator.foreach {
      case Change.RemoveEdge(edge) =>
        cpg.graph
          .nodes(edge.outNode().id())
          .next()
          .outE(edge.label())
          .forEachRemaining(e => if (e.inNode().id() == edge.inNode().id()) e.remove())
      case Change.CreateEdge(src, dst, label, packedProperties) =>
        val srcId: Long = id(src, dg).asInstanceOf[Long]
        val dstId: Long = id(dst, dg).asInstanceOf[Long]
        val e: overflowdb.Edge =
          cpg.graph.node(srcId).addEdge(label, cpg.graph.node(dstId))
        PackedProperties.unpack(packedProperties).foreach { case (k: String, v: Any) =>
          e.setProperty(k, v)
        }
      case _ => // do nothing
    }
  }

  override def bulkTx(dg: AppliedDiff): Unit = {
    dg.getDiffGraph.iterator.collect { case x: DetachedNodeData => x }.foreach { node =>
      val id      = idFromNodeData(node)
      val newNode = cpg.graph.addNode(id, node.label)
      propertiesFromNodeData(node).foreach { case (k, v) => newNode.setProperty(k, v) }
    }
    dg.getDiffGraph.iterator.filterNot(_.isInstanceOf[DetachedNodeData]).foreach {
      case c: BatchedUpdate.CreateEdge =>
        val srcId = idFromNodeData(c.src)
        val dstId = idFromNodeData(c.dst)
        val e     = cpg.graph.node(srcId).addEdge(c.label, cpg.graph.node(dstId))
        Option(c.propertiesAndKeys) match {
          case Some(edgeKeyValues) =>
            propertiesFromObjectArray(edgeKeyValues).foreach { case (k, v) => e.setProperty(k, v) }
          case None =>
        }
      case c: BatchedUpdate.RemoveNode => safeRemove(cpg.graph.node(c.node.id()))
      case c: BatchedUpdate.SetNodeProperty =>
        cpg.graph.node(c.node.id()).setProperty(c.label, c.value)
    }
  }

  private def accumNodesToDelete(
      n: Node,
      visitedNodes: mutable.Set[Node],
      edgeToFollow: String*
  ): Unit = {
    if (!visitedNodes.contains(n)) {
      visitedNodes.add(n)
      n.out(edgeToFollow: _*)
        .forEachRemaining(accumNodesToDelete(_, visitedNodes, edgeToFollow: _*))
    }
  }

  override def removeSourceFiles(filenames: String*): Unit = {
    val fs = filenames.toSet
    cpg.graph
      .nodes(NodeTypes.FILE)
      .filter { f =>
        fs.contains(f.property(PropertyNames.NAME).toString)
      }
      .foreach { f =>
        val fileChildren    = f.in(EdgeTypes.SOURCE_FILE).asScala.toList
        val typeDecls       = fileChildren.collect { case x: TypeDecl => x }
        val namespaceBlocks = fileChildren.collect { case x: NamespaceBlock => x }
        // Remove TYPE nodes
        typeDecls.flatMap(_.in(EdgeTypes.REF)).foreach(safeRemove)
        // Remove NAMESPACE_BLOCKs and their AST children (TYPE_DECL, METHOD, etc.)
        val nodesToDelete = mutable.Set.empty[Node]
        namespaceBlocks.foreach(
          accumNodesToDelete(_, nodesToDelete, EdgeTypes.AST, EdgeTypes.CONDITION)
        )
        nodesToDelete.foreach(safeRemove)
        // Finally remove FILE node
        safeRemove(f)
      }
  }

  private def safeRemove(n: Node): Unit = Try(if (n != null) {
    n.inE().forEachRemaining(_.remove())
    n.outE().forEachRemaining(_.remove())
    n.remove()
  }) match {
    case Failure(e) if cpg.graph.node(n.id()) != null =>
      logger.warn(
        s"Unable to delete node due to error '${e.getMessage}': [${n.id()}] ${n.label()}"
      )
    case Failure(e) =>
      logger.warn(
        s"Exception '${e.getMessage}' occurred while attempting to delete node: [${n.id()}] ${n.label()}"
      )
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

  override def idInterval(lower: Long, upper: Long): Set[Long] = cpg.graph.nodes.asScala
    .filter(n => n.id() >= lower - 1 && n.id() <= upper)
    .map(_.id())
    .toSet

  override def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Any],
      dstFullNameKey: String,
      dstNodeType: String
  ): Unit = {
    Traversal(cpg.graph.nodes(srcLabels: _*)).foreach { srcNode =>
      srcNode
        .propertyOption(dstFullNameKey)
        .filter {
          case Seq(_*) => true
          case dstFullName =>
            srcNode.propertyDefaultValue(dstFullNameKey) != null &&
              !srcNode.propertyDefaultValue(dstFullNameKey).equals(dstFullName)
        }
        .ifPresent { x =>
          (x match {
            case dstFullName: String  => Seq(dstFullName)
            case dstFullNames: Seq[_] => dstFullNames
            case _                    => Seq()
          }).collect { case x: String => x }
            .filter(dstNodeMap.contains)
            .flatMap { dstFullName: String =>
              dstNodeMap(dstFullName) match {
                case x: Long => Some(x)
                case _       => None
              }
            }
            .foreach { dstNodeId: Long =>
              srcNode match {
                case src: StoredNode =>
                  val dst = cpg.graph.nodes(dstNodeId).next()
                  if (!src.out(edgeType).asScala.contains(dst))
                    src.addEdge(edgeType, dst)
              }
            }
        }
    }
  }

  override def staticCallLinker(): Unit = {
    cpg.graph
      .nodes(NodeTypes.CALL)
      .collect { case x: Call if x.dispatchType == DispatchTypes.STATIC_DISPATCH => x }
      .foreach { c: Call =>
        methodFullNameToNode.get(c.methodFullName) match {
          case Some(dstId) if cpg.graph.nodes(dstId.asInstanceOf[Long]).hasNext =>
            c.addEdge(EdgeTypes.CALL, cpg.graph.node(dstId.asInstanceOf[Long]))
          case _ =>
        }
      }
  }
// TODO: What is this?
  override def dynamicCallLinker(): Unit = {}
//    new PlumeDynamicCallLinker(CPG(cpg.graph)).createAndApply()

  /** Serializes the graph in the OverflowDB instance to the
    * [[http://graphml.graphdrawing.org/specification/dtd.html GraphML]]
    * format to the given OutputStreamWriter. This format is supported by
    * [[https://tinkerpop.apache.org/docs/current/reference/#graphml TinkerGraph]] and
    * [[http://manual.cytoscape.org/en/stable/Supported_Network_File_Formats.html#graphml Cytoscape]].
    * @param exportPath the path to write the GraphML representation of the graph to.
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
          osw.write(
            "<data key=\"" + k + "\">" + StringEscapeUtils.escapeXml(v.toString) + "</data>"
          )
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
