package com.github.plume.oss

import better.files.File
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.base.AstCreationPass
import com.github.plume.oss.passes.incremental.{PlumeDiffPass, PlumeHashPass}
import io.joern.jimple2cpg.Jimple2Cpg.language
import io.joern.jimple2cpg.passes.SootAstCreationPass
import io.joern.jimple2cpg.{Config, Jimple2Cpg}
import io.joern.jimple2cpg.util.ProgramHandlingUtil
import io.joern.jimple2cpg.util.ProgramHandlingUtil.{ClassFile, extractClassesInPackageLayout}
import io.joern.x2cpg.{SourceFiles, X2CpgFrontend}
import io.joern.x2cpg.X2Cpg.withNewEmptyCpg
import io.joern.x2cpg.datastructures.Global
import io.joern.x2cpg.passes.frontend.{MetaDataPass, TypeNodePass}
import io.shiftleft.codepropertygraph.Cpg
import org.slf4j.LoggerFactory
import soot.options.Options
import soot.{G, PhaseOptions, Scene}

import java.io.File as JFile
import java.nio.file.Paths
import scala.jdk.CollectionConverters.{EnumerationHasAsScala, SeqHasAsJava}
import scala.language.postfixOps
import scala.util.Try

object JimpleAst2Database {
  val language: String = "PLUME"
}

class JimpleAst2Database(driver: IDriver, sootOnlyBuild: Boolean = false) {

  import Jimple2Cpg.*

  private val logger = LoggerFactory.getLogger(classOf[Jimple2Cpg])

  /** Load all class files from archives or directories recursively
    * @return
    *   The list of extracted class files whose package path could be extracted, placed on that package path relative to
    *   [[tmpDir]]
    */
  private def loadClassFiles(src: File, tmpDir: File): List[ClassFile] = {
    val archiveFileExtensions = Set(".jar", ".war", ".zip")
    extractClassesInPackageLayout(
      src,
      tmpDir,
      isClass = e => e.extension.contains(".class"),
      isArchive = e => e.extension.exists(archiveFileExtensions.contains),
      false,
      0
    )
  }

  /** Extract all class files found, place them in their package layout and load them into soot.
    */
  private def sootLoad(classFiles: List[ClassFile]): List[ClassFile] = {
    val fullyQualifiedClassNames = classFiles.flatMap(_.fullyQualifiedClassName)
    logger.info(s"Loading ${classFiles.size} program files")
    logger.debug(s"Source files are: ${classFiles.map(_.file.canonicalPath)}")
    fullyQualifiedClassNames.foreach { fqcn =>
      Scene.v().addBasicClass(fqcn)
      Scene.v().loadClassAndSupport(fqcn)
    }
    classFiles
  }

  /** Apply the soot passes
    * @param tmpDir
    *   A temporary directory that will be used as the classpath for extracted class files
    */
  private def cpgApplyPasses(config: Config, tmpDir: File): Unit = {
    val input = File(config.inputPath)
    configureSoot(config, tmpDir)

    val sourceFileNames = loadClassFiles(input, tmpDir)
    logger.info("Loading classes to soot")

    // Load classes into Soot
    val codeToProcess = new PlumeDiffPass(tmpDir.pathAsString, sourceFileNames, driver).createAndApply().toList
    sootLoad(codeToProcess)
    Scene.v().loadNecessaryClasses()
    logger.info(s"Loaded ${Scene.v().getApplicationClasses.size()} classes")

    if (!sootOnlyBuild) {
      // Project Soot classes
      val astCreator = new AstCreationPass(codeToProcess.map(_.file.pathAsString), driver, tmpDir)
      astCreator.createAndApply()
      new PlumeHashPass(driver).createAndApply()
    }
  }

  def createAst(config: Config): Unit = {
    try {
      File.temporaryDirectory("jimple2cpg-").apply(cpgApplyPasses(config, _))
    } finally {
      G.reset()
    }
  }

  private def configureSoot(config: Config, outDir: File): Unit = {
    // set application mode
    Options.v().set_app(false)
    Options.v().set_whole_program(false)
    // keep debugging info
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
    // ignore library code
    Options.v().set_no_bodies_for_excluded(true)
    Options.v().set_allow_phantom_refs(true)
    // keep variable names
    Options.v().setPhaseOption("jb.sils", "enabled:false")
    Options.v().setPhaseOption("jb", "use-original-names:true")
    // Keep exceptions
    Options.v().set_show_exception_dests(true)
    Options.v().set_omit_excepting_unit_edges(false)
    // output jimple
    Options.v().set_output_format(Options.output_format_jimple)
    Options.v().set_output_dir(outDir.canonicalPath)

    Options.v().set_dynamic_dir(config.dynamicDirs.asJava)
    Options.v().set_dynamic_package(config.dynamicPkgs.asJava)

    Options.v().set_soot_classpath(outDir.canonicalPath)
    Options.v().set_prepend_classpath(true)

    if (config.fullResolver) {
      // full transitive resolution of all references
      Options.v().set_full_resolver(true)
    }
  }
}
