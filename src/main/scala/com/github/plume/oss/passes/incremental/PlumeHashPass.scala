package com.github.plume.oss.passes.incremental

import com.github.plume.oss.drivers.{IDriver, OverflowDbDriver}
import com.github.plume.oss.passes.PlumeConcurrentWriterPass
import com.github.plume.oss.util.HashUtil
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.{NodeTypes, PropertyNames}
import io.shiftleft.codepropertygraph.generated.nodes.File
import io.shiftleft.semanticcpg.language.*
import org.slf4j.{Logger, LoggerFactory}
import overflowdb.BatchedUpdate.DiffGraphBuilder
import overflowdb.{DetachedNodeGeneric, Node}

import java.io.File as JFile
import scala.util.{Failure, Success, Try}

case class StoredFile(id: Long, name: String)

/** Performs hash calculations on the files represented by the FILE nodes. This is
  */
class PlumeHashPass(driver: IDriver) extends PlumeConcurrentWriterPass[StoredFile](driver) {

  import PlumeHashPass._

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
    Try(HashUtil.getFileHash(new JFile(part.name))) match {
      case Failure(exception) =>
        logger.warn(s"Unable to generate hash for file at $part", exception)
      case Success(fileHash) =>
        val node = new Node {
          override def label(): String = NodeTypes.FILE
          override def id(): Long      = part.id
        }
        localDiff.setNodeProperty(node, PropertyNames.HASH, fileHash)
    }
    diffGraph.absorb(localDiff)
  }

}

object PlumeHashPass {
  val logger: Logger = LoggerFactory.getLogger(PlumeHashPass.getClass)
}
