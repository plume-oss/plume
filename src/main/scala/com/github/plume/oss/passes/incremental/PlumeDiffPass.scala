package com.github.plume.oss.passes.incremental

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.util.HashUtil
import io.shiftleft.codepropertygraph.generated.{NodeTypes, PropertyNames}
import org.slf4j.{Logger, LoggerFactory}

import java.io.File

/** This pass finds all files that have changed in the database when compared to the currently loaded and will remove
  * all related information to the files which have changed from the database.
  */
class PlumeDiffPass(filenames: List[String], driver: IDriver) {

  import PlumeDiffPass._

  private val classHashes: Map[String, String] =
    driver
      .propertyFromNodes(NodeTypes.FILE, PropertyNames.NAME, PropertyNames.HASH, "")
      .flatMap { m =>
        val name = m.getOrElse(PropertyNames.NAME, "")
        val hash = m.getOrElse(PropertyNames.HASH, "")
        if (hash != null) {
          Some(name.toString -> hash.toString)
        } else {
          None
        }
      }
      .toMap

  /** Returns all given filenames as java.io.File objects.
    */
  def partIterator: Iterator[File] = filenames.map(new File(_)).iterator

  /** Find and delete changed source files.
    */
  def createAndApply(): Seq[String] = {
    val changedFiles = partIterator
      .filter { f =>
        classHashes.get(f.getAbsolutePath) match {
          case Some(hash) => HashUtil.getFileHash(f) != hash
          case None       => false // New files
        }
      }
      .map(_.getAbsolutePath)
      .toSeq
    if (changedFiles.nonEmpty) {
      logger.debug(s"Detected changes in the following files: ${changedFiles.mkString(", ")}")
      driver.removeSourceFiles(changedFiles: _*)
    }
    val newFiles = partIterator
      .map(_.getAbsolutePath)
      .filterNot(classHashes.contains)
      .toSeq
    changedFiles ++ newFiles
  }

}

object PlumeDiffPass {
  val logger: Logger = LoggerFactory.getLogger(PlumeDiffPass.getClass)
}