package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._

class MethodReturnTests extends PlumeCodeToCpgSuite {

  override val code =
    """class Foo {
      |  int foo() { return 1; }
      |}
      |""".stripMargin

  "should have METHOD_RETURN node with correct fields" in {
    val List(x) = cpg.method.name("foo").methodReturn.typeFullName("int").l
    x.code shouldBe "int"
    x.typeFullName shouldBe "int"
    // I think line 2 would be correct but close enough
    // given that it's bytecode
    x.lineNumber shouldBe Some(1)
    // we expect the METHOD_RETURN node to be the right-most
    // child so that when traversing the AST from left to
    // right in CFG construction, we visit it last.
    x.order shouldBe 2
  }

  "should allow traversing to method" in {
    cpg.methodReturn.code("int").method.name.l shouldBe List("foo")
  }

}
