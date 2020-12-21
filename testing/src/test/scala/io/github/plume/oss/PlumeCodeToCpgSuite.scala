package io.github.plume.oss
import java.io.File

import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.semanticcpg.testfixtures.{CodeToCpgFixture, LanguageFrontend}

class PlumeFrontend extends LanguageFrontend {
  override val fileSuffix: String = ".java"

  override def execute(sourceCodeFile: File): Cpg = {
    ???
  }
}

class PlumeCodeToCpgSuite extends CodeToCpgFixture(new PlumeFrontend) {

}
