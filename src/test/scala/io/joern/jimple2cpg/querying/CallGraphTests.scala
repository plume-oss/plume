package io.joern.jimple2cpg.querying

import io.joern.jimple2cpg.testfixtures.PlumeCpgFixture
import io.shiftleft.semanticcpg.language.{NoResolve, _}

class CallGraphTests extends PlumeCpgFixture {

  implicit val resolver: NoResolve.type = NoResolve

  override val code =
    """
       class Foo {
        int add(int x, int y) {
         return x + y;
        }
        int main(int argc, char argv) {
         System.out.println(add(1+2, 3));
         return 0;
        }
       }
    """

  "should find that add is called by main" in {
    cpg.method.name("add").caller.name.toSet shouldBe Set("main")
  }

  "should find that main calls add and others" in {
    // The addition here is solved already by the compiler
    cpg.method.name("main").callee.name.filterNot(_.startsWith("<operator>")).toSet shouldBe Set(
      "add",
      "println"
    )
  }

  "should find a set of outgoing calls for main" in {
    cpg.method.name("main").call.code.toSet shouldBe
      Set(
        "add(3, 3)",
        "println($stack4)",
        "$stack3 = <java.lang.System: java.io.PrintStream out>",
        "argc = @parameter0: int",
        "argv = @parameter1: char",
        "this = @this: Foo",
        "java.lang.System.out",
        "$stack4 = virtualinvoke this.<Foo: int add(int,int)>(3, 3)"
      )
  }

  "should find one callsite for add" in {
    cpg.method.name("add").callIn.code.toSet shouldBe Set("add(3, 3)")
  }

  "should find that argument '1+2' is passed to parameter 'x'" in {
    cpg.parameter.name("x").argument.code.toSet shouldBe Set("3")
  }

  "should allow traversing from argument to formal parameter" in {
    cpg.argument.parameter.name.toSet should not be empty
  }

  "should allow traversing from argument to call" in {
    cpg.method.name("println").callIn.argument.inCall.name.toSet shouldBe Set("println")
  }

}
