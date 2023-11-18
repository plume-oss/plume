package com.github.plume.oss.passes.incremental

import better.files.File
import com.github.plume.oss.PlumeStatistics
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.util.HashUtil
import io.joern.jimple2cpg.util.ProgramHandlingUtil.ClassFile
import io.shiftleft.codepropertygraph.generated.{NodeTypes, PropertyNames}
import org.slf4j.{Logger, LoggerFactory}

import java.io.File as JFile

/** This pass finds all files that have changed in the database when compared to the currently loaded and will remove
  * all related information to the files which have changed from the database.
  */
class PlumeDiffPass(pathRoot: String, classFiles: List[ClassFile], driver: IDriver) {

  import PlumeDiffPass.*

  private val classHashes: Map[String, String] =
    driver
      .propertyFromNodes(NodeTypes.FILE, PropertyNames.NAME, PropertyNames.HASH, "")
      .flatMap { m =>
        val name = m.getOrElse(PropertyNames.NAME, "")
        val hash = m.getOrElse(PropertyNames.HASH, "")
        if (hash != null) {
          Some(stripTempDirPrefix(name.toString) -> hash.toString)
        } else {
          None
        }
      }
      .toMap

  /** Removes the temporary directory prefix which differs between extractions.
    * @param filePath
    *   the path at which the file is located.
    * @return
    *   the name of the file from a project root level.
    */
  private def stripTempDirPrefix(filePath: String): String =
    filePath.replaceAll(s"$pathRoot${JFile.separator}", "")

  /** Find and delete changed source files.
    */
  def createAndApply(): Seq[ClassFile] = {
    val changedFiles = classFiles
      .filter { f =>
        classHashes.get(stripTempDirPrefix(f.file.pathAsString)) match {
          case Some(hash) => HashUtil.getFileHash(f.file) != hash
          case None       => false // New files
        }
      }

    if (changedFiles.nonEmpty) {
      logger.debug(s"Detected changes in the following files: ${changedFiles.mkString(", ")}")
      PlumeStatistics.time(
        PlumeStatistics.TIME_REMOVING_OUTDATED_GRAPH, {
          driver.removeSourceFiles(changedFiles.map(_.file.pathAsString): _*)
        }
      )
    }
    val newFiles = classFiles
      .filterNot { f => classHashes.contains(stripTempDirPrefix(f.file.pathAsString)) }

    changedFiles ++ newFiles
  }

}

object PlumeDiffPass {
  val logger: Logger = LoggerFactory.getLogger(PlumeDiffPass.getClass)
}
