package io.github.plume.oss.drivers

import io.github.plume.oss.drivers.NeptuneDriver.DEFAULT_PORT
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewNode, StoredNode}
import io.shiftleft.passes.AppliedDiffGraph
import org.apache.commons.configuration.BaseConfiguration
import org.apache.tinkerpop.gremlin.driver.Cluster
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Graph
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3.{Empty, RequestT, basicRequest}

import scala.util.Using

class NeptuneDriver(
    hostname: String,
    port: Int = DEFAULT_PORT,
    keyCertChainFile: String = "src/main/resources/conf/SFSRootCAC2.pem"
) extends GremlinDriver {

  override protected val logger: Logger = LoggerFactory.getLogger(classOf[NeptuneDriver])

  private val cluster = Cluster
    .build()
    .addContactPoints(hostname)
    .port(port)
    .enableSsl(true)
    .keyCertChainFile(keyCertChainFile)
    .create()

  override protected val config: BaseConfiguration = null
  override protected val graph: Graph              = null

  override def traversal(): GraphTraversalSource =
    AnonymousTraversalSource.traversal().withRemote(DriverRemoteConnection.using(cluster))

  override def isConnected: Boolean = !cluster.isClosed

  override def clear(): Unit = {
    val noVs = Using.resource(traversal()) { g => g.V().count().next() }
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
      // TODO: Do HTTP bulk delete
    }
  }

  override def id(node: AbstractNode, dg: AppliedDiffGraph): Any =
    node match {
      case n: NewNode    => dg.nodeToGraphId(n).toString
      case n: StoredNode => n.id().toString
      case _             => throw new RuntimeException(s"Unable to obtain ID for $node")
    }

  override def idInterval(lower: Long, upper: Long): Set[Long] = ???

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
