package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.util.BatchedUpdateUtil._
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewNode, StoredNode}
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, PropertyNames}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.{Change, PackedProperties}
import io.shiftleft.proto.cpg.Cpg.DispatchTypes
import org.apache.commons.configuration.BaseConfiguration
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.P.{neq, within}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.{coalesce, constant, has, values}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{
  GraphTraversal,
  GraphTraversalSource,
  __
}
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, T, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.slf4j.{Logger, LoggerFactory}
import overflowdb.BatchedUpdate.AppliedDiff
import overflowdb.{BatchedUpdate, DetachedNodeData}

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala, MapHasAsScala}
import scala.util.{Failure, Success, Try}

/** The driver used by databases implementing Gremlin.
  */
abstract class GremlinDriver(txMax: Int = 50) extends IDriver {

  protected val logger: Logger            = LoggerFactory.getLogger(classOf[GremlinDriver])
  protected val config: BaseConfiguration = new BaseConfiguration()
  config.setProperty(
    "gremlin.graph",
    "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph"
  )
  config.setProperty("gremlin.tinkergraph.vertexIdManager", "LONG")
  protected val graph: Graph =
    PlumeStatistics.time(PlumeStatistics.TIME_OPEN_DRIVER, { TinkerGraph.open(config) })
  protected var traversalSource: Option[GraphTraversalSource] = None
  private val connected                                       = new AtomicBoolean(true)

  override def isConnected: Boolean = connected.get()

  override def close(): Unit =
    Try(PlumeStatistics.time(PlumeStatistics.TIME_CLOSE_DRIVER, { graph.close() })) match {
      case Success(_) => connected.set(false)
      case Failure(e) =>
        logger.warn("Exception thrown while attempting to close graph.", e)
        connected.set(false)
    }

  /** Gives a graph traversal source if available or generates a re-usable one if none is available yet.
    * @return a Gremlin graph traversal source.
    */
  protected def g(): GraphTraversalSource = {
    traversalSource match {
      case Some(conn) => conn
      case None =>
        val conn = graph.traversal()
        traversalSource = Some(conn)
        conn
    }
  }

  override def clear(): Unit = g().V().drop().iterate()

  override def exists(nodeId: Long): Boolean = g().V(typedNodeId(nodeId)).hasNext

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    g().V(typedNodeId(srcId)).out(edge).asScala.filter(v => v.id() == typedNodeId(dstId)).hasNext

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Do node operations first in groups operations
    dg.diffGraph.iterator
      .collect {
        case x: Change.CreateNode      => x
        case x: Change.SetNodeProperty => x
        case x: Change.RemoveNode      => x
      }
      .grouped(txMax)
      .foreach { ops: Seq[Change] => bulkNodeTx(g(), ops, dg) }
    // Now that all nodes are in, do edges
    dg.diffGraph.iterator
      .collect {
        case x: Change.CreateEdge => x
        case x: Change.RemoveEdge => x
      }
      .grouped(txMax)
      .foreach { ops: Seq[Change] => bulkEdgeTx(g(), ops, dg) }
  }

  override def bulkTx(dg: AppliedDiff): Unit = {
    dg.getDiffGraph.iterator.asScala
      .collect {
        case c: BatchedUpdate.RemoveNode      => c
        case c: BatchedUpdate.SetNodeProperty => c
        case c: DetachedNodeData              => c
      }
      .grouped(txMax)
      .foreach { changes =>
        var ptr: Option[GraphTraversal[Vertex, Vertex]] = None
        changes.foreach {
          case node: DetachedNodeData =>
            val nodeId  = typedNodeId(idFromNodeData(node))
            val propMap = propertiesFromNodeData(node)
            ptr match {
              case Some(p) =>
                ptr = Some(p.addV(node.label).property(T.id, nodeId))
                serializeLists(propMap).foreach { case (k, v) => p.property(k, v) }
              case None =>
                ptr = Some(g().addV(node.label).property(T.id, nodeId))
                serializeLists(propMap).foreach { case (k, v) => ptr.get.property(k, v) }
            }
          case c: BatchedUpdate.RemoveNode =>
            val nodeId = typedNodeId(c.node.id())
            ptr match {
              case Some(p) => ptr = Some(p.V(nodeId).drop())
              case None    => ptr = Some(g().V(nodeId).drop())
            }
          case c: BatchedUpdate.SetNodeProperty =>
            val v =
              if (
                c.label == PropertyNames.INHERITS_FROM_TYPE_FULL_NAME || c.label == PropertyNames.OVERLAYS
              )
                c.value.toString.split(",")
              else c.value
            val nodeId = typedNodeId(c.node.id())
            ptr match {
              case Some(p) => ptr = Some(p.V(nodeId).property(c.label, v))
              case None    => ptr = Some(g().V(nodeId).property(c.label, v))
            }
        }
        // Commit transaction
        ptr match {
          case Some(p) => p.iterate()
          case None    =>
        }
      }
    dg.getDiffGraph.iterator.asScala
      .collect { case c: BatchedUpdate.CreateEdge => c }
      .grouped(txMax)
      .foreach { changes =>
        var ptr: Option[GraphTraversal[Vertex, Edge]] = None
        changes.foreach { c: BatchedUpdate.CreateEdge =>
          val srcId = typedNodeId(idFromNodeData(c.src))
          val dstId = typedNodeId(idFromNodeData(c.dst))
          ptr match {
            case Some(p) => ptr = Some(p.V(srcId).addE(c.label).to(__.V(dstId)))
            case None    => ptr = Some(g().V(srcId).addE(c.label).to(__.V(dstId)))
          }
          Option(c.propertiesAndKeys) match {
            case Some(edgeKeyValues) =>
              propertiesFromObjectArray(edgeKeyValues).foreach { case (k, v) =>
                ptr.get.property(k, v)
              }
            case None =>
          }
        }
        // Commit transaction
        ptr match {
          case Some(p) => p.iterate()
          case None    =>
        }
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
            ptr = Some(p.addV(node.label).property(T.id, id(node, dg)))
            serializeLists(node.properties).foreach { case (k, v) => p.property(k, v) }
          case None =>
            ptr = Some(g.addV(node.label).property(T.id, id(node, dg)))
            serializeLists(node.properties).foreach { case (k, v) => ptr.get.property(k, v) }
        }
      case Change.SetNodeProperty(node, key, value) =>
        val v =
          if (key == PropertyNames.INHERITS_FROM_TYPE_FULL_NAME || key == PropertyNames.OVERLAYS)
            value.toString.split(",")
          else value
        ptr match {
          case Some(p) => ptr = Some(p.V(typedNodeId(node.id())).property(key, v))
          case None    => ptr = Some(g.V(typedNodeId(node.id())).property(key, v))
        }
      case Change.RemoveNode(rawNodeId) =>
        val nodeId = typedNodeId(rawNodeId)
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
              p.V(typedNodeId(edge.outNode().id()))
                .outE(edge.label())
                .where(__.inV().has(T.id, typedNodeId(edge.inNode().id())))
                .drop()
            )
          case None =>
            ptr = Some(
              g.V(typedNodeId(edge.outNode().id()))
                .outE(edge.label())
                .where(__.inV().has(T.id, typedNodeId(edge.inNode().id())))
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

  override def removeSourceFiles(filenames: String*): Unit = {
    val fs = g()
      .V()
      .hasLabel(NodeTypes.FILE)
      .filter(__.has(PropertyNames.NAME, within[String](filenames: _*)))
      .id()
      .toSet
      .asScala
      .toSeq

    g()
      .V(fs: _*)
      .in(EdgeTypes.SOURCE_FILE)
      .filter(__.hasLabel(NodeTypes.TYPE_DECL))
      .in(EdgeTypes.REF)
      .drop()
      .iterate()

    g()
      .V(fs: _*)
      .in(EdgeTypes.SOURCE_FILE)
      .hasLabel(NodeTypes.NAMESPACE_BLOCK)
      .aggregate("x")
      .repeat(__.out(EdgeTypes.AST, EdgeTypes.CONDITION))
      .emit()
      .barrier()
      .aggregate("x")
      .select[Vertex]("x")
      .unfold[Vertex]()
      .dedup()
      .drop()
      .iterate()

    g()
      .V(fs: _*)
      .drop()
      .iterate()
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = {
    var ptr = g()
      .V()
      .hasLabel(nodeType)
      .project[Any](T.id.toString, keys: _*)
      .by(T.id)
    keys.foreach(k => ptr = ptr.by(coalesce(values(k), constant("NULL"))))
    ptr.asScala
      .map(_.asScala.map { case (k, v) =>
        if (v == "NULL")
          k -> IDriver.getPropertyDefault(k)
        else if (v == PropertyNames.OVERLAYS || v == PropertyNames.INHERITS_FROM_TYPE_FULL_NAME)
          k -> v.toString.split(",").toSeq
        else
          k -> v
      }.toMap)
      .toList
  }

  @inline
  protected def typedNodeId(nodeId: Long): Any =
    nodeId

  override def id(node: AbstractNode, dg: AppliedDiffGraph): Any =
    node match {
      case n: NewNode    => typedNodeId(dg.nodeToGraphId(n))
      case n: StoredNode => typedNodeId(n.id())
      case _             => throw new RuntimeException(s"Unable to obtain ID for $node")
    }

  override def idInterval(lower: Long, upper: Long): Set[Long] =
    g()
      .V()
      .filter(has(T.id, P.gte(lower - 1)).and(has(T.id, P.lte(upper))))
      .id()
      .asScala
      .map(_.toString.toLong)
      .toSet

  override def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Any],
      dstFullNameKey: String,
      dstNodeType: String
  ): Unit =
    g()
      .V()
      .hasLabel(srcLabels.head, srcLabels.drop(1): _*)
      .filter(
        has(dstFullNameKey)
          .and(has(dstFullNameKey, neq(IDriver.INT_DEFAULT)))
          .and(has(dstFullNameKey, neq(IDriver.STRING_DEFAULT)))
      )
      .project[Any]("id", dstFullNameKey)
      .by(T.id)
      .by(coalesce(values(dstFullNameKey), constant("NULL")))
      .asScala
      .map(_.asScala.toMap)
      .foreach { m =>
        val n     = deserializeLists(m)
        val srcId = n.getOrElse("id", null).toString.toLong
        (n.getOrElse(dstFullNameKey, null) match {
          case xs: Seq[_] => xs
          case x          => Seq(x)
        }).collect { case x: String => x }.foreach { dstFullName =>
          dstNodeMap.get(dstFullName) match {
            case Some(dstId: Any) if !exists(srcId, dstId.toString.toLong, edgeType) =>
              g().V(typedNodeId(srcId)).addE(edgeType).to(__.V(dstId)).iterate()
            case _ =>
          }
        }
      }

  override def staticCallLinker(): Unit =
    g()
      .V()
      .hasLabel(NodeTypes.CALL)
      .has(PropertyNames.DISPATCH_TYPE, DispatchTypes.STATIC_DISPATCH.name())
      .project[Any]("id", PropertyNames.METHOD_FULL_NAME)
      .by(T.id)
      .by(coalesce(values(PropertyNames.METHOD_FULL_NAME), constant("NULL")))
      .asScala
      .map(_.asScala.toMap)
      .foreach { m =>
        val srcId       = m.getOrElse("id", null).toString.toLong
        val dstFullName = m.getOrElse(PropertyNames.METHOD_FULL_NAME, null).asInstanceOf[String]
        if (dstFullName != null) {
          methodFullNameToNode.get(dstFullName) match {
            case Some(dstId) if !exists(srcId, dstId.toString.toLong, EdgeTypes.CALL) =>
              g().V(typedNodeId(srcId)).addE(EdgeTypes.CALL).to(__.V(dstId)).iterate()
            case _ =>
          }
        }
      }

  override def dynamicCallLinker(): Unit = {}

}
