package io.github.plume.oss.drivers

import io.github.plume.oss.drivers.OverflowDbDriver.newOverflowGraph
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewNode, StoredNode}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.slf4j.LoggerFactory
import overflowdb.{Config, Node}

import java.io.{File => JFile}
import scala.collection.mutable
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try}

case class OverflowDbDriver(
    storageLocation: Option[String] = Option(
      JFile.createTempFile("plume-", ".odb").getAbsolutePath
    ),
    heapPercentageThreshold: Int = 80,
    serializationStatsEnabled: Boolean = false
) extends IDriver {

  private val logger           = LoggerFactory.getLogger(classOf[OverflowDbDriver])
  private var cpg: Option[Cpg] = None

  override def isConnected: Boolean = cpg.isDefined

  override def connect(): Unit = {
    cpg match {
      case Some(_) => logger.warn("OverflowDB driver is already connected.")
      case None =>
        val odbConfig = Config
          .withDefaults()
          .withHeapPercentageThreshold(heapPercentageThreshold)
        storageLocation match {
          case Some(path) => odbConfig.withStorageLocation(path)
          case None       => odbConfig.disableOverflow()
        }
        if (serializationStatsEnabled) odbConfig.withSerializationStatsEnabled()
        cpg = Option(newOverflowGraph(odbConfig))
    }
  }

  override def clear(): Unit = cpg match {
    case Some(cpg) => cpg.graph.nodes.asScala.foreach(_.remove())
    case None      =>
  }

  override def exists(nodeId: Long): Boolean = cpg match {
    case Some(cpg) => cpg.graph.node(nodeId) != null
    case None      => false
  }

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    cpg match {
      case Some(cpg) => cpg.graph.node(srcId).out(edge).asScala.exists { dst => dst.id() == dstId }
      case None      => false
    }

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    cpg match {
      case Some(cpg) =>
        // Do node operations first
        dg.diffGraph.iterator.foreach {
          case Change.RemoveNode(nodeId) =>
            cpg.graph.node(nodeId).remove()
          case Change.RemoveNodeProperty(nodeId, propertyKey) =>
            cpg.graph.nodes(nodeId).next().removeProperty(propertyKey)
          case Change.CreateNode(node) =>
            val newNode = cpg.graph.addNode(dg.nodeToGraphId(node), node.label)
            node.properties.foreach { case (k, v) => newNode.setProperty(k, v) }
          case Change.SetNodeProperty(node, key, value) =>
            cpg.graph.nodes(node.id()).next().setProperty(key, value)
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
            val srcId: Long = id(src, dg)
            val dstId: Long = id(dst, dg)
            val e: overflowdb.Edge =
              cpg.graph.nodes(srcId).next().addEdge(label, cpg.graph.nodes(dstId).next())
            PackedProperties.unpack(packedProperties).foreach { case (k: String, v: Any) =>
              e.setProperty(k, v)
            }
          case _ => // do nothing
        }
      case None =>
    }
  }

  private def id(node: AbstractNode, dg: AppliedDiffGraph): Long = {
    node match {
      case n: NewNode    => dg.nodeToGraphId(n)
      case n: StoredNode => n.id()
      case _             => throw new RuntimeException(s"Unable to obtain ID for $node")
    }
  }

  override def deleteNodeWithChildren(
      nodeType: String,
      edgeToFollow: String,
      propertyKey: String,
      propertyValue: Any
  ): Unit = {
    val visitedNodes = mutable.Set[Node]()
    def dfsDelete(n: Node): Unit = {
      if (!visitedNodes.contains(n)) {
        visitedNodes.add(n)
        n.out(edgeToFollow).forEachRemaining(dfsDelete(_))
      }
      n.remove()
    }
    cpg match {
      case Some(cpg) =>
        cpg.graph
          .nodes(nodeType)
          .asScala
          .filter(n => n.property(propertyKey, null) == propertyValue)
          .toList
          .headOption match {
          case Some(headNode) => dfsDelete(headNode)
          case None           =>
        }
      case None =>
    }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Seq[String]] = {
    cpg match {
      case Some(cpg) =>
        cpg.graph
          .nodes(nodeType)
          .asScala
          .map { n =>
            keys.map { k =>
              if (k.toLowerCase == "id") {
                n.id().toString
              } else {
                n.propertiesMap().getOrDefault(k, null).toString
              }
            }
          }
          .toList
      case None => List()
    }
  }

  override def close(): Unit = {
    cpg match {
      case Some(g) =>
        Try(g.close()) match {
          case Success(_) => cpg = None
          case Failure(e) =>
            logger.warn("Exception thrown while attempting to close graph.", e); cpg = None
        }
      case None => logger.warn("OverflowDB driver is already disconnected.")
    }
  }

  override def idInterval(lower: Long, upper: Long): Set[Long] = {
    cpg match {
      case Some(value) =>
        value.graph.nodes.asScala
          .filter(n => n.id() >= lower && n.id() <= upper)
          .map(_.id())
          .toSet
      case None => Set()
    }
  }
}

object OverflowDbDriver {
  def newOverflowGraph(odbConfig: Config = Config.withDefaults()): Cpg = Cpg.withConfig(odbConfig)
}
