package io.github.plume.oss

import io.github.plume.oss.drivers.{IDriver, OverflowDbDriver}
import io.github.plume.oss.passes.{AstCreationPass, IncrementalKeyPool, PlumeCfgCreationPass, PlumeFileCreationPass, PlumeMetaDataPass, PlumeNamespaceCreator}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.IntervalKeyPool
import io.shiftleft.semanticcpg.passes.CfgCreationPass
import io.shiftleft.semanticcpg.passes.cfgdominator.CfgDominatorPass
import io.shiftleft.semanticcpg.passes.codepencegraph.CdgPass
import io.shiftleft.semanticcpg.passes.containsedges.ContainsEdgePass
import io.shiftleft.semanticcpg.passes.languagespecific.fuzzyc.MethodStubCreator
import io.shiftleft.semanticcpg.passes.linking.calllinker.StaticCallLinker
import io.shiftleft.semanticcpg.passes.linking.linker.Linker
import io.shiftleft.semanticcpg.passes.metadata.MetaDataPass
import io.shiftleft.semanticcpg.passes.methoddecorations.MethodDecoratorPass
import io.shiftleft.semanticcpg.passes.namespacecreator.NamespaceCreator
import io.shiftleft.semanticcpg.passes.typenodes.{TypeDeclStubCreator, TypeNodePass}
import io.shiftleft.x2cpg.SourceFiles
import io.shiftleft.x2cpg.X2Cpg.newEmptyCpg
import org.slf4j.LoggerFactory
import soot.{G, PhaseOptions, Scene}
import soot.options.Options

import java.nio.file.Files
import java.util.zip.ZipFile
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import scala.util.{Failure, Success, Using}
import java.io.{File => JFile}

object Jimple2Cpg {
  val language = "PLUME"
}

class Jimple2Cpg {

  import Jimple2Cpg._

  private val logger = LoggerFactory.getLogger(classOf[Jimple2Cpg])

  /** Creates a CPG from Jimple.
    *
    * @param sourceCodePath The path to the Jimple code or code that can be transformed into Jimple.
    * @param outputPath     The path to store the CPG. If `outputPath` is `None`, the CPG is created in-memory.
    * @return The constructed CPG.
    */
  def createCpg(
      sourceCodePath: String,
      outputPath: Option[String] = Option(JFile.createTempFile("plume-", ".odb").getAbsolutePath),
      driver: IDriver = new OverflowDbDriver()
  ): Cpg = {
    configureSoot(sourceCodePath)
    val cpg = newEmptyCpg(outputPath)

    Using(driver) { d =>
      if (!d.isConnected) d.connect()

      val metaDataKeyPool = new IncrementalKeyPool(1, 100, driver.getVertexIds(1, 100))
      val typesKeyPool = new IncrementalKeyPool(101, 1000100, driver.getVertexIds(101, 1000100))
      val namespaceKeyPool =
        new IncrementalKeyPool(1000101, 2000200, driver.getVertexIds(1000101, 2000200))
      val filesKeyPool =
        new IncrementalKeyPool(2000201, 3000200, driver.getVertexIds(1000101, 3000200))
      val methodKeyPool =
        new IncrementalKeyPool(1000101, Long.MaxValue, driver.getVertexIds(1000101, Long.MaxValue))

      new PlumeMetaDataPass(cpg, language, Some(metaDataKeyPool)).createAndApply(driver)

      val sourceFileExtensions = Set(".class", ".jimple")
      val archiveFileExtensions = Set(".jar", ".war")
      // Unpack any archives on the path onto the source code path as project root
      val archives = SourceFiles.determine(Set(sourceCodePath), archiveFileExtensions)
      // Load source files and unpack archives if necessary
      val sourceFileNames = (archives
        .map(new ZipFile(_))
        .flatMap(x => {
          unzipArchive(x, sourceCodePath) match {
            case Failure(e) =>
              logger.error(s"Error extracting files from archive at ${x.getName}", e); null
            case Success(value) => value
          }
        })
        .map(_.getAbsolutePath) ++ SourceFiles.determine(
        Set(sourceCodePath),
        sourceFileExtensions
      )).distinct
      val astCreator = new AstCreationPass(sourceCodePath, sourceFileNames, cpg, methodKeyPool)
      astCreator.createAndApply(d)

      new PlumeCfgCreationPass(cpg).createAndApply(driver)

      new PlumeNamespaceCreator(cpg, Some(namespaceKeyPool)).createAndApply()
      new PlumeFileCreationPass(cpg, Some(filesKeyPool)).createAndApply(driver)

      new TypeNodePass(astCreator.global.usedTypes.keys().asScala.toList, cpg, Some(typesKeyPool))
        .createAndApply()
      new TypeDeclStubCreator(cpg).createAndApply()
      new MethodStubCreator(cpg).createAndApply()
      new MethodDecoratorPass(cpg).createAndApply()

      new ContainsEdgePass(cpg).createAndApply()
      new CfgDominatorPass(cpg).createAndApply()
      new CdgPass(cpg).createAndApply()

      new Linker(cpg).createAndApply()
      new StaticCallLinker(cpg).createAndApply()
    }

    closeSoot()

    cpg
  }

  private def configureSoot(sourceCodePath: String): Unit = {
    // set application mode
    Options.v().set_app(false)
    Options.v().set_whole_program(false)
    // make sure classpath is configured correctly
    Options.v().set_soot_classpath(sourceCodePath)
    Options.v().set_prepend_classpath(true)
    // keep debugging info
    Options.v().set_keep_line_number(true)
    Options.v().set_keep_offset(true)
    // ignore library code
    Options.v().set_no_bodies_for_excluded(true)
    Options.v().set_allow_phantom_refs(true)
    // keep variable names
    PhaseOptions.v().setPhaseOption("jb", "use-original-names:true")
    Scene.v().loadBasicClasses()
    Scene.v().loadDynamicClasses()
  }

  private def closeSoot(): Unit = {
    G.reset()
  }

  /** Unzips a ZIP file into a sequence of files. All files unpacked are deleted at the end of CPG construction.
    *
    * @param zf             The ZIP file to extract.
    * @param sourceCodePath The project root path to unpack to.
    */
  private def unzipArchive(zf: ZipFile, sourceCodePath: String) = scala.util.Try {
    Using.resource(zf) { zip: ZipFile =>
      // Copy zipped files across
      zip
        .entries()
        .asScala
        .filter(!_.isDirectory)
        .filter(_.getName.contains(".class"))
        .flatMap(entry => {
          val sourceCodePathFile = new JFile(sourceCodePath)
          // Handle the case if the input source code path is an archive itself
          val destFile = if (sourceCodePathFile.isDirectory) {
            new JFile(sourceCodePath + JFile.separator + entry.getName)
          } else {
            new JFile(
              sourceCodePathFile.getParentFile.getAbsolutePath + JFile.separator + entry.getName
            )
          }
          // dirName accounts for nested directories as a result of JAR package structure
          val dirName = destFile.getAbsolutePath
            .substring(0, destFile.getAbsolutePath.lastIndexOf(JFile.separator))
          // Create directory path
          new JFile(dirName).mkdirs()
          try {
            if (destFile.exists()) destFile.delete()
            Using.resource(zip.getInputStream(entry)) { input =>
              Files.copy(input, destFile.toPath)
            }
            destFile.deleteOnExit()
            Option(destFile)
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

}
