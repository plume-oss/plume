package io.github.plume.oss
import java.io.{File, FileWriter}
import io.github.plume.oss.drivers.{DriverFactory, GraphDatabase, OverflowDbDriver}
import io.github.plume.oss.graphio.GraphMLWriter
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.cpgloading.{CpgLoader, CpgLoaderConfig}
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

import scala.util.Using

class PlumeFrontend extends LanguageFrontend {
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    val cpgFile = File.createTempFile("plume", ".bin")
    cpgFile.deleteOnExit()
    Using(DriverFactory.invoke(GraphDatabase.OVERFLOWDB).asInstanceOf[OverflowDbDriver]) { driver =>
      driver.storageLocation(cpgFile.getAbsolutePath)
      val extractor = new Extractor(driver)
      extractor.load(sourceCodeFile).project()
      LocalCache.INSTANCE.clear()
    }
    val odbConfig = overflowdb.Config.withDefaults().withStorageLocation(cpgFile.getAbsolutePath)
    val config = CpgLoaderConfig.withDefaults.withOverflowConfig(odbConfig)
    CpgLoader.loadFromOverflowDb(config)
  }
}

class PlumeCodeToCpgSuite extends CodeToCpgFixture(new PlumeFrontend) {

}
