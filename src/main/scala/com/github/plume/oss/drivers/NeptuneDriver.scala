package com.github.plume.oss.drivers

import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.domain._
import com.github.plume.oss.drivers.NeptuneDriver.DEFAULT_PORT
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, jawn}
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.slf4j.{Logger, LoggerFactory}
import scalaj.http.{Http, HttpOptions}
import sttp.model.Uri

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Using

final class NeptuneDriver(
    hostname: String,
    port: Int = DEFAULT_PORT,
    keyCertChainFile: String = "src/main/resources/conf/SFSRootCAC2.pem",
    txMax: Int = 50
) extends GremlinDriver(txMax) {

  override protected val logger: Logger = LoggerFactory.getLogger(classOf[NeptuneDriver])

  lazy implicit val initResetDecoder: Decoder[InitiateResetResponse] =
    deriveDecoder[InitiateResetResponse]
  lazy implicit val tokenDecoder: Decoder[TokenPayload] =
    deriveDecoder[TokenPayload]
  lazy implicit val gremlinVersionDecoder: Decoder[GremlinVersion] =
    deriveDecoder[GremlinVersion]
  lazy implicit val perfResetDecoder: Decoder[PerformResetResponse] =
    deriveDecoder[PerformResetResponse]
  lazy implicit val statusDecoder: Decoder[InstanceStatusResponse] =
    deriveDecoder[InstanceStatusResponse]

  private var cluster = connectToCluster

  private def connectToCluster = PlumeStatistics.time(
    PlumeStatistics.TIME_OPEN_DRIVER, {
      Cluster
        .build()
        .addContactPoints(hostname)
        .port(port)
        .enableSsl(true)
        .keyCertChainFile(keyCertChainFile)
        .create()
    }
  )

  override def traversal(): GraphTraversalSource =
    AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster))

  override def isConnected: Boolean = !cluster.isClosed

  override def clear(): Unit = {
    Using.resource(traversal()) { g => g.V().count().next() } match {
      case noVs if noVs == 0L => // do nothing
      case noVs if noVs < 10000L =>
        Using.resource(traversal()) { g =>
          var deleted = 0L
          val step    = 100
          while (deleted < noVs) {
            g.V().sample(step).drop().iterate()
            deleted += step
          }
        }
      case _ =>
        cluster.close()
        val systemUri =
          Uri("https", hostname, port)
            .addPath(Seq("system"))
        val initResetResponse = Http(systemUri.toString())
          .postForm(Seq("action" -> "initiateDatabaseReset"))
          .option(HttpOptions.readTimeout(10000))
          .asString
        val token: String = jawn.decode[InitiateResetResponse](initResetResponse.body) match {
          case Left(e) =>
            throw new RuntimeException(s"Unable to initiate database reset! $e")
          case Right(resetResponse: InitiateResetResponse) => resetResponse.payload.token
        }
        val performResetResponse = Http(systemUri.toString())
          .postForm(Seq("action" -> "performDatabaseReset", "token" -> token))
          .option(HttpOptions.readTimeout(10000))
          .asString
        jawn.decode[PerformResetResponse](performResetResponse.body) match {
          case Left(e) =>
            logger.error("Unable to perform database reset!")
            throw e
          case Right(resetResponse) =>
            if (!resetResponse.status.contains("200"))
              throw new RuntimeException("Unable to perform database reset!")
            val statusUri = Uri("https", hostname, port).addPath(Seq("status"))
            Iterator
              .continually(
                jawn.decode[InstanceStatusResponse](
                  Http(statusUri.toString())
                    .option(HttpOptions.readTimeout(10000))
                    .asString
                    .body
                )
              )
              .takeWhile {
                case Left(e)         => logger.warn("Unable to obtain instance status", e); true
                case Right(response) => response.status != "healthy"
              }
              .foreach(_ => Thread.sleep(5000))
        }
        cluster = connectToCluster
    }
  }

  override def typedNodeId(nodeId: Long): Any =
    nodeId.toString

  override def idInterval(lower: Long, upper: Long): Set[Long] =
    Using.resource(traversal()) { g =>
      g.V()
        .id()
        .toSet
        .asScala
        .map(_.toString.toLong)
        .filter { x => x >= lower - 1 && x <= upper }
        .toSet
    }

  override def close(): Unit = PlumeStatistics.time(
    PlumeStatistics.TIME_CLOSE_DRIVER, {
      try {
        cluster.close()
      } catch {
        case e: Exception => logger.error("Exception thrown while attempting to close graph.", e)
      }
    }
  )

}

object NeptuneDriver {

  /** Default port number a remote Gremlin server.
    */
  private val DEFAULT_PORT = 8182
}
