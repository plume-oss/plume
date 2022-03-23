package com.github.plume.oss

import com.github.plume.oss.drivers.{IDriver, OverflowDbDriver}
import com.github.plume.oss.passes._
import com.github.plume.oss.passes.base._
import com.github.plume.oss.passes.controlflow.PlumeCfgCreationPass
import com.github.plume.oss.passes.controlflow.cfgdominator.PlumeCfgDominatorPass
import com.github.plume.oss.passes.controlflow.codepencegraph.PlumeCdgPass
import com.github.plume.oss.passes.incremental.{PlumeDiffPass, PlumeHashPass}
import com.github.plume.oss.passes.reachingdef._
import io.joern.jimple2cpg.util.ProgramHandlingUtil
import io.joern.jimple2cpg.util.ProgramHandlingUtil.{extractSourceFilesFromArchive, moveClassFiles}
import io.joern.x2cpg.SourceFiles
import io.joern.x2cpg.X2Cpg.newEmptyCpg
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.{NodeTypes, PropertyNames}
import io.shiftleft.passes.CpgPassBase
import org.slf4j.LoggerFactory
import soot.options.Options
import soot.{G, PhaseOptions, Scene, SootClass}

import java.io.{File => JFile}
import java.nio.file.Paths
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object Jimple2Cpg {
  val language: String = "PLUME"

  /** Formats the file name the way Soot refers to classes within a class path. e.g.
    * /unrelated/paths/class/path/Foo.class => class.path.Foo
    *
    * @param filename the file name to transform.
    * @return the correctly formatted class path.
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
      .replace(".class", "")
      .replace(JFile.separator, ".")
  }
}

/** The main entrypoint for converting JVM bytecode/Jimple to a code property graph representation.
  */
class Jimple2Cpg {

  import Jimple2Cpg.{getQualifiedClassPath, language}

  private val logger = LoggerFactory.getLogger(classOf[Jimple2Cpg])

  /** Creates a CPG from Jimple.
    *
    * @param rawSourceCodePath  The path to the Jimple code or code that can be transformed into Jimple.
    * @param referenceGraphOutputPath         The path to store the CPG. If `outputPath` is `None`, the CPG is created in-memory.
    * @param driver             The driver used to interact with the backend database.
    * @param sootOnlyBuild      (Experimental) Used to determine how many resources are used when only loading files
    *                           into Soot.
    * @throws Exception if any fatal error occurred during the CPG creation.
    * @return The constructed CPG.
    */
  def createCpg(
      rawSourceCodePath: String,
      referenceGraphOutputPath: Option[String] = None,
      driver: IDriver = new OverflowDbDriver(),
      sootOnlyBuild: Boolean = false
  ): Cpg = PlumeStatistics.time(
    PlumeStatistics.TIME_EXTRACTION, {
      try {
        // Determine if the given path is a file or directory and sanitize accordingly
        val rawSourceCodeFile = new JFile(rawSourceCodePath)
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
        val cpg = newEmptyCpg(referenceGraphOutputPath)

        val metaDataKeyPool = new IncrementalKeyPool(1, 100, driver.idInterval(1, 100))
        val typesKeyPool    = new IncrementalKeyPool(101, 2_000_100, driver.idInterval(101, 2_000_100))
        val methodKeyPool =
          new IncrementalKeyPool(
            30_001_001,
            Long.MaxValue,
            driver.idInterval(30_001_001, Long.MaxValue)
          )

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
        val codeToProcess  = new PlumeDiffPass(sourceFileNames, driver).createAndApply()
        val allSootClasses = loadClassesIntoSoot(codeToProcess)
        if (sootOnlyBuild) return cpg

        // Record statistics for experimental purposes
        if (codeToProcess.isEmpty) {
          logger.info("No files have changed since last update. Exiting...")
          return cpg
        } else {
          val numChangedMethods = allSootClasses.map(_.getMethodCount).sum
          logger.info(
            s"Processing ${codeToProcess.size} new or changed program files " +
              s"($numChangedMethods new or changed methods)"
          )
          PlumeStatistics.setMetric(PlumeStatistics.PROGRAM_CLASSES, allSootClasses.size)
          PlumeStatistics.setMetric(PlumeStatistics.PROGRAM_METHODS, numChangedMethods)
        }

        // After the diff pass any changed types are removed. Remaining types should be black listed to avoid duplicates
        val unchangedTypes = driver
          .propertyFromNodes(NodeTypes.TYPE_DECL, PropertyNames.FULL_NAME)
          .flatMap(_.get(PropertyNames.FULL_NAME))
          .map(_.toString)
          .toSet[String]
        val unchangedNamespaces = driver
          .propertyFromNodes(NodeTypes.NAMESPACE_BLOCK, PropertyNames.NAME)
          .flatMap(_.get(PropertyNames.NAME))
          .map(_.toString)
          .toSet[String]

        new PlumeMetaDataPass(cpg, language, Some(metaDataKeyPool), unchangedTypes)
          .createAndApply(driver)

        // Project Soot classes
        val astCreator = new AstCreationPass(codeToProcess.toList, cpg, methodKeyPool)
        astCreator.createAndApply(driver)
        // Clear classes from Soot
        G.reset()
        new PlumeTypeNodePass(
          astCreator.global.usedTypes.keys().asScala.toList,
          cpg,
          Some(typesKeyPool),
          unchangedTypes
        ).createAndApply(driver)

        basePasses(cpg, driver, unchangedTypes, unchangedNamespaces).foreach(
          _.createAndApply(driver)
        )
        controlFlowPasses(cpg).foreach(_.createAndApply(driver))
        new PlumeReachingDefPass(cpg, unchangedTypes = unchangedTypes).createAndApply(driver)
        new PlumeHashPass(cpg).createAndApply(driver)
        driver match {
          case x: OverflowDbDriver => x.removeExpiredPathsFromCache(unchangedTypes)
          case _                   =>
        }

        driver.buildInterproceduralEdges()
        cpg
      } finally {
        clean()
      }
    }
  )

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

  private def loadClassesIntoSoot(sourceFileNames: Seq[String]): Seq[SootClass] = {
    val sootClasses = sourceFileNames
      .map(getQualifiedClassPath)
      .map { cp =>
        Scene.v().addBasicClass(cp)
        Scene.v().loadClassAndSupport(cp)
      }
    Scene.v().loadNecessaryClasses()
    sootClasses
  }

  private def basePasses(
      cpg: Cpg,
      driver: IDriver,
      blackList: Set[String],
      nBlacklist: Set[String]
  ): Seq[PlumeCpgPassBase] = {
    val namespaceKeyPool =
      new IncrementalKeyPool(1_000_101, 2_000_200, driver.idInterval(1_000_101, 2_000_200))
    val filesKeyPool =
      new IncrementalKeyPool(2_000_201, 3_000_200, driver.idInterval(2_000_201, 3_000_200))
    val typeDeclKeyPool =
      new IncrementalKeyPool(3_000_201, 4_000_200, driver.idInterval(3_000_201, 4_000_200))
    val methodStubKeyPool =
      new IncrementalKeyPool(4_000_101, 10_001_000, driver.idInterval(4_000_101, 10_001_000))
    val methodDecoratorKeyPool =
      new IncrementalKeyPool(10_001_001, 30_001_000, driver.idInterval(10_001_001, 30_001_000))
    Seq(
      new PlumeFileCreationPass(cpg, Some(filesKeyPool)),
      new PlumeNamespaceCreator(cpg, Some(namespaceKeyPool), nBlacklist),
      new PlumeTypeDeclStubCreator(cpg, Some(typeDeclKeyPool), blackList),
      new PlumeMethodStubCreator(cpg, Some(methodStubKeyPool), blackList),
      new PlumeMethodDecoratorPass(cpg, Some(methodDecoratorKeyPool), blackList),
      new PlumeContainsEdgePass(cpg)
    ).collect { case pass: Any with PlumeCpgPassBase => pass }
  }

  private def controlFlowPasses(cpg: Cpg): Seq[CpgPassBase with PlumeCpgPassBase] = Seq(
    new PlumeCfgCreationPass(cpg),
    new PlumeCfgDominatorPass(cpg),
    new PlumeCdgPass(cpg)
  ).collect { case pass: CpgPassBase with PlumeCpgPassBase => pass }

  private def configureSoot(): Unit = {
    logger.info("Configuring Soot")
    // set application mode
    Options.v().set_app(false)
    Options.v().set_whole_program(false)
    // make sure classpath is configured correctly
    Options.v().set_soot_classpath(ProgramHandlingUtil.getUnpackingDir.toAbsolutePath.toString)
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
