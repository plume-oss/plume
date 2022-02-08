package com.github.plume.oss.testfixtures

import com.github.plume.oss.Jimple2Cpg
import com.github.plume.oss.drivers.OverflowDbDriver
import com.github.plume.oss.JavaCompiler.compileJava
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

import java.io.{File, PrintWriter}
import java.nio.file.Files
import scala.util.Using

class PlumeFrontend extends LanguageFrontend {

  val driver: OverflowDbDriver = new OverflowDbDriver(dataFlowCacheFile = None)
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    new Jimple2Cpg().createCpg(sourceCodeFile.getAbsolutePath, driver = driver)
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
