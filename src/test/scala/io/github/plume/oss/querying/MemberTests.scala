package io.github.plume.oss.querying

import io.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.semanticcpg.language._

class MemberTests extends Jimple2CpgFixture {

  override val code: String =
    """
      |class Foo {
      |  int x;
      |}
      |""".stripMargin

  "should contain MEMBER node with correct properties" in {
    val List(x) = cpg.member("x").l
    x.name shouldBe "x"
    x.code shouldBe "int x"
    x.typeFullName shouldBe "int"
    x.order shouldBe 2 // The other child is the <init> method
  }

  "should allow traversing from MEMBER to TYPE_DECL" in {
    val List(x) = cpg.member.typeDecl.l
    x.name shouldBe "Foo"
  }
}
