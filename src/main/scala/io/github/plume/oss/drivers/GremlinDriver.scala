package io.github.plume.oss.drivers

import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.apache.commons.configuration2.BaseConfiguration
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{
  GraphTraversal,
  GraphTraversalSource,
  __
}
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, T, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean
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
  val connected              = new AtomicBoolean(true)

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
    // remove edges in serial
//    Using.resource(graph.traversal()) { g =>
//      dg.diffGraph.iterator
//        .foreach {
//          case Change.RemoveEdge(edge) =>
//            g.V(edge.outNode().id())
//              .outE(edge.label())
//              .forEachRemaining(x => {
//                if (x.inVertex().id() == edge.inNode().id) x.remove()
//              })
//          case _ => // nothing
//        }
//    }
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
            node.properties.foreach { case (k, v) => p.property(k, v) }
          case None =>
            ptr = Some(g.addV(node.label).property(T.id, dg.nodeToGraphId(node)))
            node.properties.foreach { case (k, v) => ptr.get.property(k, v) }
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

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = {
    Using.resource(graph.traversal()) { g =>
      var ptr = g
        .V()
        .hasLabel(nodeType)
        .project(T.id.toString, keys: _*)
        .by(T.id)
      keys.foreach(k => ptr = ptr.by(k))
      ptr.asScala
        .map(m => m.asInstanceOf[java.util.Map[String, Any]].asScala.toMap)
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

}
