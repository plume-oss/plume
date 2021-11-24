package io.github.plume.oss.drivers

import io.circe.Encoder
import io.circe.generic.codec.DerivedAsObjectCodec.deriveCodec
import io.github.plume.oss.drivers.NeptuneDriver.DEFAULT_PORT
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewNode, StoredNode}
import io.shiftleft.passes.AppliedDiffGraph
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.{GraphTraversalSource, __}
import org.apache.tinkerpop.gremlin.process.traversal.{AnonymousTraversalSource, P}
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3.circe._
import sttp.client3.{Empty, HttpURLConnectionBackend, Identity, RequestT, SttpBackend, basicRequest}
import sttp.model.Uri

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.util.Using

class NeptuneDriver(
    hostname: String,
    port: Int = DEFAULT_PORT,
    keyCertChainFile: String = "src/main/resources/conf/SFSRootCAC2.pem"
) extends GremlinDriver {

  override protected val logger: Logger = LoggerFactory.getLogger(classOf[NeptuneDriver])

  implicit val initiateResetEncoder: Encoder[InitiateResetBody] =
    Encoder.forProduct1("action")(u => u.action)
  implicit val performResetEncoder: Encoder[PerformResetBody] =
    Encoder.forProduct2("action", "token")(u => (u.action, u.token))

  private val cluster = Cluster
    .build()
    .addContactPoints(hostname)
    .port(port)
    .enableSsl(true)
    .keyCertChainFile(keyCertChainFile)
    .create()

  override def traversal(): GraphTraversalSource =
    AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster))

  override def isConnected: Boolean = !cluster.isClosed

  override def clear(): Unit = {
    val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
    val noVs                                = Using.resource(traversal()) { g => g.V().count().next() }
    if (noVs < 10000) {
      Using.resource(traversal()) { g =>
        var deleted = 0L
        val step    = 100
        while (deleted < noVs) {
          g.V().sample(step).drop().iterate()
          deleted += step
        }
      }
    } else {
      val systemUri = Uri("https", hostname, port).addPath(Seq("system"))
      val token: String = request()
        .post(systemUri)
        .body(InitiateResetBody())
        .response(asJson[InitiateResetResponse])
        .send(backend)
        .body match {
        case Left(e)              => logger.error("Unable to initiate database reset!"); throw e
        case Right(resetResponse) => resetResponse.token
      }
      request()
        .post(systemUri)
        .body(PerformResetBody(token))
        .send(backend)
        .body match {
        case Left(e) =>
          logger.error("Unable to perform database reset!")
          throw new RuntimeException(e)
        case Right(_) =>
          val statusUri = Uri("https", hostname, port).addPath(Seq("status"))
          Iterator
            .continually(
              request()
                .get(statusUri)
                .response(asJson[InstanceStatusResponse])
                .send(backend)
                .body
            )
            .takeWhile {
              case Left(e)         => logger.warn("Unable to obtain instance status", e); true
              case Right(response) => if (response.status == "healthy") true else false
            }
            .foreach(_ => Thread.sleep(5000))
      }
    }
  }

  case class InitiateResetBody(action: String = "initiateDatabaseReset")
  case class InitiateResetResponse(token: String)
  case class PerformResetBody(token: String, action: String = "performDatabaseReset")
  case class InstanceStatusResponse(status: String)

  override def id(node: AbstractNode, dg: AppliedDiffGraph): Any =
    node match {
      case n: NewNode    => dg.nodeToGraphId(n).toString
      case n: StoredNode => n.id().toString
      case _             => throw new RuntimeException(s"Unable to obtain ID for $node")
    }

  override def idInterval(lower: Long, upper: Long): Set[Long] =
    Using.resource(traversal()) { g =>
      g.V()
        .id()
//        .map(new java.util.function.Function[Traverser[String], Long] {
//          override def apply(t: Traverser[String]): Long = t.get().toLong
//        })
        .filter(__.is(P.gte((lower - 1).toString).and(P.lte(upper.toString))))
        .asScala
        .map(_.toString.toLong)
        .toSet
    }

  override def close(): Unit =
    try {
      cluster.close()
    } catch {
      case e: Exception => logger.error("Exception thrown while attempting to close graph.", e)
    }

  private def request(): RequestT[Empty, Either[String, String], Any] =
    basicRequest
      .contentType("application/json")
}

object NeptuneDriver {

  /** Default port number a remote Gremlin server.
    */
  private val DEFAULT_PORT = 8182
}
