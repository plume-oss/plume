package com.github.plume.oss.util

import io.joern.x2cpg.SourceFiles
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.{ClassReader, ClassVisitor, Opcodes}
import org.slf4j.LoggerFactory

import java.io.{File, FileInputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import scala.util.{Failure, Success, Try, Using}

/** Responsible for handling JAR unpacking and handling the temporary build directory.
  */
object ProgramHandlingUtil {

  private val logger = LoggerFactory.getLogger(ProgramHandlingUtil.getClass)

  /** The temporary directory used to unpack class files to.
    */
  private var TEMP_DIR: Option[Path] = Some(Files.createTempDirectory("plume"))

  logger.debug(s"Using temporary folder at $TEMP_DIR")

  /** Inspects class files and moves them to the temp directory based on their package path.
    *
    * @param files the class files to move.
    * @return the list of class files at their new locations.
    */
  def moveClassFiles(files: List[String]): List[String] = {
    val dir                      = getUnpackingDir
    var destPath: Option[String] = None

    sealed class ClassPathVisitor extends ClassVisitor(Opcodes.ASM8) {
      override def visit(
          version: Int,
          access: Int,
          name: String,
          signature: String,
          superName: String,
          interfaces: Array[String]
      ): Unit = {
        destPath = Some(dir.toAbsolutePath.toString + File.separator + name + ".class")
      }
    }

    files.flatMap { f =>
      Using.resource(new FileInputStream(f)) { fis =>
        val cr          = new ClassReader(fis)
        val rootVisitor = new ClassPathVisitor()
        cr.accept(rootVisitor, SKIP_CODE)
      }
      destPath match {
        case Some(destPath) =>
          val dstFile = new File(destPath)
          dstFile.mkdirs()
          Files.copy(Paths.get(f), dstFile.toPath, StandardCopyOption.REPLACE_EXISTING)
          Some(dstFile.getAbsolutePath)
        case None => None
      }
    }
  }

  /** Returns the temporary directory used to unpack and analyze projects in.
    * @return the path pointing to the directory.
    */
  def getUnpackingDir: Path = {
    TEMP_DIR match {
      case None =>
        val p = Files.createTempDirectory("plume")
        TEMP_DIR = Some(p)
        p
      case Some(dir) => dir
    }
  }

  /** Unzips a ZIP file into a sequence of files. All files unpacked are deleted at the end of CPG construction.
    *
    * @param zf             The ZIP file to extract.
    * @param sourceCodePath The project root path to unpack to.
    */
  def unzipArchive(zf: ZipFile, sourceCodePath: String): Try[Seq[String]] = scala.util.Try {
    val dir = getUnpackingDir
    Using.resource(zf) { zip: ZipFile =>
      // Copy zipped files across
      zip
        .entries()
        .asScala
        .filter(f => !f.isDirectory && f.getName.contains(".class"))
        .flatMap(entry => {
          val sourceCodePathFile = new File(sourceCodePath)
          // Handle the case if the input source code path is an archive itself
          val destFile = if (sourceCodePathFile.isDirectory) {
            new File(dir.toAbsolutePath.toString + File.separator + entry.getName)
          } else {
            new File(dir.toAbsolutePath.toString + File.separator + entry.getName)
          }
          // dirName accounts for nested directories as a result of JAR package structure
          val dirName = destFile.getAbsolutePath
            .substring(0, destFile.getAbsolutePath.lastIndexOf(File.separator))
          // Create directory path
          new File(dirName).mkdirs()
          try {
            if (destFile.exists()) destFile.delete()
            Using.resource(zip.getInputStream(entry)) { input =>
              Files.copy(input, destFile.toPath)
            }
            Option(destFile.getAbsolutePath)
          } catch {
            case e: Exception =>
              logger.warn(
                s"Encountered an error while extracting entry ${entry.getName} from archive ${zip.getName}.",
                e
              )
              Option.empty
          }
        })
        .toSeq
    }
  }

  /** Retrieve parseable files from archive types.
    */
  def extractSourceFilesFromArchive(
      sourceCodeDir: String,
      archiveFileExtensions: Set[String]
  ): List[String] = {
    val archives = if (new File(sourceCodeDir).isFile) {
      List(sourceCodeDir)
    } else {
      SourceFiles.determine(Set(sourceCodeDir), archiveFileExtensions)
    }
    archives.flatMap { x =>
      unzipArchive(new ZipFile(x), sourceCodeDir) match {
        case Failure(e) =>
          throw new RuntimeException(s"Error extracting files from archive at $x", e)
        case Success(files) => files
      }
    }
  }

  /** Removes all files in the temporary unpacking directory.
    */
  def clean(): Unit = FileUtils.deleteDirectory(getUnpackingDir.toFile)

}
