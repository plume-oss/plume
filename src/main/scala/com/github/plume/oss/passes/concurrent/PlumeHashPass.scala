package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.util.HashUtil
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.PropertyNames
import io.shiftleft.codepropertygraph.generated.nodes.File
import io.shiftleft.passes.DiffGraph
import io.shiftleft.semanticcpg.language._
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File => JFile}
import scala.util.{Failure, Success, Try}

/** Performs hash calculations on the files represented by the FILE nodes.
  */
class PlumeHashPass(cpg: Cpg) extends PlumeConcurrentCpgPass[File](cpg) {

  import PlumeHashPass._

  /** We only hash application files who have not been hashed before. Any newly added files will have fresh FILE nodes
    * without a defined HASH property.
    */
  override def generateParts(): Array[File] =
    cpg.file.where(_.typeDecl.isExternal(false)).filter(_.hash.isEmpty).toArray

  /** Use the information in the given file node to find the local file and store its hash locally.
    */
  override def runOnPart(diffGraph: DiffGraph.Builder, part: File): Unit = {
    val localDiff = DiffGraph.newBuilder
    Try(HashUtil.getFileHash(new JFile(part.name))) match {
      case Failure(exception) =>
        logger.warn(s"Unable to generate hash for file at ${part.name}", exception)
      case Success(fileHash) =>
        localDiff.addNodeProperty(part, PropertyNames.HASH, fileHash)
    }
    diffGraph.moveFrom(localDiff)
  }

}

object PlumeHashPass {
  val logger: Logger = LoggerFactory.getLogger(PlumeHashPass.getClass)
}
