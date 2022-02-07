package com.github.plume.oss

import io.joern.dataflowengineoss.queryengine.{PathElement, ReachableByResult, ResultTable}
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{Call, CfgNode, StoredNode}
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters

/** Contains case classes that can be used independently.
  */
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
          if (!cpg.graph.nodes(id).hasNext)
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

  /** A serializable version of ReachableByResult.
    * @param path a path of nodes represented by [[SerialReachableByResult]]s.
    * @param table a pointer to the global serializable result table.
    * @param callSite the call site that was expanded to kick off the task. We require this to match call sites to
    *                 exclude non-realizable paths through other callers
    * @param callDepth the call depth of this result.
    * @param partial indicate whether this result stands on its own or requires further analysis, e.g., by expanding
    *                output arguments backwards into method output parameters.
    */
  final case class SerialReachableByResult(
      path: Vector[SerialPathElement],
      callSite: Option[Long],
      callDepth: Int = 0,
      partial: Boolean = false
  )

  /** A serializable version of ReachableByResult.
    */
  object SerialReachableByResult {

    private val logger = LoggerFactory.getLogger(classOf[SerialReachableByResult])

    /** Creates a serializable version of ReachableByResult.
      * @param rbr the ReachableByResult class.
      * @param table a pointer to the global serializable result table.
      * @return a serializable ReachableByResult.
      */
    def apply(
        rbr: ReachableByResult
    ): SerialReachableByResult = {
      new SerialReachableByResult(
        rbr.path.map(SerialPathElement.apply),
        rbr.callSite match {
          case Some(call) => Some(call.id())
          case None       => None
        },
        rbr.callDepth,
        rbr.partial
      )
    }

    /** Deserializes a given of [[SerialReachableByResult]].
      * @param srb the serial ReachableByResult class.
      * @param cpg the code property graph pointer.
      * @param table a pointer to the global serializable result table.
      * @return a deserialized ReachableByResult.
      */
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

  /** A serializable version of the SerialPathElement.
    * @param nodeId the ID of the node this path element represents.
    * @param visible whether this path element should be shown in the flow.
    * @param resolved whether we have resolved the method call this argument belongs to.
    * @param outEdgeLabel label of the outgoing DDG edge.
    */
  final case class SerialPathElement(
      nodeId: Long,
      visible: Boolean = true,
      resolved: Boolean = true,
      outEdgeLabel: String = ""
  )

  /** A serializable version of the SerialPathElement.
    */
  object SerialPathElement {

    /** Creates a [[SerialPathElement]] from a given PathElement.
      * @param pe the PathElement to serialize.
      * @return a serializable version of PathElement.
      */
    def apply(pe: PathElement): SerialPathElement = {
      new SerialPathElement(
        pe.node.id(),
        pe.visible,
        pe.resolved,
        pe.outEdgeLabel
      )
    }

    /** Deserializes the given [[SerialPathElement]].
      * @param spe the serializable version of the representative PathElement.
      * @param cpg the code property graph pointer.
      * @return the deserialized PathElement.
      */
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
