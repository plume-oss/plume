package io.github.plume.oss.querying
import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._

class MethodTests extends PlumeCodeToCpgSuite {

  override val code =
    """
      |class Foo {
      | int foo(int param1, int param2) {
      | return 1;
      | }
      |}
      |""".stripMargin

  "should return correct function/method name" in {
    cpg.method.name.toSet shouldBe Set("foo", "<init>")
  }
}
