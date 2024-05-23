package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.util.BatchedUpdateUtil
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, PropertyNames}
import org.apache.commons.configuration.BaseConfiguration
import org.apache.tinkerpop.gremlin.process.traversal.P.within
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.{coalesce, constant, values}
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{GraphTraversal, GraphTraversalSource, __}
import org.apache.tinkerpop.gremlin.structure.{Edge, Graph, T, Vertex}
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.slf4j.{Logger, LoggerFactory}
import overflowdb.BatchedUpdate.{Change, DiffOrBuilder}
import overflowdb.{BatchedUpdate, DetachedNodeData}

import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala, MapHasAsScala}
import scala.util.{Failure, Success, Try}

/** The driver used by databases implementing Gremlin.
  */
abstract class GremlinDriver(txMax: Int = 50) extends IDriver {

  protected val logger: Logger            = LoggerFactory.getLogger(classOf[GremlinDriver])
  protected val config: BaseConfiguration = new BaseConfiguration()
  config.setProperty("gremlin.graph", "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph")
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
    * @return
    *   a Gremlin graph traversal source.
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

  override def bulkTx(dg: DiffOrBuilder): Int = {
    // Do node operations first in groups operations
    dg.iterator()
      .asScala
      .collect {
        case x: DetachedNodeData              => x
        case x: BatchedUpdate.SetNodeProperty => x
      }
      .grouped(txMax)
      .foreach(bulkNodeTx(g(), _))
    // Now that all nodes are in, do edges
    dg.iterator()
      .asScala
      .collect { case x: BatchedUpdate.CreateEdge => x }
      .grouped(txMax)
      .foreach(bulkEdgeTx(g(), _))

    dg.size()
  }

  private def bulkNodeTx(g: GraphTraversalSource, ops: Seq[Change]): Unit = {
    var ptr: Option[GraphTraversal[Vertex, Vertex]] = None
    ops.foreach {
      case change: DetachedNodeData =>
        val nodeId = change.pID
        change.setRefOrId(nodeId)
        val properties = BatchedUpdateUtil.propertiesFromNodeData(change).toMap
        ptr match {
          case Some(p) =>
            ptr = Some(p.addV(change.label).property(T.id, typedNodeId(nodeId)))
            serializeLists(properties).foreach { case (k, v) => p.property(k, v) }
          case None =>
            ptr = Some(g.addV(change.label).property(T.id, typedNodeId(nodeId)))
            serializeLists(properties).foreach { case (k, v) => ptr.get.property(k, v) }
        }
      case setProps: BatchedUpdate.SetNodeProperty =>
        val key   = setProps.label
        val node  = setProps.node
        val value = setProps.value
        val v =
          if (key == PropertyNames.INHERITS_FROM_TYPE_FULL_NAME || key == PropertyNames.OVERLAYS)
            value.toString.split(",")
          else value
        ptr match {
          case Some(p) => ptr = Some(p.V(typedNodeId(node.pID)).property(key, v))
          case None    => ptr = Some(g.V(typedNodeId(node.pID)).property(key, v))
        }
      case _ => // nothing
    }
    // Commit transaction
    ptr match {
      case Some(p) => p.iterate()
      case None    =>
    }
  }

  private def bulkEdgeTx(g: GraphTraversalSource, ops: Seq[BatchedUpdate.CreateEdge]): Unit = {
    var ptr: Option[GraphTraversal[Vertex, Edge]] = None
    ops.foreach { e =>
      ptr match {
        case Some(p) => ptr = Some(p.V(typedNodeId(e.src.pID)).addE(e.label).to(__.V(e.dst.pID)))
        case None    => ptr = Some(g.V(typedNodeId(e.src.pID)).addE(e.label).to(__.V(e.dst.pID)))
      }
      unpack(e.propertiesAndKeys).foreach { case (k: String, v: Any) => ptr.get.property(k, v) }
    }
    // Commit transaction
    ptr match {
      case Some(p) => p.iterate()
      case None    =>
    }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = {
    var ptr = g()
      .V()
      .hasLabel(nodeType)
      .project[Any](T.id.toString, keys*)
      .by(T.id)
    keys.foreach(k => ptr = ptr.by(coalesce(values(k), constant("NULL"))))
    ptr.asScala
      .map(
        _.asScala
          .map { case (k, v) =>
            if (v == "NULL")
              k -> IDriver.getPropertyDefault(k)
            else if (v == PropertyNames.OVERLAYS || v == PropertyNames.INHERITS_FROM_TYPE_FULL_NAME)
              k -> v.toString.split(",").toSeq
            else
              k -> v
          }
          .toMap
      )
      .toList
  }

  @inline
  protected def typedNodeId(nodeId: Long): Any =
    nodeId

}
