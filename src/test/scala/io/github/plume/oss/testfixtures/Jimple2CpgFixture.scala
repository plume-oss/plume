package io.github.plume.oss.testfixtures

import io.github.plume.oss.JavaCompiler.compileJava
import io.github.plume.oss.Jimple2Cpg
import io.github.plume.oss.drivers.OverflowDbDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

import java.io.{File, PrintWriter}
import java.nio.file.Files
import scala.util.Using

class PlumeFrontend extends LanguageFrontend {

  val driver: OverflowDbDriver = new OverflowDbDriver()
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    val cpg    = new Jimple2Cpg().createCpg(sourceCodeFile.getAbsolutePath, driver = driver)
    cpg
  }
}

class Jimple2CpgFixture extends CodeToCpgFixture(new PlumeFrontend) {

  val driver: OverflowDbDriver = frontend.asInstanceOf[PlumeFrontend].driver

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
