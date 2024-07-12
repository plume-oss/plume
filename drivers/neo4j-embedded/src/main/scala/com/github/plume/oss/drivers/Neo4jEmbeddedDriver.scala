package com.github.plume.oss.drivers

import better.files.File
import com.github.plume.oss.drivers.Neo4jEmbeddedDriver.*
import com.github.plume.oss.util.BatchedUpdateUtil.*
import io.shiftleft.codepropertygraph.generated.nodes.StoredNode
import org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME
import org.neo4j.dbms.api.{DatabaseManagementService, DatabaseManagementServiceBuilder}
import org.neo4j.graphdb.{GraphDatabaseService, Label, Transaction}
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.{CreateEdge, DiffOrBuilder, SetNodeProperty}
import overflowdb.{BatchedUpdate, DetachedNodeData}

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try, Using}

/** The driver used to connect to a remote Neo4j instance. Once can optionally call buildSchema to add indexes for
  * improved performance on larger graphs.
  */
final class Neo4jEmbeddedDriver(
  databaseName: String = DEFAULT_DATABASE_NAME,
  databaseDir: File = DEFAULT_DATABASE_DIR,
  txMax: Int = DEFAULT_TX_MAX
) extends IDriver
    with ISchemaSafeDriver {

  private val logger            = LoggerFactory.getLogger(getClass)
  private val connected         = new AtomicBoolean(true)
  private var managementService = new DatabaseManagementServiceBuilder(databaseDir.path).build()
  registerShutdownHook(managementService)
  private var graphDb = managementService.database(databaseName)

  private def registerShutdownHook(managementService: DatabaseManagementService): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        managementService.shutdown()
      }
    })
  }

  def graph: GraphDatabaseService = graphDb

  private def connect(): Unit = {
    managementService = new DatabaseManagementServiceBuilder(databaseDir.path).build()
    graphDb = managementService.database(databaseName)
    connected.set(true)
  }

  override def isConnected: Boolean = connected.get()

  override def clear(): Unit = {
    close()
    databaseDir.delete(swallowIOExceptions = true)
    connect()
  }

  override def close(): Unit =
    Try(managementService.shutdown()) match {
      case Failure(e) => logger.warn("Exception thrown while attempting to close graph.", e)
      case Success(_) => connected.set(false)
    }

  override def exists(nodeId: Long): Boolean =
    Using.resource(graphDb.beginTx) { tx =>
      tx
        .execute(
          s"""
               |MATCH (n)
               |WHERE n.id = $$nodeId
               |RETURN n
               |""".stripMargin,
          new util.HashMap[String, Object](1) {
            put("nodeId", nodeId.asInstanceOf[Object])
          }
        )
        .asScala
        .nonEmpty
    }

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    Using.resource(graphDb.beginTx) { tx =>
      val edgeExists = tx
        .execute(
          s"""
               |MATCH (a), (b)
               |WHERE a.id = $$srcId AND b.id = $$dstId
               |RETURN EXISTS ((a)-[:$edge]->(b)) as edge_exists
               |""".stripMargin,
          new util.HashMap[String, Object](2) {
            put("srcId", srcId.asInstanceOf[Object])
            put("dstId", dstId.asInstanceOf[Object])
          }
        )
        .next()
        .get("edge_exists")
        .toString
      edgeExists == "true"
    }

  private def nodePayload(n: DetachedNodeData): (util.Map[String, Object], String) = {
    val pMap = propertiesFromNodeData(n).map { case (k, v) =>
      k -> (v match {
        case x: String  => x
        case xs: Seq[_] => CollectionConverters.IterableHasAsJava(xs.toList).asJava
        case x          => x
      })
    }.toMap ++ Map("id" -> idFromNodeData(n))

    nodePropertiesToCypherQuery(pMap)
  }

  private def nodePropertiesToCypherQuery(pMap: Map[String, Any]) = {
    val pString = pMap.map { case (k, _) => s"$k:$$$k" }.mkString(",")
    val jpMap   = new util.HashMap[String, Object](pMap.size)
    pMap.foreach { case (x, y) => jpMap.put(x, y.asInstanceOf[Object]) }
    (jpMap, pString)
  }

  private def bulkCreateNode(ops: Seq[DetachedNodeData]): Unit =
    Using.resource(graphDb.beginTx) { tx =>
      ops
        .map { change =>
          val nodeId = change.pID
          change.setRefOrId(nodeId)
          val (params, pString) = nodePayload(change)
          params -> s"MERGE (n:${change.label} {$pString})"
        }
        .foreach { case (params: util.Map[String, Object], query: String) =>
          Try(tx.execute(query, params)) match {
            case Failure(e) =>
              logger.error(s"Unable to write bulk create node transaction $query", e)
            case Success(_) =>
          }
        }
      tx.commit()
    }

  private def bulkNodeSetProperty(ops: Seq[BatchedUpdate.SetNodeProperty]): Unit =
    Using.resource(graphDb.beginTx) { tx =>
      ops
        .collect { case c: BatchedUpdate.SetNodeProperty =>
          (c.label, c.value, c.node)
        }
        .map { case (key: String, value: Any, node: StoredNode) =>
          val newV = value match {
            case x: String => "\"" + x + "\""
            case Seq()     => SchemaBuilder.STRING_DEFAULT
            case xs: Seq[_] =>
              "[" + xs.map { x => Seq("\"", x, "\"").mkString }.mkString(",") + "]"
            case x: Number => x.toString
            case x         => logger.warn(s"Unhandled property $x (${x.getClass}")
          }
          (
            new util.HashMap[String, Object](2) {
              put("nodeId", node.id().asInstanceOf[Object])
              put("newV", newV.asInstanceOf[Object])
            },
            s"""
                 |MATCH (n:${node.label} {id: $$nodeId})
                 |SET n.$key = $$newV
                 |""".stripMargin
          )
        }
        .foreach { case (params: util.Map[String, Object], query: String) =>
          Try(tx.execute(query, params)) match {
            case Failure(e) =>
              logger.error(s"Unable to write bulk set node property transaction $query", e)
            case Success(_) =>
          }
        }
      tx.commit()
    }

  private def bulkCreateEdge(ops: Seq[BatchedUpdate.CreateEdge]): Unit =
    Using.resource(graphDb.beginTx) { tx =>
      ops.foreach { c =>
        val srcLabel = labelFromNodeData(c.src)
        val dstLabel = labelFromNodeData(c.dst)
        val query = s"""
                         |MATCH (src:$srcLabel {id: $$srcId}), (dst:$dstLabel {id: $$dstId})
                         |CREATE (src)-[:${c.label}]->(dst)
                         |""".stripMargin
        Try(
          tx.execute(
            query,
            new util.HashMap[String, Object](2) {
              put("srcId", idFromNodeData(c.src).asInstanceOf[Object])
              put("dstId", idFromNodeData(c.dst).asInstanceOf[Object])
            }
          )
        ) match {
          case Failure(e) =>
            logger.error(s"Unable to write bulk create edge transaction $query", e)
          case Success(_) =>
        }
      }
      tx.commit()
    }

  override def bulkTx(dg: DiffOrBuilder): Int = {
    // Node operations
    dg.iterator.asScala
      .collect { case x: DetachedNodeData => x }
      .grouped(txMax)
      .foreach(bulkCreateNode)
    dg.iterator.asScala
      .collect { case x: BatchedUpdate.SetNodeProperty => x }
      .grouped(txMax)
      .foreach(bulkNodeSetProperty)
    // Edge operations
    dg.iterator.asScala
      .collect { case x: BatchedUpdate.CreateEdge => x }
      .grouped(txMax)
      .foreach(bulkCreateEdge)

    dg.size()
  }

  private def runPayload(
    tx: Transaction,
    filePayload: String,
    params: util.HashMap[String, Object] = new util.HashMap[String, Object](0)
  ) = {
    try {
      tx.execute(filePayload, params)
    } catch {
      case e: Exception => logger.error(s"Unable to link AST nodes: $filePayload", e)
    }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] =
    Using.resource(graphDb.beginTx) { tx =>
      tx.findNodes(Label.label(nodeType))
        .map { node =>
          Map("id" -> node.getProperty("id")) ++ keys.map { k =>
            k -> Try(node.getProperty(k)).getOrElse(SchemaBuilder.getPropertyDefault(k))
          }.toMap
        }
        .asScala
        .toList
    }

  override def buildSchema(): Unit = {
    Using.resource(graphDb.beginTx) { tx =>
      val payload = buildSchemaPayload()
      try {
        payload.lines().forEach(line => tx.execute(line))
      } catch {
        case e: Exception =>
          logger.error(s"Unable to set schema: $payload", e)
      }
    }
  }

  override def buildSchemaPayload(): String = {
    val btreeAndConstraints = NODES_IN_SCHEMA
      .map(l => s"""
                   |CREATE BTREE INDEX ${l.toLowerCase}_id_btree_index IF NOT EXISTS FOR (n:$l) ON (n.id)
                   |""".stripMargin.trim)
      .mkString("\n")
    s"""CREATE LOOKUP INDEX node_label_lookup_index IF NOT EXISTS FOR (n) ON EACH labels(n)
       |$btreeAndConstraints""".stripMargin
  }
}

object Neo4jEmbeddedDriver {

  /** Default database directory file is a temporary directory.
    */
  private val DEFAULT_DATABASE_DIR = File.newTemporaryDirectory("plume-").deleteOnExit(swallowIOExceptions = true)

  /** Default maximum number of transactions to bundle in a single transaction
    */
  private val DEFAULT_TX_MAX = 25
}
