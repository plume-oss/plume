package io.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, PropertyNames}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.apache.commons.configuration2.BaseConfiguration
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.P.{neq, within}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{
  GraphTraversal,
  GraphTraversalSource,
  __
}
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, T, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsScala}
import scala.util.{Failure, Success, Try, Using}

/** The driver used by databases implementing Gremlin.
  */
abstract class GremlinDriver extends IDriver {

  private val logger                      = LoggerFactory.getLogger(classOf[GremlinDriver])
  protected val config: BaseConfiguration = new BaseConfiguration()
  config.setProperty(
    "gremlin.graph",
    "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph"
  )
  config.setProperty("gremlin.tinkergraph.vertexIdManager", "LONG")
  protected val graph: Graph = TinkerGraph.open(config)
  private val connected      = new AtomicBoolean(true)

  override def isConnected: Boolean = connected.get()

  override def close(): Unit = Try(graph.close()) match {
    case Success(_) => connected.set(false)
    case Failure(e) =>
      logger.warn("Exception thrown while attempting to close graph.", e)
      connected.set(false)
  }

  override def clear(): Unit = Using.resource(graph.traversal()) { g => g.V().drop().iterate() }

  override def exists(nodeId: Long): Boolean = Using.resource(graph.traversal()) { g =>
    g.V(nodeId).hasNext
  }

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    Using.resource(graph.traversal()) { g =>
      g.V(srcId).out(edge).asScala.filter(v => v.id() == dstId).hasNext
    }

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Do node operations first in groups operations
    dg.diffGraph.iterator
      .collect {
        case x: Change.CreateNode      => x
        case x: Change.SetNodeProperty => x
        case x: Change.RemoveNode      => x
      }
      .grouped(50)
      .foreach { ops: Seq[Change] =>
        Using.resource(graph.traversal()) { g => bulkNodeTx(g, ops, dg) }
      }
    // Now that all nodes are in, do edges
    dg.diffGraph.iterator
      .collect {
        case x: Change.CreateEdge => x
        case x: Change.RemoveEdge => x
      }
      .grouped(50)
      .foreach { ops: Seq[Change] =>
        Using.resource(graph.traversal()) { g => bulkEdgeTx(g, ops, dg) }
      }
  }

  private def bulkNodeTx(
      g: GraphTraversalSource,
      ops: Seq[Change],
      dg: AppliedDiffGraph
  ): Unit = {
    var ptr: Option[GraphTraversal[Vertex, Vertex]] = None
    ops.foreach {
      case Change.CreateNode(node) =>
        ptr match {
          case Some(p) =>
            ptr = Some(p.addV(node.label).property(T.id, dg.nodeToGraphId(node)))
            removeLists(node.properties).foreach { case (k, v) => p.property(k, v) }
          case None =>
            ptr = Some(g.addV(node.label).property(T.id, dg.nodeToGraphId(node)))
            removeLists(node.properties).foreach { case (k, v) => ptr.get.property(k, v) }
        }
      case Change.SetNodeProperty(node, key, value) =>
        ptr match {
          case Some(p) => ptr = Some(p.V(node.id()).property(key, value))
          case None    => ptr = Some(g.V(node.id()).property(key, value))
        }
      case Change.RemoveNode(nodeId) =>
        ptr match {
          case Some(p) => ptr = Some(p.V(nodeId).drop())
          case None    => ptr = Some(g.V(nodeId).drop())
        }
      case _ => // nothing
    }
    // Commit transaction
    ptr match {
      case Some(p) => p.iterate()
      case None    =>
    }
  }

  private def bulkEdgeTx(
      g: GraphTraversalSource,
      ops: Seq[Change],
      dg: AppliedDiffGraph
  ): Unit = {
    var ptr: Option[GraphTraversal[Vertex, Edge]] = None
    ops.foreach {
      case Change.CreateEdge(src, dst, label, packedProperties) =>
        ptr match {
          case Some(p) => ptr = Some(p.V(id(src, dg)).addE(label).to(__.V(id(dst, dg))))
          case None    => ptr = Some(g.V(id(src, dg)).addE(label).to(__.V(id(dst, dg))))
        }
        PackedProperties.unpack(packedProperties).foreach { case (k: String, v: Any) =>
          ptr.get.property(k, v)
        }
      case Change.RemoveEdge(edge) =>
        ptr match {
          case Some(p) =>
            ptr = Some(
              p.V(edge.outNode().id())
                .outE(edge.label())
                .where(__.inV().has(T.id, edge.inNode().id()))
                .drop()
            )
          case None =>
            ptr = Some(
              g.V(edge.outNode().id())
                .outE(edge.label())
                .where(__.inV().has(T.id, edge.inNode().id()))
                .drop()
            )
        }
      case _ => // nothing
    }
    // Commit transaction
    ptr match {
      case Some(p) => p.iterate()
      case None    =>
    }
  }

  override def deleteNodeWithChildren(
      nodeType: String,
      edgeToFollow: String,
      propertyKey: String,
      propertyValue: Any
  ): Unit = {
    Using.resource(graph.traversal()) { g =>
      g.V()
        .hasLabel(nodeType)
        .aggregate("x")
        .repeat(__.out(edgeToFollow))
        .emit()
        .barrier()
        .aggregate("x")
        .select[Vertex]("x")
        .unfold[Vertex]()
        .drop()
        .iterate()
    }
  }

  override def removeSourceFiles(filenames: String*): Unit = {
    Using.resource(graph.traversal()) { g =>
       val fs = g.V()
          .hasLabel(NodeTypes.FILE)
          .filter(__.has(PropertyNames.NAME, within[String](filenames: _*)))
         .id().toSet

      g.V(fs)
        .in(EdgeTypes.SOURCE_FILE)
        .filter(__.hasLabel(NodeTypes.TYPE_DECL))
        .in(EdgeTypes.REF)
        .drop()
        .iterate()

      g.V(fs)
        .in(EdgeTypes.SOURCE_FILE)
        .hasLabel(NodeTypes.NAMESPACE_BLOCK)
        .aggregate("x")
        .repeat(__.out(EdgeTypes.AST))
        .emit()
        .barrier()
        .aggregate("x")
        .select[Vertex]("x")
        .unfold[Vertex]()
        .drop()
        .iterate()

      g.V(fs)
        .drop()
        .iterate()
    }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = {
    Using.resource(graph.traversal()) { g =>
      var ptr = g
        .V()
        .hasLabel(nodeType)
        .project[Any](T.id.toString, keys: _*)
        .by(T.id)
      keys.foreach(k => ptr = ptr.by(k))
      ptr.asScala
        .map(_.asScala.toMap)
        .toList
    }
  }

  override def idInterval(lower: Long, upper: Long): Set[Long] =
    Using.resource(graph.traversal()) { g =>
      g.V()
        .order()
        .by(T.id, Order.asc)
        .range(lower - 1, upper)
        .id()
        .asScala
        .map(_.toString.toLong)
        .toSet
    }

  override def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Long],
      dstFullNameKey: String
  ): Unit = {
    Using.resource(graph.traversal()) { g =>
      g
        .V()
        .hasLabel(srcLabels.head, srcLabels.drop(1): _*)
        .filter(
          has(dstFullNameKey)
            .and(has(dstFullNameKey, neq(null)))
            .and(has(dstFullNameKey, neq(-1)))
            .and(has(dstFullNameKey, neq("<empty>")))
        )
        .project[Any]("id", dstFullNameKey)
        .by(T.id)
        .by(dstFullNameKey)
        .asScala
        .map(_.asScala.toMap)
        .foreach { m =>
          val srcId       = m.getOrElse("id", null).asInstanceOf[Long]
          val dstFullName = m.getOrElse(dstFullNameKey, null).asInstanceOf[String]
          if (dstFullName != null) {
            dstNodeMap.get(dstFullName) match {
              case Some(dstId) =>
                if (!exists(srcId, dstId, edgeType)) {
                  g.V(srcId).addE(edgeType).to(__.V(dstId)).iterate()
                }
              case None =>
            }
          }
        }
    }
  }

}
