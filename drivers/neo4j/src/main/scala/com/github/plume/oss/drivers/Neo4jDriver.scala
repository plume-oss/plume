package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.drivers.Neo4jDriver._
import com.github.plume.oss.util.BatchedUpdateUtil._
import io.shiftleft.codepropertygraph.generated.nodes.{NewNode, StoredNode}
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, PropertyNames}
import io.shiftleft.passes.AppliedDiffGraph
import io.shiftleft.passes.DiffGraph.Change
import org.neo4j.driver.{AuthTokens, GraphDatabase, Transaction, Value}
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.AppliedDiff
import overflowdb.{BatchedUpdate, DetachedNodeData}

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
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
    session.writeTransaction { tx =>
      tx.run(
        """
          |MATCH (n)
          |DETACH DELETE n
          |""".stripMargin
      )
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
    session.writeTransaction { tx =>
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
      session.writeTransaction { tx =>
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

  private def nodePayload(id: Long, n: NewNode): (util.Map[String, Object], String) = {
    val pMap = n.properties.map { case (k, v) =>
      k -> (v match {
        case x: String  => x
        case xs: Seq[_] => CollectionConverters.IterableHasAsJava(xs.toList).asJava
        case x          => x
      })
    } ++ Map("id" -> id)

    nodePropertiesToCypherQuery(pMap)
  }

  private def nodePayload(n: DetachedNodeData): (util.Map[String, Object], String) = {
    val pMap = propertiesFromNodeData(n).map { case (k, v) =>
      k -> (v match {
        case x: String  => x
        case xs: Seq[_] => CollectionConverters.IterableHasAsJava(xs.toList).asJava
        case x          => x
      })
    } ++ Map("id" -> idFromNodeData(n))

    nodePropertiesToCypherQuery(pMap)
  }

  private def nodePropertiesToCypherQuery(pMap: Map[String, Any]) = {
    val pString = pMap.map { case (k, _) => s"$k:$$$k" }.mkString(",")
    val jpMap   = new util.HashMap[String, Object](pMap.size)
    pMap.foreach { case (x, y) => jpMap.put(x, y.asInstanceOf[Object]) }
    (jpMap, pString)
  }

  private def bulkDeleteNode(ops: Seq[Any]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops
          .collect {
            case c: Change.RemoveNode        => (c.nodeId, None)
            case c: BatchedUpdate.RemoveNode => (c.node.id(), Some(c.node.label()))
          }
          .map { case (nodeId: Long, maybeLabel: Option[String]) =>
            val label = maybeLabel match {
              case Some(value) => s":$value"
              case None        => ""
            }
            (
              s"""
                |MATCH (n$label {id:$$nodeId})
                |DETACH DELETE (n)
                |""".stripMargin,
              nodeId
            )
          }
          .foreach { case (query, nodeId) =>
            Try(
              tx.run(
                query,
                new util.HashMap[String, Object](1) {
                  put("nodeId", nodeId.asInstanceOf[Object])
                }
              )
            ) match {
              case Failure(e) =>
                logger.error(s"Unable to write bulk delete node transaction $query", e)
              case Success(_) =>
            }
          }
        tx.commit()
      }
    }

  private def bulkCreateNode(ops: Seq[Change.CreateNode], dg: AppliedDiffGraph): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops
          .map { case Change.CreateNode(node) =>
            val (params, pString) = nodePayload(dg.nodeToGraphId(node), node)
            params -> s"MERGE (n:${node.label} {$pString})"
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

  private def bulkCreateNode(ops: Seq[DetachedNodeData]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops
          .map { c: DetachedNodeData =>
            val (params, pString) = nodePayload(c)
            params -> s"MERGE (n:${c.label} {$pString})"
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

  private def bulkNodeSetProperty(ops: Seq[Any]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops
          .collect {
            case c: BatchedUpdate.SetNodeProperty =>
              (c.label, c.value, c.node)
            case c: Change.SetNodeProperty =>
              (c.key, c.value, c.node)
          }
          .map { case (key: String, value: Any, node: StoredNode) =>
            val newV = value match {
              case x: String => "\"" + x + "\""
              case Seq()     => IDriver.STRING_DEFAULT
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

  private def bulkRemoveEdge(ops: Seq[Change.RemoveEdge]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops
          .map { case Change.RemoveEdge(edge) =>
            val srcLabel = edge.outNode().label()
            val dstLabel = edge.inNode().label()
            (
              new util.HashMap[String, Object](2) {
                put("srcId", edge.outNode().id().asInstanceOf[Object])
                put("dstId", edge.inNode().id().asInstanceOf[Object])
              },
              s"""
                 |MATCH (n:$srcLabel {id: $$srcId})-[r:${edge.label()}]->(m:$dstLabel {id: $$dstId})
                 |DELETE r
                 |""".stripMargin
            )
          }
          .foreach { case (params: util.Map[String, Object], query: String) =>
            Try(tx.run(query, params)) match {
              case Failure(e) =>
                logger.error(s"Unable to write bulk remove edge property transaction $query", e)
              case Success(_) =>
            }
          }
        tx.commit()
      }
    }

  /** This does not add edge properties as they are often not used in the CPG.
    */
  private def bulkCreateEdge(ops: Seq[Change.CreateEdge], dg: AppliedDiffGraph): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops.foreach { case Change.CreateEdge(src, dst, label, _) =>
          val query = s"""
                         |MATCH (src:${src.label} {id: $$srcId}), (dst:${dst.label} {id: $$dstId})
                         |CREATE (src)-[:$label]->(dst)
                         |""".stripMargin
          Try(
            tx.run(
              query,
              new util.HashMap[String, Object](2) {
                put("srcId", id(src, dg).asInstanceOf[Object])
                put("dstId", id(dst, dg).asInstanceOf[Object])
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

  private def bulkCreateEdge(ops: Seq[BatchedUpdate.CreateEdge]): Unit =
    Using.resource(driver.session()) { session =>
      Using.resource(session.beginTransaction()) { tx =>
        ops.foreach { c: BatchedUpdate.CreateEdge =>
          val srcLabel = labelFromNodeData(c.src)
          val dstLabel = labelFromNodeData(c.dst)
          val query    = s"""
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

  override def bulkTx(dg: AppliedDiffGraph): Unit = {
    // Node operations
    dg.diffGraph.iterator
      .collect { case x: Change.RemoveNode => x }
      .grouped(txMax)
      .foreach(bulkDeleteNode)
    dg.diffGraph.iterator
      .collect { case x: Change.CreateNode => x }
      .grouped(txMax)
      .foreach(bulkCreateNode(_, dg))
    dg.diffGraph.iterator
      .collect { case x: Change.SetNodeProperty => x }
      .grouped(txMax)
      .foreach(bulkNodeSetProperty)
    // Edge operations
    dg.diffGraph.iterator
      .collect { case x: Change.RemoveEdge => x }
      .grouped(txMax)
      .foreach(bulkRemoveEdge)
    dg.diffGraph.iterator
      .collect { case x: Change.CreateEdge => x }
      .grouped(txMax)
      .foreach(bulkCreateEdge(_, dg))
  }

  override def bulkTx(dg: AppliedDiff): Unit = {
    dg.diffGraph.iterator.asScala
      .collect { case x: DetachedNodeData => x }
      .grouped(txMax)
      .foreach(bulkCreateNode)
    dg.diffGraph.iterator.asScala
      .collect { case x: BatchedUpdate.SetNodeProperty => x }
      .grouped(txMax)
      .foreach(bulkNodeSetProperty)
    dg.diffGraph.iterator.asScala
      .collect { case x: BatchedUpdate.RemoveNode => x }
      .grouped(txMax)
      .foreach(bulkDeleteNode)
    dg.diffGraph.iterator.asScala
      .collect { case x: BatchedUpdate.CreateEdge => x }
      .grouped(txMax)
      .foreach(bulkCreateEdge)
  }

  /** Removes the namespace block with all of its AST children specified by the given FILENAME property.
    */
  private def deleteNamespaceBlockWithAstChildrenByFilename(filename: String): Unit =
    Using.resource(driver.session()) { session =>
      session.writeTransaction { tx =>
        tx
          .run(
            s"""
                |MATCH (a:${NodeTypes.NAMESPACE_BLOCK})-[r:${EdgeTypes.AST}*]->(t)
                |WHERE a.${PropertyNames.FILENAME} = $$filename
                |FOREACH (x IN r | DELETE x)
                |DETACH DELETE a, t
                |""".stripMargin,
            new util.HashMap[String, Object](1) { put("filename", filename.asInstanceOf[Object]) }
          )
      }
    }

  override def removeSourceFiles(filenames: String*): Unit = {
    Using.resource(driver.session()) { session =>
      val fileSet = CollectionConverters.IterableHasAsJava(filenames.toSeq).asJava
      session.writeTransaction { tx =>
        val filePayload = s"""
             |MATCH (f:${NodeTypes.FILE})
             |MATCH (f)<-[:${EdgeTypes.SOURCE_FILE}]-(td:${NodeTypes.TYPE_DECL})<-[:${EdgeTypes.REF}]-(t)
             |WHERE f.NAME IN $$fileSet
             |DETACH DELETE f, t
             |""".stripMargin
        runPayload(
          tx,
          filePayload,
          new util.HashMap[String, Object](1) {
            put("fileSet", fileSet)
          }
        )
      }
    }
    filenames.foreach(deleteNamespaceBlockWithAstChildrenByFilename)
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
            (keys :+ "id").flatMap { k: String =>
              val v = record.get(k)
              if (v.hasType(typeSystem.NULL())) {
                Some(k -> IDriver.getPropertyDefault(k))
              } else if (k == "id") {
                Some(k -> v.asLong(IDriver.LONG_DEFAULT))
              } else if (v.hasType(typeSystem.INTEGER())) {
                Some(k -> v.asInt(IDriver.INT_DEFAULT))
              } else if (v.hasType(typeSystem.BOOLEAN())) {
                Some(k -> v.asBoolean(IDriver.BOOL_DEFAULT))
              } else if (v.hasType(typeSystem.STRING())) {
                Some(k -> v.asString(IDriver.STRING_DEFAULT))
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

  override def idInterval(lower: Long, upper: Long): Set[Long] = Using.resource(driver.session()) {
    session =>
      session.readTransaction { tx =>
        tx
          .run(
            s"""
                |MATCH (n)
                |WHERE n.id >= $$lower AND n.id <= $$upper
                |RETURN n.id as id
                |""".stripMargin,
            new util.HashMap[String, Object](2) {
              put("lower", lower.asInstanceOf[Object])
              put("upper", upper.asInstanceOf[Object])
            }
          )
          .list()
          .asScala
          .map(record => record.get("id").asLong())
          .toSet
      }
  }
  override def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Any],
      dstFullNameKey: String,
      dstNodeType: String
  ): Unit = {
    Using.resource(driver.session()) { session =>
      session.writeTransaction { tx =>
        val payload = s"""
                           |MATCH (n:${srcLabels.mkString(":")})
                           |WHERE EXISTS(n.$dstFullNameKey)
                           | AND n.$dstFullNameKey <> "<empty>"
                           | AND n.$dstFullNameKey <> -1
                           |RETURN n.id AS id, n.$dstFullNameKey as $dstFullNameKey
                           |""".stripMargin
        try {
          tx.run(payload)
            .list()
            .asScala
            .flatMap { record =>
              (record.get(dstFullNameKey) match {
                case x: Value if x.`type`() == typeSystem.LIST() =>
                  x.asList().asScala.collect { case y: String => y }.toSeq
                case x: Value => Seq(x.asString())
              }).map { fullName =>
                record.get("id").asLong() -> dstNodeMap.get(fullName)
              }
            }
            .foreach { case (srcId: Long, maybeDst: Option[Any]) =>
              maybeDst match {
                case Some(dstId) =>
                  val astLinkPayload = s"""
                                          | MATCH (n {id: $$srcId}), (m {id: $$dstId})
                                          | WHERE NOT EXISTS((n)-[:$edgeType]->(m))
                                          | CREATE (n)-[r:$edgeType]->(m)
                                          |""".stripMargin
                  runPayload(
                    tx,
                    astLinkPayload,
                    new util.HashMap[String, Object](2) {
                      put("srcId", srcId.asInstanceOf[Object])
                      put("dstId", dstId.asInstanceOf[Object])
                    }
                  )
                case None =>
              }
            }
        } catch {
          case e: Exception =>
            logger.error(s"Unable to obtain AST nodes to link: $payload", e)
        }
      }
    }
  }

  override def staticCallLinker(): Unit = {
    Using.resource(driver.session()) { session =>
      session.writeTransaction { tx =>
        val payload =
          s"""
             |MATCH (call:${NodeTypes.CALL})
             |MATCH (method:${NodeTypes.METHOD} {${PropertyNames.FULL_NAME}: call.${PropertyNames.METHOD_FULL_NAME}})
             |WHERE NOT EXISTS((call)-[:${EdgeTypes.CALL}]->(method))
             |CREATE (call)-[r:${EdgeTypes.CALL}]->(method)
             |""".stripMargin
        runPayload(tx, payload)
      }
    }
  }

  override def dynamicCallLinker(): Unit = {}

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
          |CREATE BTREE INDEX ${l.toLowerCase}_id_btree_index IF NOT EXISTS FOR (n:$l) ON (n.id)
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
