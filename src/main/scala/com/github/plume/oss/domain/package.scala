package com.github.plume.oss

import io.joern.dataflowengineoss.queryengine.{PathElement, ReachableByResult, ResultTable}
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{Call, CfgNode, StoredNode}
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters

package object domain {

  private val logger = LoggerFactory.getLogger("com.github.plume.oss.domain")

  /** Converts serialized path results to deserialized ReachableByResults. This is assumed to be called before any nodes
    * are removed from the graph since these results were serialized.
    *
    * @param serTab serialized raw results.
    * @return deserialized ReachableByResults table.
    */
  def deserializeResultTable(
      serTab: ConcurrentHashMap[Long, Vector[SerialReachableByResult]],
      cpg: Cpg
  ): Option[ResultTable] = {
    val resultTable = new ResultTable()

    try {
      CollectionConverters
        .MapHasAsScala(serTab)
        .asScala
        .map { case (id, vec) =>
          if (cpg.graph.nodes(id).hasNext)
            throw new RuntimeException(
              """Current database does not contain references to previous ReachableByResults cache. Unable to re-use
                |old cache.""".stripMargin
            )
          (
            cpg.graph.nodes(id).next(),
            vec.map { f: SerialReachableByResult =>
              SerialReachableByResult.unapply(f, cpg, resultTable)
            }
          )
        }
        .foreach { case (k, v) => resultTable.table.put(k.asInstanceOf[StoredNode], v) }
      Some(resultTable)
    } catch {
      case e: RuntimeException =>
        logger.warn(e.getMessage)
        None
      case e: Exception =>
        logger.error("Unable to deserialize results table.", e)
        None
    }
  }

  case class SerialReachableByResult(
      path: Vector[SerialPathElement],
      table: ConcurrentHashMap[Long, Vector[SerialReachableByResult]],
      callSite: Option[Long],
      callDepth: Int = 0,
      partial: Boolean = false
  )

  object SerialReachableByResult {

    private val logger = LoggerFactory.getLogger(classOf[SerialReachableByResult])

    def apply(
        rbr: ReachableByResult,
        table: ConcurrentHashMap[Long, Vector[SerialReachableByResult]]
    ): SerialReachableByResult = {
      new SerialReachableByResult(
        rbr.path.map(SerialPathElement.apply),
        table,
        rbr.callSite match {
          case Some(call) => Some(call.id())
          case None       => None
        },
        rbr.callDepth,
        rbr.partial
      )
    }

    def unapply(srb: SerialReachableByResult, cpg: Cpg, table: ResultTable): ReachableByResult = {
      ReachableByResult(
        srb.path.map { sbr => SerialPathElement.unapply(sbr, cpg) },
        table,
        if (srb.callSite.isDefined) {
          cpg.graph.nodes(srb.callSite.get).next() match {
            case node: Call => Some(node)
            case n =>
              logger.warn(s"Unable to serialize call node ${n.getClass}.")
              None
          }
        } else {
          None
        },
        srb.callDepth,
        srb.partial
      )
    }
  }

  case class SerialPathElement(
      nodeId: Long,
      visible: Boolean = true,
      resolved: Boolean = true,
      outEdgeLabel: String = ""
  )

  object SerialPathElement {

    private val logger = LoggerFactory.getLogger(classOf[SerialPathElement])

    def apply(pe: PathElement): SerialPathElement = {
      new SerialPathElement(
        pe.node.id(),
        pe.visible,
        pe.resolved,
        pe.outEdgeLabel
      )
    }

    def unapply(spe: SerialPathElement, cpg: Cpg): PathElement = {
      PathElement(
        cpg.graph.nodes(spe.nodeId).next().asInstanceOf[CfgNode],
        spe.visible,
        spe.resolved,
        spe.outEdgeLabel
      )
    }
  }
}
