package io.github.plume.oss.drivers

import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import org.apache.commons.configuration2.BaseConfiguration
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{GraphTraversal, __}
import org.apache.tinkerpop.gremlin.structure.{Graph, T, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.{Failure, Success, Try, Using}

/** The driver used by databases implementing Gremlin.
  */
abstract class GremlinDriver extends IDriver {

  private val logger                    = LoggerFactory.getLogger(classOf[GremlinDriver])
  protected val config: BaseConfiguration = new BaseConfiguration()
  config.setProperty(
    "gremlin.graph",
    "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph"
  )
  config.setProperty("gremlin.tinkergraph.vertexIdManager", "LONG")
  protected val graph: Graph     = TinkerGraph.open(config)
  val connected = new AtomicBoolean(true)

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
    Using.resource(graph.traversal()) { g =>
      // Do node operations first in groups of 50 operations
      dg.diffGraph.iterator
        .collect {
          case x: Change.RemoveNode         => x
          case x: Change.RemoveNodeProperty => x
          case x: Change.CreateNode         => x
          case x: Change.SetNodeProperty    => x
        }
        .grouped(50)
        .foreach { ops: Seq[Change] =>
          val ptr: GraphTraversal[Vertex, Vertex] = g.V()
          ops.foreach {
            case Change.RemoveNode(nodeId) => ptr.V(nodeId).remove()
            case Change.RemoveNodeProperty(nodeId, propertyKey) =>
              ptr.V(nodeId).properties(propertyKey).remove()
            case Change.CreateNode(node) =>
              ptr.addV(node.label).property(T.id, dg.nodeToGraphId(node))
              node.properties.foreach { case (k, v) => ptr.property(k, v) }
            case Change.SetNodeProperty(node, key, value) =>
              ptr.V(node.id()).property(key, value)
          }
          // Commit transaction
          ptr.next()
        }
      // Now that all nodes are in, add edges
      dg.diffGraph.iterator
        .collect { case x: Change.CreateEdge =>
          x
        }
        .grouped(50)
        .foreach { ops: Seq[Change] =>
          val ptr: GraphTraversal[Vertex, Vertex] = g.V()
          ops.foreach { case Change.CreateEdge(src, dst, label, packedProperties) =>
            ptr.V(id(src, dg)).addE(label).to(g.V(id(dst, dg)))
            PackedProperties.unpack(packedProperties).foreach { case (k: String, v: Any) =>
              ptr.property(k, v)
            }
          }
          ptr.next()
        }
      // remove edges in serial
      dg.diffGraph.iterator
        .foreach {
          case Change.RemoveEdge(edge) =>
            g.V(edge.outNode().id())
              .outE(edge.label())
              .forEachRemaining(x => {
                if (x.inVertex().id() == edge.inNode().id) x.remove()
              })
          case _ => // nothing
        }
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

  override def propertyFromNodes(nodeType: String, keys: String*): List[Seq[String]] =
    Using.resource(graph.traversal()) { g =>
      g.V()
        .hasLabel(nodeType)
        .values(keys: _*)
        .asScala
        .toList
    }

  override def idInterval(lower: Long, upper: Long): Set[Long] =
    Using.resource(graph.traversal()) { g =>
      g.V()
        .order()
        .by(T.id, Order.asc)
        .range(lower, upper)
        .id()
        .asScala
        .map(_.toString.toLong)
        .toSet
    }

}
