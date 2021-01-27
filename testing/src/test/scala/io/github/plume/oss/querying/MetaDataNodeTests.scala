package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._

class MetaDataNodeTests extends PlumeCodeToCpgSuite {
  override val code =
    """
      |class Foo {}
      |""".stripMargin

  "should contain exactly one node with all mandatory fields set" in {
    cpg.metaData.l match {
      case List(x) =>
        x.language shouldBe "Java"
        // TODO
        // x.version shouldBe "0.1"
        x.overlays shouldBe List("semanticcpg")
      case _ => fail()
    }
  }
}
