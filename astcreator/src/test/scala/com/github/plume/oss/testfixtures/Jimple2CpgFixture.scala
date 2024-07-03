package com.github.plume.oss.testfixtures

import com.github.plume.oss.JimpleAst2Database
import com.github.plume.oss.drivers.OverflowDbDriver
import com.github.plume.oss.JavaCompiler.compileJava
import io.joern.jimple2cpg.testfixtures.JimpleCodeToCpgFixture
import io.joern.jimple2cpg.Config
import io.joern.x2cpg.testfixtures.{Code2CpgFixture, LanguageFrontend, DefaultTestCpg}
import io.shiftleft.codepropertygraph.Cpg
import org.slf4j.LoggerFactory

import java.io.{File, PrintWriter}
import java.nio.file.Path
import scala.util.Using

trait PlumeFrontend(val _driver: Option[OverflowDbDriver]) extends LanguageFrontend {

  private val logger              = LoggerFactory.getLogger(classOf[PlumeFrontend])
  override val fileSuffix: String = ".java"

  val driver: OverflowDbDriver = _driver match {
    case Some(d) => d
    case None    => new OverflowDbDriver()
  }

  override def execute(sourceCodeFile: File): Cpg = {
    new JimpleAst2Database(driver).createAst(Config().withInputPath(sourceCodeFile.getAbsolutePath))
    Cpg(driver.cpg.graph)
  }
}

class PlumeTestCpg(_driver: Option[OverflowDbDriver]) extends DefaultTestCpg with PlumeFrontend(_driver) {

  override protected def codeDirPreProcessing(rootFile: Path, codeFiles: List[Path]): Unit = {
    val sourceFiles = codeFiles.map(_.toFile).filter(_.getName.endsWith(".java"))
    if (sourceFiles.nonEmpty) JimpleCodeToCpgFixture.compileJava(rootFile, sourceFiles)
  }
}

class Jimple2CpgFixture(_driver: Option[OverflowDbDriver] = None) extends Code2CpgFixture(() => PlumeTestCpg(_driver))
