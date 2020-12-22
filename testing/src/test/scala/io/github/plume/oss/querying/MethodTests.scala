package io.github.plume.oss.querying
import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._

class MethodTests extends PlumeCodeToCpgSuite {

  override val code =
    """ class Foo {
      | int foo(int param1, int param2) {
      | return 1;
      | }
      |}
      |""".stripMargin

  "should return correct function/method name" in {
    cpg.method.name.toSet shouldBe Set("foo", "<init>")
  }

  "should return correct line number" in {
    cpg.method.lineNumber.toSet shouldBe Set(0,2)
  }

  // TODO If there is a way to obtain a `lineNumber` end, that
  // should be implemented and the following two tests should
  // be commented in

  //  "should return correct end line number" in {
  //    cpg.method.name("foo").lineNumberEnd.l shouldBe List(4)
  //  }
  //
  //  "should return correct number of lines" in {
  //    cpg.method.name("foo").numberOfLines.l shouldBe List(2)
  //  }

  "should have correct method signature" in {
    cpg.method.name("foo").signature.toSet shouldBe Set("int foo(int,int)")
  }

  "should return correct number of parameters" in {
    cpg.method.name("foo").parameter.name.toSet shouldBe Set("param1", "param2")
  }

  // TODO `EVAL_TYPE/REF` edges seem to not be correct
  //  "should return correct parameter types" in {
  //    cpg.parameter.name("param1").evalType.l shouldBe List("int")
  //    cpg.parameter.name("param2").evalType.l shouldBe List("int")
  //  }

//  "should return correct return type" in {
//    cpg.methodReturn.evalType.l shouldBe List("int")
//    cpg.method.name("foo").methodReturn.evalType.l shouldBe List("int")
//    cpg.parameter.name("argc").method.methodReturn.evalType.l shouldBe List("int")
//  }

  "should return a filename for method 'foo'" in {
    cpg.method.name("foo").file.name.l should not be empty
  }

  "should allow filtering by number of parameters" in {
    cpg.method.filter(_.parameter.size == 2).name.l shouldBe List("foo")
    cpg.method.filter(_.parameter.size == 1).name.l shouldBe List()
  }

}
