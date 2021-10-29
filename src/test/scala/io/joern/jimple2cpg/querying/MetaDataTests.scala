package io.joern.jimple2cpg.querying

import io.joern.jimple2cpg.testfixtures.Jimple2CpgFixture
import io.shiftleft.semanticcpg.language._

class MetaDataTests extends Jimple2CpgFixture {

  override val code: String =
    """
      |class Foo {}
      |""".stripMargin

  "should contain exactly one node with all mandatory fields set" in {
    val List(x) = cpg.metaData.l
    x.language shouldBe "PLUME"
    x.version shouldBe "0.1"
    x.overlays shouldBe List("semanticcpg")
  }

  "should not have any incoming or outgoing edges" in {
    cpg.metaData.size shouldBe 1
    cpg.metaData.in.l shouldBe List()
    cpg.metaData.out.l shouldBe List()
  }

}
