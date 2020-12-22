package io.github.plume.oss
import java.io.File
import io.github.plume.oss.drivers.{DriverFactory, GraphDatabase, OverflowDbDriver}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.cpgloading.CpgLoader
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

import scala.util.Using

class PlumeFrontend extends LanguageFrontend {
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    val cpgFile = File.createTempFile("plume", ".bin")
    cpgFile.deleteOnExit()
    Using(DriverFactory.invoke(GraphDatabase.OVERFLOWDB).asInstanceOf[OverflowDbDriver]) { driver =>
      driver.setStorageLocation(cpgFile.getAbsolutePath)
      val extractor = new Extractor(driver)
      extractor.load(sourceCodeFile)
      extractor.project()
      driver.close()
    }
    CpgLoader.load(cpgFile.getAbsolutePath)
  }
}

class PlumeCodeToCpgSuite extends CodeToCpgFixture(new PlumeFrontend) {

}
