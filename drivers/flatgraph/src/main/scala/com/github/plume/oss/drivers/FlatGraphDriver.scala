package com.github.plume.oss.drivers

import com.github.plume.oss.drivers.FlatGraphDriver.newOverflowGraph
import com.github.plume.oss.util.BatchedUpdateUtil
import com.github.plume.oss.util.BatchedUpdateUtil.*
import io.shiftleft.codepropertygraph.cpgloading.CpgLoader
import io.shiftleft.codepropertygraph.generated.*
import io.shiftleft.codepropertygraph.generated.nodes.AbstractNode
import org.apache.commons.text.StringEscapeUtils
import io.shiftleft.codepropertygraph.generated.language.*
import org.slf4j.LoggerFactory
import flatgraph.{DiffGraphBuilder, DiffGraphApplier, GNode}
import java.io.{FileOutputStream, OutputStreamWriter, File as JFile}
import java.nio.file.Path
import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsScala}
import scala.util.*

/** Driver to create an FlatGraph database file.
  * @param storageLocation
  *   where the database will serialize to and deserialize from.
  */
final class FlatGraphDriver(
  storageLocation: Option[String] = Option(JFile.createTempFile("plume-", ".fg").getAbsolutePath)
) extends IDriver {

  private val logger = LoggerFactory.getLogger(classOf[FlatGraphDriver])

  /** A direct pointer to the code property graph object.
    */
  val cpg: Cpg = storageLocation match {
    case Some(path) => Cpg.withStorage(Path.of(path))
    case None       => Cpg.empty
  }

  override def isConnected: Boolean = !cpg.graph.isClosed

  override def close(): Unit = Try(cpg.close()) match {
    case Success(_) =>
    case Failure(e) =>
      logger.warn("Exception thrown while attempting to close graph.", e)
  }

  override def clear(): Unit = {
    cpg.all.grouped(1000).foreach(batchedRemoval)
  }

  override def exists(nodeId: Long): Boolean = cpg.graph.node(nodeId) != null

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean =
    cpg.graph.node(srcId).out(edge).exists { dst => dst.id() == dstId }

  override def bulkTx(dg: DiffGraphBuilder): Int = {
    DiffGraphApplier.applyDiff(cpg.graph, dg)
    dg.size
  }

  private def batchedRemoval(ns: Iterable[GNode]): Unit = {
    val dg = Cpg.newDiffGraphBuilder
    ns.filterNot(_ == null).foreach(dg.removeNode)
    DiffGraphApplier.applyDiff(cpg.graph, dg)
  }

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] =
    cpg.graph
      .nodes(nodeType)
      .collect { case n: AbstractNode =>
        keys.map { k =>
          k -> n.properties.get(k)
        }.toMap + ("id" -> n.id())
      }
      .toList

}

object FlatGraphDriver {
  def newOverflowGraph(storagePath: Path): Cpg = Cpg.withStorage(storagePath)
}
