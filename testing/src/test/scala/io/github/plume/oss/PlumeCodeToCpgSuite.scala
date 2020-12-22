package io.github.plume.oss
import java.io.File

import io.github.plume.oss.drivers.{DriverFactory, GraphDatabase, IDriver, OverflowDbDriver}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

import scala.util.Using

class PlumeFrontend extends LanguageFrontend {
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {

    Using(DriverFactory.invoke(GraphDatabase.OVERFLOWDB).asInstanceOf[OverflowDbDriver]) { driver =>

    }

    ???
  }
}

class PlumeCodeToCpgSuite extends CodeToCpgFixture(new PlumeFrontend) {

}
