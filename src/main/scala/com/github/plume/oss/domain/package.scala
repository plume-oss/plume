package com.github.plume.oss

import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import io.joern.dataflowengineoss.queryengine.{PathElement, ReachableByResult, ResultTable}
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{Call, CfgNode, StoredNode}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.ByteArrayOutputStream
import org.slf4j.LoggerFactory
import overflowdb.traversal.jIteratortoTraversal

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.{GZIPInputStream, GZIPOutputStream}
import scala.io.Source
import scala.jdk.CollectionConverters
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala
import scala.util.Using

/** Contains case classes that can be used independently.
  */
package object domain {

  private val logger = LoggerFactory.getLogger("com.github.plume.oss.domain")
  private val objectMapper = JsonMapper
    .builder()
    .addModule(DefaultScalaModule)
    .build() :: ClassTagExtensions

  /** Serializes a given object as JSON and then returns the GZIP compressed base 64 encoded string.
    * @param o object to serialize.
    * @return the GZIP compressed base 64 encoded string.
    */
  private def compress(o: SerializableTable): String = {
    val str = objectMapper.writeValueAsString(o)
    if (str == null || str.isEmpty) return str
    val out = new ByteArrayOutputStream()
    Using.resource(new GZIPOutputStream(out)) { gzip =>
      gzip.write(str.getBytes)
    }
    Base64.encodeBase64String(out.toByteArray)
  }

  /** Given an object and a path, will serialize and compress the object to the given path
    * using [[compress]].
    * @param o object to serialize.
    * @param p path to write serialized data to.
    */
  def compressToFile(o: ConcurrentHashMap[Long, Vector[SerialReachableByResult]], p: Path): Unit = {
    logger.info(s"Serializing ${o.asScala.flatMap(_._2).size} data flow paths")
    val deflatedStr = compress(SerializableTable(o))
    Files.write(p, deflatedStr.getBytes(StandardCharsets.UTF_8))
  }

  /** Deserializes a given object from GZIP compressed base 64 encoded to JSON string.
    * @param deflatedTxt object to deserialize.
    * @return the GZIP compressed base 64 encoded string.
    */
  private def decompress(deflatedTxt: String): String = {
    val bytes = Base64.decodeBase64(deflatedTxt)
    Using.resource(new GZIPInputStream(new ByteArrayInputStream(bytes))) { zis =>
      IOUtils.toString(zis, "UTF-8")
    }
  }

  /** Given a path, will deserialize and decompress the file at the given path
    * using [[decompress]].
    * @param p path to read deserialized data from.
    * @return the deserialized object.
    */
  def decompressFile(p: Path): ConcurrentHashMap[Long, Vector[SerialReachableByResult]] = {
    Using.resource(Source.fromFile(p.toFile)) { deflatedStr =>
      val o = objectMapper
        .readValue(
          decompress(deflatedStr.mkString),
          new TypeReference[SerializableTable] {}
        )
        .table
      logger.info(s"Deserialized ${o.asScala.flatMap(_._2).size} data flow paths")
      o
    }
  }

  /** Converts serialized path results to deserialized ReachableByResults. This is assumed to be called before any nodes
    * are removed from the graph since these results were serialized.
    *
    * @param serTab serialized raw results.
    * @return deserialized ReachableByResults table.
    */
  def deserializeResultTable(
      serTab: Option[ConcurrentHashMap[Long, Vector[SerialReachableByResult]]],
      cpg: Cpg
  ): Option[ResultTable] = {
    serTab match {
      case Some(tab) =>
        val resultTable = new ResultTable()

        try {
          CollectionConverters
            .MapHasAsScala(tab)
            .asScala
            .flatMap { case (id, vec) =>
              cpg.graph.nodes(id).nextOption match {
                case Some(node) if node != null =>
                  Some(
                    (
                      node,
                      vec.map { f: SerialReachableByResult =>
                        SerialReachableByResult.unapply(f, cpg, resultTable)
                      }
                    )
                  )
                case _ =>
                  logger.warn(s"Lost node $id holding ${vec.size}. This indicates corrupted storage.")
                  None
              }
            }
            .foreach { case (k, v) => resultTable.table.put(k.asInstanceOf[StoredNode], v) }
          Some(resultTable)
        } catch {
          case e: Exception =>
            logger.error("Unable to deserialize results table.", e)
            None
        }
      case None => None
    }
  }

  final case class SerializableTable(
      @JsonDeserialize(keyAs = classOf[Long]) table: ConcurrentHashMap[Long, Vector[
        SerialReachableByResult
      ]]
  )

  /** A serializable version of ReachableByResult.
    * @param path a path of nodes represented by [[SerialReachableByResult]]s.
    * @param callSite the call site that was expanded to kick off the task. We require this to match call sites to
    *                 exclude non-realizable paths through other callers
    * @param callDepth the call depth of this result.
    * @param partial indicate whether this result stands on its own or requires further analysis, e.g., by expanding
    *                output arguments backwards into method output parameters.
    */
  final case class SerialReachableByResult(
      path: Vector[SerialPathElement],
      @JsonDeserialize(contentAs = classOf[Long]) callSite: Option[Long],
      callDepth: Int = 0,
      partial: Boolean = false
  )

  /** A serializable version of ReachableByResult.
    */
  object SerialReachableByResult {

    private val logger = LoggerFactory.getLogger(classOf[SerialReachableByResult])

    /** Creates a serializable version of ReachableByResult.
      * @param rbr the ReachableByResult class.
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
