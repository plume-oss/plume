package com.github.plume.oss

import com.github.plume.oss.Jimple2Cpg.getQualifiedClassPath
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.base.AstCreationPass
import com.github.plume.oss.passes.incremental.PlumeDiffPass
import io.joern.jimple2cpg.Config
import io.joern.jimple2cpg.Jimple2Cpg.getQualifiedClassPath
import io.joern.jimple2cpg.util.ProgramHandlingUtil
import io.joern.jimple2cpg.util.ProgramHandlingUtil.extractClassesInPackageLayout
import io.joern.x2cpg.SourceFiles
import io.shiftleft.codepropertygraph.Cpg
import org.slf4j.LoggerFactory
import soot.options.Options
import soot.{G, PhaseOptions, Scene}

import java.io.File as JFile
import java.nio.file.Paths
import scala.language.postfixOps
import scala.util.Try

object JimpleAst2Database {
  val language: String = "PLUME"

  /** Formats the file name the way Soot refers to classes within a class path. e.g.
    * /unrelated/paths/class/path/Foo.class => class.path.Foo
    *
    * @param filename
    *   the file name to transform.
    * @return
    *   the correctly formatted class path.
    */
  def getQualifiedClassPath(filename: String): String = {
    val codePath = ProgramHandlingUtil.getUnpackingDir
    val codeDir: String = if (codePath.toFile.isDirectory) {
      codePath.toAbsolutePath.normalize.toString
    } else {
      Paths.get(codePath.toFile.getParentFile.getAbsolutePath).normalize.toString
    }
    filename
      .replace(codeDir + JFile.separator, "")
      .replace(JFile.separator, ".")
  }
}

class JimpleAst2Database(driver: IDriver, sootOnlyBuild: Boolean = false) extends io.joern.jimple2cpg.Jimple2Cpg {

  private val logger = LoggerFactory.getLogger(getClass)

  def createAst(config: Config): Unit = {
    val rawSourceCodeFile = new JFile(config.inputPath)
    val sourceTarget      = rawSourceCodeFile.toPath.toAbsolutePath.normalize.toString
    val sourceCodeDir = if (rawSourceCodeFile.isDirectory) {
      sourceTarget
    } else {
      Paths
        .get(new JFile(sourceTarget).getParentFile.getAbsolutePath)
        .normalize
        .toString
    }

    configureSoot()

    val sourceFileExtensions  = Set(".class", ".jimple")
    val archiveFileExtensions = Set(".jar", ".war")
    // Load source files and unpack archives if necessary
    val sourceFileNames = if (sourceTarget == sourceCodeDir) {
      // Load all source files in a directory
      loadSourceFiles(sourceCodeDir, sourceFileExtensions, archiveFileExtensions)
    } else {
      // Load single file that was specified
      loadSourceFiles(sourceTarget, sourceFileExtensions, archiveFileExtensions)
    }

    logger.info(s"Loading ${sourceFileNames.size} program files")
    logger.debug(s"Source files are: $sourceFileNames")

    // Load classes into Soot
    val codeToProcess = new PlumeDiffPass(sourceFileNames, driver).createAndApply().toList
    loadClassesIntoSoot(codeToProcess)
    if (!sootOnlyBuild) {

      // Project Soot classes
      val astCreator = new AstCreationPass(sourceFileNames, driver)
      astCreator.createAndApply()
    }
    // Clear classes from Soot
    G.reset()
  }

  override private def createCpg(config: Config): Try[Cpg] = super.createCpg(config)

  /** Load all source files from archive and/or source file types.
    */
  private def loadSourceFiles(
    sourceCodePath: String,
    sourceFileExtensions: Set[String],
    archiveFileExtensions: Set[String]
  ): List[String] = {
    (
      extractSourceFilesFromArchive(sourceCodePath, archiveFileExtensions) ++
        moveClassFiles(SourceFiles.determine(Set(sourceCodePath), sourceFileExtensions))
    ).distinct
  }

  private def loadClassesIntoSoot(sourceFileNames: List[String]): Unit = {
    sourceFileNames
      .map(getQualifiedClassPath)
      .foreach { cp =>
        Scene.v().addBasicClass(cp)
        Scene.v().loadClassAndSupport(cp)
      }
    Scene.v().loadNecessaryClasses()
  }

  private def configureSoot(): Unit = {
    // set application mode
    Options.v().set_app(false)
    Options.v().set_whole_program(false)
    // make sure classpath is configured correctly
    Options.v().set_soot_classpath(ProgramHandlingUtil.getUnpackingDir.toString)
    Options.v().set_prepend_classpath(true)
    // keep debugging info
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
    // ignore library code
    Options.v().set_no_bodies_for_excluded(true)
    Options.v().set_allow_phantom_refs(true)
    // keep variable names
    Options.v.setPhaseOption("jb.sils", "enabled:false")
    PhaseOptions.v().setPhaseOption("jb", "use-original-names:true")
  }

  private def clean(): Unit = {
    G.reset()
    ProgramHandlingUtil.clean()
  }

}
