package io.github.plume.oss.drivers
import io.github.plume.oss.drivers.Neo4jDriver._
import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.Change
import org.neo4j.driver.{AuthTokens, GraphDatabase}
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try, Using}

/** The driver used to connect to a remote Neo4j instance.
  */
class Neo4jDriver(
    hostname: String = DEFAULT_HOSTNAME,
    port: Int = DEFAULT_PORT,
    username: String = DEFAULT_USERNAME,
    password: String = DEFAULT_PASSWORD
) extends IDriver {

  private val logger    = LoggerFactory.getLogger(classOf[Neo4jDriver])
  private val connected = new AtomicBoolean(true)
  private val driver =
    GraphDatabase.driver(s"bolt://$hostname:$port", AuthTokens.basic(username, password))

  override def isConnected: Boolean = connected.get()

  override def clear(): Unit = Using.resource(driver.session()) { session =>
    session.writeTransaction { tx =>
      tx.run(
        """
          |MATCH (n)
          |DETACH DELETE n
          |""".stripMargin
      )
    }
  }

  override def close(): Unit = Try(driver.close()) match {
    case Failure(e) => logger.warn("Exception thrown while attempting to close graph.", e)
    case Success(_) => connected.set(false)
  }

  override def exists(nodeId: Long): Boolean = Using.resource(driver.session()) { session =>
    session.writeTransaction { tx =>
      !tx
        .run(s"""
               |MATCH (n)
               |WHERE n.ID = $nodeId
               |RETURN n
               |""".stripMargin)
        .list
        .isEmpty
    }
  }

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    Using.resource(driver.session()) { session =>
      session.writeTransaction { tx =>
        tx
          .run(s"""
                |MATCH (a), (b)
                |WHERE a.ID = $srcId AND b.ID = $dstId
                |RETURN EXISTS ((a)-[:$edge]->(b)) as edge_exists
                |""".stripMargin)
          .next()["edge_exists"]
          .toString == "TRUE"
      }
    }

  private def nodePayload(id: Long, n: NewNode): String = {
    def escape(raw: String): String = {
      import scala.reflect.runtime.universe._
      Literal(Constant(raw)).toString
    }
    val propertyStr = (n.properties.map { case (k, v) =>
      s"$k:" + v match {
        case x: String => s""""${escape(x)}""""
        case x         => x
      }
    }.toList :+ s"ID: $id")
      .mkString(",")
    s"CREATE (n$id:${n.label} {$propertyStr})"
  }

  private def bulkDeleteNode(ops: Seq[Change.RemoveNode]): Unit = {
    val m = ops
      .map {
        case Change.RemoveNode(nodeId) => s"(n$nodeId {ID: $nodeId})"
        case _                         =>
      }
      .mkString(",")
    val d = ops
      .map {
        case Change.RemoveNode(nodeId) => s"n$nodeId"
        case _                         =>
      }
      .mkString(",")
    val payload = s"""
                |MATCH $m
                |DETACH DELETE $d
                |""".stripMargin
    Using.resource(driver.session()) { session =>
      try {
        session.writeTransaction { tx => tx.run(payload) }
      } catch {
        case e: Exception =>
          logger.error(s"Unable to write bulk delete node transaction $payload", e)
      }
    }
  }

  private def bulkCreateNode(ops: Seq[Change.CreateNode], dg: AppliedDiffGraph): Unit = {
    val ns = ops
      .map { case Change.CreateNode(node) => nodePayload(dg.nodeToGraphId(node), node) }
      .mkString("\n")
    Using.resource(driver.session()) { session =>
      try {
        session.writeTransaction { tx => tx.run(ns) }
      } catch {
        case e: Exception => logger.error(s"Unable to write bulk create node transaction $ns", e)
      }
    }
  }

  private def bulkNodeSetProperty(ops: Seq[Change.SetNodeProperty]): Unit = {
    val ms = ops.map { case Change.SetNodeProperty(node, _, _) =>
      s"(n${node.id()}:${node.label} {ID:${node.id()}})"
    }
    val ss = ops.map { case Change.SetNodeProperty(node, k, v) =>
      val s = v match {
        case x: String       => s""""$x""""
        case x: Number       => x.toString
        case x: List[String] => x.toString
        case x               => logger.warn(s"Unhandled property $x (${x.getClass}")
      }
      s"n${node.id()}.$k = $s"
    }
    val payload =
      s"""
        | MATCH $ms
        | SET $ss
        |""".stripMargin
    Using.resource(driver.session()) { session =>
      try {
        session.writeTransaction { tx => tx.run(payload) }
      } catch {
        case e: Exception => logger.error(s"Unable to write bulk set node transaction $payload", e)
      }
    }
  }

  private def bulkRemoveEdge(ops: Seq[Change.RemoveEdge]): Unit = {
    val ms = ops.zipWithIndex
      .map { case (Change.RemoveEdge(edge), i) =>
        val srcId    = edge.outNode().id()
        val srcLabel = edge.outNode().label()
        val dstId    = edge.inNode().id()
        val dstLabel = edge.outNode().label()
        s"(n$srcId:$srcLabel {ID: $srcId})-[r$i:${edge.label()}]->(n$dstId:$dstLabel {ID: $dstId})"
      }
      .mkString(",")
    val rs = ops.zipWithIndex
      .map { case (Change.RemoveEdge(edge), i) => s"r$i:${edge.label()}" }
      .mkString(",")
    Using.resource(driver.session()) { session =>
      val payload = s"""
                       |MATCH $ms
                       |DELETE $rs
                       |""".stripMargin
      try {
        session.writeTransaction { tx => tx.run(payload) }
      } catch {
        case e: Exception =>
          logger.error(s"Unable to write bulk remove edge transaction $payload", e)
      }
    }
  }

  /** This does not add edge properties as they are often not used in the CPG.
    */
  private def bulkCreateEdge(ops: Seq[Change.CreateEdge], dg: AppliedDiffGraph): Unit = {
    val ms = ops
      .flatMap { case Change.CreateEdge(src, dst, _, _) =>
        val srcId = id(src, dg)
        val dstId = id(dst, dg)
        Seq(s"(n$srcId {ID: $srcId})", s"(n$dstId {ID: $dstId})")
      }
      .toSet
      .mkString(",")
    val cs = ops.zipWithIndex
      .map { case (Change.CreateEdge(src, dst, label, _), i) =>
        val srcId = id(src, dg)
        val dstId = id(dst, dg)
        s"(n$srcId)-[r$i:$label]->(n$dstId)"
      }
      .mkString(",")
    Using.resource(driver.session()) { session =>
      val payload = s"""
                       |MATCH $ms
                       |CREATE $cs
                       |""".stripMargin
      try {
        session.writeTransaction { tx => tx.run(payload) }
      } catch {
        case e: Exception =>
          logger.error(s"Unable to write bulk create edge transaction $payload", e)
      }
    }
  }

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Node operations
    dg.diffGraph.iterator
      .collect { case x: Change.RemoveNode => x }
      .grouped(50)
      .foreach(bulkDeleteNode)
    dg.diffGraph.iterator
      .collect { case x: Change.CreateNode => x }
      .grouped(50)
      .foreach(bulkCreateNode(_, dg))
    dg.diffGraph.iterator
      .collect { case x: Change.SetNodeProperty => x }
      .grouped(50)
      .foreach(bulkNodeSetProperty)
    // Edge operations
    dg.diffGraph.iterator
      .collect { case x: Change.RemoveEdge => x }
      .grouped(50)
      .foreach(bulkRemoveEdge)
    dg.diffGraph.iterator
      .collect { case x: Change.CreateEdge => x }
      .grouped(50)
      .foreach(bulkCreateEdge(_, dg))
  }

  override def deleteNodeWithChildren(
      nodeType: String,
      edgeToFollow: String,
      propertyKey: String,
      propertyValue: Any
  ): Unit = Using.resource(driver.session()) { session =>
    val valueStr = propertyValue match {
      case x: Int  => s"$x"
      case x: Long => s"$x"
      case x       => s"\'$x\'"
    }
    session.writeTransaction { tx =>
      tx
        .run(s"""
                |MATCH (a:$nodeType)-[r:$edgeToFollow*]->(t)
                |WHERE a.$propertyKey = $valueStr
                |FOREACH (x IN r | DELETE x)
                |DETACH DELETE a, t
                |""".stripMargin)
        .next()["edge_exists"]
        .toString == "TRUE"
    }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] =
    Using.resource(driver.session()) { session =>
      val typeSystem = driver.defaultTypeSystem()
      session.writeTransaction { tx =>
        tx
          .run(s"""
                  |MATCH (n: $nodeType)
                  |RETURN ${(keys.map(f => s"n.$f as $f") :+ "n.ID as id").mkString(",")}
                  |""".stripMargin)
          .list()
          .asScala
          .map(record =>
            (keys :+ "ID").map { k: String =>
              val v = record.get(k)
              if (k == "ID") {
                k -> v.asLong(-1L)
              } else if (v.hasType(typeSystem.INTEGER())) {
                k -> v.asInt(-1)
              } else if (v.hasType(typeSystem.LIST())) {
                k -> v.asList[String](java.util.List[String]).asScala
              } else {
                k -> v.asString("<empty>")
              }
            }.toMap
          )
          .toList
      }
    }

  override def idInterval(lower: Long, upper: Long): Set[Long] = Using.resource(driver.session()) {
    session =>
      session.writeTransaction { tx =>
        tx
          .run(s"""
                |MATCH (n)
                |WHERE n.ID >= $lower AND n.ID <= $upper
                |RETURN n.ID as id
                |""".stripMargin)
          .list()
          .asScala
          .map(record => record.get("id").asLong())
          .toSet
      }
  }

}

object Neo4jDriver {

  /** Default username for the Neo4j server.
    */
  private val DEFAULT_USERNAME = "neo4j"

  /** Default password for the Neo4j server.
    */
  private val DEFAULT_PASSWORD = "neo4j"

  /** Default hostname for the Neo4j server.
    */
  private val DEFAULT_HOSTNAME = "localhost"

  /** Default port number a remote Bolt server.
    */
  private val DEFAULT_PORT = 7687
}
