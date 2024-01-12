package com.github.plume.oss.passes.incremental

import better.files.File as BetterFile
import com.github.plume.oss.drivers.{IDriver, OverflowDbDriver}
import com.github.plume.oss.passes.PlumeConcurrentWriterPass
import com.github.plume.oss.util.HashUtil
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.{File, NewFile}
import io.shiftleft.codepropertygraph.generated.{NodeTypes, PropertyNames}
import io.shiftleft.semanticcpg.language.*
import org.slf4j.{Logger, LoggerFactory}
import overflowdb.BatchedUpdate.DiffGraphBuilder
import overflowdb.{DetachedNodeGeneric, Edge, Graph, Node, Property, PropertyKey}

import java.util
import java.util.Optional
import scala.util.{Failure, Success, Try}

case class StoredFile(id: Long, name: String)

/** Performs hash calculations on the files represented by the FILE nodes. This is
  */
class PlumeHashPass(driver: IDriver) extends PlumeConcurrentWriterPass[StoredFile](driver) {

  import PlumeHashPass.*

  /** We only hash application files who have not been hashed before. Any newly added files will have fresh FILE nodes
    * without a defined HASH property.
    */
  override def generateParts(): Array[StoredFile] = {
    driver
      .propertyFromNodes(NodeTypes.FILE, "id", PropertyNames.NAME, PropertyNames.HASH, PropertyNames.IS_EXTERNAL)
      .filterNot(_.getOrElse(PropertyNames.IS_EXTERNAL, true).toString.toBoolean)
      .filter(_.getOrElse(PropertyNames.HASH, "").toString.isBlank)
      .map(f => StoredFile(f("id").toString.toLong, f(PropertyNames.NAME).toString))
      .toArray
  }

  /** Use the information in the given file node to find the local file and store its hash locally.
    */
  override def runOnPart(diffGraph: DiffGraphBuilder, part: StoredFile): Unit = {
    val localDiff = new DiffGraphBuilder
    Try(HashUtil.getFileHash(BetterFile(part.name))) match {
      case Failure(exception) =>
        logger.warn(s"Unable to generate hash for file at $part", exception)
      case Success(fileHash) =>
        val node = AnonymousNode(part.id, NodeTypes.FILE)
        localDiff.setNodeProperty(node, PropertyNames.HASH, fileHash)
    }
    diffGraph.absorb(localDiff)
  }

}

object PlumeHashPass {
  val logger: Logger = LoggerFactory.getLogger(PlumeHashPass.getClass)
}

class AnonymousNode(val id: Long, val label: String) extends Node {
  override def both(edgeLabels: String*): util.Iterator[Node] = null

  override def addEdgeImpl(label: String, inNode: Node, keyValues: Any*): Edge = null

  override def addEdgeImpl(label: String, inNode: Node, keyValues: util.Map[String, AnyRef]): Edge = null

  override def addEdgeSilentImpl(label: String, inNode: Node, keyValues: Any*): Unit = {}

  override def addEdgeSilentImpl(label: String, inNode: Node, keyValues: util.Map[String, AnyRef]): Unit = {}

  override def out(): util.Iterator[Node] = null

  override def out(edgeLabels: String*): util.Iterator[Node] = null

  override def in(): util.Iterator[Node] = null

  override def in(edgeLabels: String*): util.Iterator[Node] = null

  override def both(): util.Iterator[Node] = null

  override def outE(): util.Iterator[Edge] = null

  override def outE(edgeLabels: String*): util.Iterator[Edge] = null

  override def inE(): util.Iterator[Edge] = null

  override def inE(edgeLabels: String*): util.Iterator[Edge] = null

  override def bothE(): util.Iterator[Edge] = null

  override def bothE(edgeLabels: String*): util.Iterator[Edge] = null

  override def graph(): Graph = null

  override def propertyKeys(): util.Set[String] = null

  override def property(key: String): AnyRef = null

  override def property[A](key: PropertyKey[A]): A = null.asInstanceOf[A]

  override def propertyOption[A](key: PropertyKey[A]): Optional[A] = null

  override def propertyOption(key: String): Optional[AnyRef] = null

  override def propertiesMap(): util.Map[String, AnyRef] = null

  override def setPropertyImpl(key: String, value: Any): Unit = {}

  override def setPropertyImpl[A](key: PropertyKey[A], value: A): Unit = {}

  override def setPropertyImpl(property: Property[_]): Unit = {}

  override def removePropertyImpl(key: String): Unit = {}

  override def removeImpl(): Unit = {}
}
