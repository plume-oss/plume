package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.drivers.Neo4jDriver.*
import com.github.plume.oss.util.BatchedUpdateUtil.*
import io.shiftleft.codepropertygraph.generated.nodes.StoredNode
import org.neo4j.driver.{AuthTokens, GraphDatabase, Transaction}
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.{CreateEdge, DiffOrBuilder, SetNodeProperty}
import overflowdb.{BatchedUpdate, DetachedNodeData}

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala}
import scala.util.{Failure, Success, Try, Using}

/** The driver used to connect to a remote Neo4j instance. Once can optionally call buildSchema to add indexes for
  * improved performance on larger graphs.
  */
final class Neo4jDriver(
  hostname: String = DEFAULT_HOSTNAME,
  port: Int = DEFAULT_PORT,
  username: String = DEFAULT_USERNAME,
  password: String = DEFAULT_PASSWORD,
  txMax: Int = DEFAULT_TX_MAX
) extends IDriver
    with ISchemaSafeDriver {

  private val logger    = LoggerFactory.getLogger(classOf[Neo4jDriver])
  private val connected = new AtomicBoolean(true)
  private val driver =
    PlumeStatistics.time(
      PlumeStatistics.TIME_OPEN_DRIVER,
      { GraphDatabase.driver(s"bolt://$hostname:$port", AuthTokens.basic(username, password)) }
    )
  private val typeSystem = driver.defaultTypeSystem()

  override def isConnected: Boolean = connected.get()

  override def clear(): Unit = Using.resource(driver.session()) { session =>
    session.executeWrite { tx =>
      tx.run("""
          |MATCH (n)
          |DETACH DELETE n
          |""".stripMargin)
        .consume()
    }
  }

  override def close(): Unit = PlumeStatistics.time(
    PlumeStatistics.TIME_CLOSE_DRIVER, {
      Try(driver.close()) match {
        case Failure(e) => logger.warn("Exception thrown while attempting to close graph.", e)
        case Success(_) => connected.set(false)
      }
    }
  )

  override def exists(nodeId: Long): Boolean = Using.resource(driver.session()) { session =>
    session.executeRead { tx =>
      CollectionHasAsScala(
        tx
          .run(
            s"""
               |MATCH (n)
               |WHERE n.id = $$nodeId
               |RETURN n
               |""".stripMargin,
            new util.HashMap[String, Object](1) {
              put("nodeId", nodeId.asInstanceOf[Object])
            }
          )
          .list
      ).asScala.nonEmpty
    }
  }

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    Using.resource(driver.session()) { session =>
      session.executeRead { tx =>
        tx
          .run(
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
          .asBoolean(false)
      }
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
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops
          .map { change =>
            val nodeId = change.pID
            change.setRefOrId(nodeId)
            val (params, pString) = nodePayload(change)
            params -> s"MERGE (n:${change.label} {$pString})"
          }
          .foreach { case (params: util.Map[String, Object], query: String) =>
            Try(tx.run(query, params)) match {
              case Failure(e) =>
                logger.error(s"Unable to write bulk create node transaction $query", e)
              case Success(_) =>
            }
          }
        tx.commit()
      }
    }

  private def bulkNodeSetProperty(ops: Seq[BatchedUpdate.SetNodeProperty]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
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
            Try(tx.run(query, params)) match {
              case Failure(e) =>
                logger.error(s"Unable to write bulk set node property transaction $query", e)
              case Success(_) =>
            }
          }
        tx.commit()
      }
    }

  private def bulkCreateEdge(ops: Seq[BatchedUpdate.CreateEdge]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops.foreach { c =>
          val srcLabel = labelFromNodeData(c.src)
          val dstLabel = labelFromNodeData(c.dst)
          val query = s"""
                         |MATCH (src:$srcLabel {id: $$srcId}), (dst:$dstLabel {id: $$dstId})
                         |CREATE (src)-[:${c.label}]->(dst)
                         |""".stripMargin
          Try(
            tx.run(
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
      tx.run(filePayload, params)
    } catch {
      case e: Exception => logger.error(s"Unable to link AST nodes: $filePayload", e)
    }
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] =
    Using.resource(driver.session()) { session =>
      session.readTransaction { tx =>
        tx
          .run(s"""
                  |MATCH (n: $nodeType)
                  |RETURN ${(keys.map(f => s"n.$f as $f") :+ "n.id as id").mkString(",")}
                  |""".stripMargin)
          .list()
          .asScala
          .map(record =>
            (keys :+ "id").flatMap { k =>
              val v = record.get(k)
              if (v.hasType(typeSystem.NULL())) {
                Some(k -> SchemaBuilder.getPropertyDefault(k))
              } else if (k == "id") {
                Some(k -> v.asLong(SchemaBuilder.LONG_DEFAULT))
              } else if (v.hasType(typeSystem.INTEGER())) {
                Some(k -> v.asInt(SchemaBuilder.INT_DEFAULT))
              } else if (v.hasType(typeSystem.BOOLEAN())) {
                Some(k -> v.asBoolean(SchemaBuilder.BOOL_DEFAULT))
              } else if (v.hasType(typeSystem.STRING())) {
                Some(k -> v.asString(SchemaBuilder.STRING_DEFAULT))
              } else if (v.hasType(typeSystem.LIST())) {
                Some(k -> v.asList())
              } else {
                None
              }
            }.toMap
          )
          .toList
      }
    }

  override def buildSchema(): Unit = {
    Using.resource(driver.session()) { session =>
      session.writeTransaction { tx =>
        val payload = buildSchemaPayload()
        try {
          payload.lines().forEach(line => tx.run(line))
        } catch {
          case e: Exception =>
            logger.error(s"Unable to set schema: $payload", e)
        }
      }
    }
  }

  override def buildSchemaPayload(): String = {
    val btreeAndConstraints = NODES_IN_SCHEMA
      .map(l => s"""
          |CREATE RANGE INDEX ${l.toLowerCase}_id_btree_index IF NOT EXISTS FOR (n:$l) ON (n.id)
          |""".stripMargin.trim)
      .mkString("\n")
    s"""CREATE LOOKUP INDEX node_label_lookup_index IF NOT EXISTS FOR (n) ON EACH labels(n)
      |$btreeAndConstraints""".stripMargin
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

  /** Default maximum number of transactions to bundle in a single transaction
    */
  private val DEFAULT_TX_MAX = 25
}
