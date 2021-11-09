package io.github.plume.oss.testfixtures

import io.github.plume.oss.Jimple2Cpg
import io.github.plume.oss.drivers.{OverflowDbDriver, TinkerGraphDriver}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

import java.io.{File, PrintWriter}
import java.nio.file.Files
import java.util.Collections
import javax.tools.{JavaCompiler, JavaFileObject, StandardLocation, ToolProvider}
import scala.jdk.CollectionConverters
import scala.util.Using

class PlumeFrontend extends LanguageFrontend {

  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    val driver = new OverflowDbDriver()
    new Jimple2Cpg().createCpg(sourceCodeFile.getAbsolutePath, driver = driver)
    Cpg(driver.cpg.graph)
  }
}

class Jimple2CpgFixture extends CodeToCpgFixture(new PlumeFrontend) {

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

  /** Compiles the source code with debugging info.
    */
  def compileJava(sourceCodeFile: File): Unit = {
    val javac       = getJavaCompiler
    val fileManager = javac.getStandardFileManager(null, null, null)
    javac
      .getTask(
        null,
        fileManager,
        null,
        CollectionConverters.SeqHasAsJava(Seq("-g", "-d", sourceCodeFile.getParent)).asJava,
        null,
        fileManager.getJavaFileObjectsFromFiles(
          CollectionConverters.SeqHasAsJava(Seq(sourceCodeFile)).asJava
        )
      )
      .call()

    fileManager
      .list(
        StandardLocation.CLASS_OUTPUT,
        "",
        Collections.singleton(JavaFileObject.Kind.CLASS),
        false
      )
      .forEach(x => new File(x.toUri).deleteOnExit())
  }

  /** Programmatically obtains the system Java compiler.
    */
  def getJavaCompiler: JavaCompiler = {
    Option(ToolProvider.getSystemJavaCompiler) match {
      case Some(javac) => javac
      case None        => throw new RuntimeException("Unable to find a Java compiler on the system!")
    }
  }

}
