package com.github.plume.oss.testfixtures

import com.github.plume.oss.{Jimple2Cpg, PlumeStatistics}
import com.github.plume.oss.drivers.OverflowDbDriver
import com.github.plume.oss.JavaCompiler.compileJava
import io.joern.x2cpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}
import io.shiftleft.codepropertygraph.Cpg
import org.slf4j.LoggerFactory

import java.io.{File, PrintWriter}
import java.nio.file.Files
import scala.util.Using

class PlumeFrontend extends LanguageFrontend {

  private val logger              = LoggerFactory.getLogger(classOf[PlumeFrontend])
  val driver: OverflowDbDriver    = new OverflowDbDriver(dataFlowCacheFile = None)
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    PlumeStatistics.reset()
    new Jimple2Cpg().createCpg(sourceCodeFile.getAbsolutePath, driver = driver)
    logger.info(s"Plume statistics from last test: ${PlumeStatistics.results()}")
    Cpg(driver.cpg.graph)
  }
}

class Jimple2CpgFixture extends CodeToCpgFixture(new PlumeFrontend) {

  val driver: OverflowDbDriver = frontend.asInstanceOf[PlumeFrontend].driver

  override def createEnhancements(cpg: Cpg): Unit = {}

  override def writeCodeToFile(sourceCode: String): File = {
    val tmpDir = Files.createTempDirectory("semanticcpgtest").toFile
    tmpDir.deleteOnExit()
    val codeFile = File.createTempFile("Test", frontend.fileSuffix, tmpDir)
    Using.resource(new PrintWriter(codeFile)) { pw => pw.write(sourceCode) }
    try {
      compileJava(codeFile)
    } finally {
      codeFile.delete()
    }
    tmpDir
  }

}
