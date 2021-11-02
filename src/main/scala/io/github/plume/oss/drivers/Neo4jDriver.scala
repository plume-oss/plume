package io.github.plume.oss.drivers
import io.shiftleft.passes.AppliedDiffGraph
import org.slf4j.LoggerFactory
import Neo4jDriver._
import org.neo4j.driver.{AuthTokens, GraphDatabase}

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
               |WHERE id(n) = $nodeId
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
                |WHERE id(a) = $srcId AND id(b) = $dstId
                |RETURN EXISTS ((a)-[:$edge]->(b)) as edge_exists
                |""".stripMargin)
          .next()["edge_exists"]
          .toString == "TRUE"
      }
    }

  override def bulkTx(dg: AppliedDiffGraph): Unit = ???

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

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = ???

  override def idInterval(lower: Long, upper: Long): Set[Long] = Using.resource(driver.session()) {
    session =>
      session.writeTransaction { tx =>
        tx
          .run(s"""
                |MATCH (n)
                |WHERE id(n) <= $lower AND id(n) = $upper
                |RETURN id(n) as id
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
