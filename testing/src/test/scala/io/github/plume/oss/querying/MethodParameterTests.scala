package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._

class MethodParameterTests extends PlumeCodeToCpgSuite {

  override val code =
    """package a;
      |class Foo {
      | int foo(int param1, int param2) {
      |  return 0;
      | }
      |}
      """.stripMargin

  "should return exactly two parameters with correct fields" in {
    cpg.parameter.name.toSet shouldBe Set("param1", "param2")

    val List(x) = cpg.parameter.name("param1").l
    x.code shouldBe "int param1"
    x.typeFullName shouldBe "int"
    x.lineNumber shouldBe Some(3)
    // x.columnNumber shouldBe Some(11)
    x.order shouldBe 1

    val List(y) = cpg.parameter.name("param2").l
    y.code shouldBe "int param2"
    y.typeFullName shouldBe "int"
    y.lineNumber shouldBe Some(2)
    // y.columnNumber shouldBe Some(21)
    y.order shouldBe 2
  }

  "should allow traversing from parameter to method" in {
    cpg.parameter.name("param1").method.name.l shouldBe List("foo")
  }

}
