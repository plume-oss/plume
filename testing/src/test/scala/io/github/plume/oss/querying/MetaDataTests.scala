package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.github.plume.oss.util.ExtractorConst
import io.shiftleft.semanticcpg.language._

class MetaDataTests extends PlumeCodeToCpgSuite {
  override val code =
    """
      |class Foo {}
      |""".stripMargin

  "should contain exactly one node with all mandatory fields set" in {
    val List(x) = cpg.metaData.l
    x.language shouldBe ExtractorConst.LANGUAGE_FRONTEND
    x.version shouldBe ExtractorConst.LANGUAGE_FRONTEND_VERSION
    x.overlays shouldBe List("semanticcpg")
  }

  "should not have any incoming or outgoing edges" in {
    cpg.metaData.size shouldBe 1
    cpg.metaData.in.l shouldBe List()
    cpg.metaData.out.l shouldBe List()
  }

}
