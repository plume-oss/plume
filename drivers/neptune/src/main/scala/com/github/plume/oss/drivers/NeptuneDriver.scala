package com.github.plume.oss.drivers

import com.github.plume.oss.drivers.NeptuneDriver.DEFAULT_PORT
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.driver.ser.Serializers
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3.*
import sttp.client3.circe.asJson
import sttp.model.Uri

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

final class NeptuneDriver(
  hostname: String,
  port: Int = DEFAULT_PORT,
  keyCertChainFile: String = "src/main/resources/conf/SFSRootCAC2.pem",
  txMax: Int = 50
) extends GremlinDriver(txMax) {

  override protected val logger: Logger = LoggerFactory.getLogger(classOf[NeptuneDriver])

  private val backend = HttpClientSyncBackend()

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

  private def connectToCluster =
    Cluster
      .build()
      .addContactPoints(hostname)
      .port(port)
      .enableSsl(true)
      .maxInProcessPerConnection(32)
      .maxSimultaneousUsagePerConnection(32)
      .serializer(Serializers.GRAPHBINARY_V1D0)
      .keyCertChainFile(keyCertChainFile)
      .create()

  override def g(): GraphTraversalSource = {
    traversalSource match {
      case Some(conn) => conn
      case None =>
        val conn =
          AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster))
        traversalSource = Some(conn)
        conn
    }
  }

  override def isConnected: Boolean = !cluster.isClosed

  override def clear(): Unit = {
    g().V().count().next() match {
      case noVs if noVs == 0L => // do nothing
      case noVs if noVs < 10000L =>
        var deleted = 0L
        val step    = 100
        while (deleted < noVs) {
          g().V().sample(step).drop().iterate()
          deleted += step
        }
      case _ =>
        cluster.close()
        val systemUri =
          Uri("https", hostname, port)
            .addPath(Seq("system"))
        logger.info("Initiating database reset...")

        val initResetRequest = basicRequest
          .post(systemUri)
          .body(Map("action" -> "initiateDatabaseReset"))
          .readTimeout(80.second)
          .response(asJson[InitiateResetResponse])

        val token: String = initResetRequest.send(backend).body match {
          case Left(e) =>
            e.printStackTrace()
            throw new RuntimeException(s"Unable to initiate database reset! $e")
          case Right(resetResponse: InitiateResetResponse) => resetResponse.payload.token
        }

        logger.info("Reset token acquired, performing database reset...")
        val performResetRequest = basicRequest
          .post(systemUri)
          .body(Map("action" -> "performDatabaseReset", "token" -> token))
          .readTimeout(80.second)
          .response(asJson[PerformResetResponse])

        performResetRequest.send(backend).body match {
          case Left(e) =>
            logger.error("Unable to perform database reset!", e)
            throw e
          case Right(resetResponse) =>
            if (!resetResponse.status.contains("200")) {
              throw new RuntimeException(s"Unable to perform database reset! $resetResponse")
            }

            val statusUri = Uri("https", hostname, port).addPath(Seq("status"))
            Iterator
              .continually(
                Try(
                  basicRequest
                    .get(statusUri)
                    .readTimeout(80.second)
                    .response(asJson[InstanceStatusResponse])
                    .send(backend)
                    .body
                ) match {
                  case Failure(exception) => Left(exception)
                  case Success(value)     => value
                }
              )
              .takeWhile {
                case Left(e) =>
                  e.printStackTrace(); logger.warn("Unable to obtain instance status", e); true
                case Right(response) => response.status != "healthy"
              }
              .foreach(_ => Thread.sleep(5000))
        }
        logger.info("Database reset complete, re-connecting to cluster.")
        cluster = connectToCluster
    }
  }

  override def close(): Unit = try {
    cluster.close()
  } catch {
    case e: Exception => logger.error("Exception thrown while attempting to close graph.", e)
  } finally {
    traversalSource = None
    backend.close()
  }

}

object NeptuneDriver {

  /** Default port number a remote Gremlin server.
    */
  private val DEFAULT_PORT = 8182
}

/** The response from Neptune after initiating a database reset.
  * @param status
  *   the status of the system.
  * @param payload
  *   the token used to perform the database reset.
  */
final case class InitiateResetResponse(status: String, payload: TokenPayload)

/** The response from Neptune after performing a database reset.
  * @param status
  *   the status of the system.
  */
final case class PerformResetResponse(status: String)

/** The Neptune token used to correlate database operations.
  * @param token
  *   a string token used for database operations.
  */
final case class TokenPayload(token: String)

/** The response from Neptune when requesting the system status.
  * @param status
  *   the status of the system.
  * @param startTime
  *   set to the UTC time at which the current server process started.
  * @param dbEngineVersion
  *   set to the Neptune engine version running on your DB cluster.
  * @param role
  *   set to "reader" if the instance is a read-replica, or to "writer" if the instance is the primary instance.
  * @param gremlin
  *   contains information about the Gremlin query language available on your cluster. Specifically, it contains a
  *   version field that specifies the current TinkerPop version being used by the engine.
  */
final case class InstanceStatusResponse(
  status: String,
  startTime: String,
  dbEngineVersion: String,
  role: String,
  gremlin: GremlinVersion
)

/** Contains information about the Gremlin query language available on your cluster. Specifically, it contains a version
  * field that specifies the current TinkerPop version being used by the engine.
  * @param version
  *   Gremlin version number.
  */
final case class GremlinVersion(version: String)
