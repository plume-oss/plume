package io.github.plume.oss.drivers

import io.github.plume.oss.drivers.OverflowDbDriver.newOverflowGraph
import io.shiftleft.codepropertygraph.generated.nodes.{NamespaceBlock, StoredNode, TypeDecl}
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes, NodeTypes, PropertyNames}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.slf4j.LoggerFactory
import overflowdb.traversal.{Traversal, jIteratortoTraversal}
import overflowdb.{Config, Node}

import java.io.{File => JFile}
import scala.collection.mutable
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try}

/** Driver to create an OverflowDB database file.
  */
case class OverflowDbDriver(
    storageLocation: Option[String] = Option(
      JFile.createTempFile("plume-", ".odb").getAbsolutePath
    ),
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
  val cpg: Cpg = newOverflowGraph(odbConfig)

  override def isConnected: Boolean = !cpg.graph.isClosed

  override def close(): Unit = {
    Try(cpg.close()) match {
      case Success(_) => // nothing
      case Failure(e) =>
        logger.warn("Exception thrown while attempting to close graph.", e)
    }
  }

  override def clear(): Unit = cpg.graph.nodes.asScala.foreach(_.remove())

  override def exists(nodeId: Long): Boolean = cpg.graph.node(nodeId) != null

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    cpg.graph.node(srcId).out(edge).asScala.exists { dst => dst.id() == dstId }

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
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
  }

  private def dfsDelete(
      n: Node,
      visitedNodes: mutable.Set[Node],
      edgeToFollow: String*
  ): Unit = {
    if (!visitedNodes.contains(n)) {
      visitedNodes.add(n)
      n.out(edgeToFollow: _*).forEachRemaining(dfsDelete(_, visitedNodes, edgeToFollow: _*))
    }
    n.remove()
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
        typeDecls.flatMap(_.in(EdgeTypes.REF)).foreach(_.remove())
        // Remove NAMESPACE_BLOCKs and their AST children (TYPE_DECL, METHOD, etc.)
        val visitedNodes = mutable.Set.empty[Node]
        namespaceBlocks.foreach(dfsDelete(_, visitedNodes, EdgeTypes.AST, EdgeTypes.CONDITION))
        // Finally remove FILE node
        f.remove()
      }
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
      dstNodeMap: mutable.Map[String, Long],
      dstFullNameKey: String,
      dstNodeType: String
  ): Unit = {
    Traversal(cpg.graph.nodes(srcLabels: _*)).foreach { srcNode =>
      srcNode
        .propertyOption(dstFullNameKey)
        .filter { dstFullName =>
          dstFullName.isInstanceOf[Seq[String]] ||
          (srcNode.propertyDefaultValue(dstFullNameKey) != null &&
            !srcNode.propertyDefaultValue(dstFullNameKey).equals(dstFullName))
        }
        .ifPresent { x =>
          val ds = x match {
            case dstFullName: String       => Seq(dstFullName)
            case dstFullNames: Seq[String] => dstFullNames
            case _                         => Seq()
          }
          ds.foreach { dstFullName =>
            val src = srcNode.asInstanceOf[StoredNode]
            dstNodeMap.get(dstFullName) match {
              case Some(dstNodeId) =>
                val dst = cpg.graph.nodes(dstNodeId).next()
                if (!src.out(edgeType).asScala.contains(dst))
                  src.addEdge(edgeType, dst)
              case None =>
            }
          }
        }
    }
  }
}

object OverflowDbDriver {
  def newOverflowGraph(odbConfig: Config = Config.withDefaults()): Cpg = Cpg.withConfig(odbConfig)
}
