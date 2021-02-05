package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.codepropertygraph.generated.{Operators, nodes}
import io.shiftleft.semanticcpg.language.NoResolve
import io.shiftleft.semanticcpg.language._

class CallTests extends PlumeCodeToCpgSuite {

  implicit val resolver = NoResolve

  override val code = """
       class Foo {
         int add(int x, int y) {
            return x + y;
         }

        int main(int argc, char argv) {
         return add(argc, 3);
        }
       }
    """

  "should contain a call node for `add` with correct fields" in {
    val List(x) = cpg.call("add").l
    // TODO
    // x.code shouldBe "add((1+2), 3)"
    x.name shouldBe "add"
    x.order shouldBe 1
    x.methodInstFullName shouldBe None // Deprecated
    x.methodFullName shouldBe "Foo: int add(int,int)"
    x.argumentIndex shouldBe 1
    // TODO x.signature
    // x.typeFullName : deprecated
    x.lineNumber shouldBe Some(8)
  }

  "should allow traversing from call to arguments" in {
    cpg.call("add").argument.size shouldBe 2

    val List(arg1) = cpg.call("add").argument(1).l
    arg1.isInstanceOf[nodes.Identifier] shouldBe true
    arg1.asInstanceOf[nodes.Identifier].name shouldBe "argc"
    arg1.code shouldBe "argc"
    arg1.order shouldBe 1
    arg1.argumentIndex shouldBe 1

    val List(arg2) = cpg.call("add").argument(2).l
    arg2.isInstanceOf[nodes.Literal] shouldBe true
    arg2.asInstanceOf[nodes.Literal].code shouldBe "3"
    arg2.code shouldBe "3"
    arg2.order shouldBe 2
    arg2.argumentIndex shouldBe 2
  }

  "should allow traversing from call to surrounding method" in {
    val List(x) = cpg.call("add").method.l
    x.name shouldBe "main"
  }

  "should allow traversing from call to callee method" in {
    val List(x) = cpg.call("add").callee.l
    x.name shouldBe "add"
  }

  "should allow traversing from argument to parameter" in {
    val List(x) = cpg.call("add").argument(1).parameter.l
    x.name shouldBe "x"
  }

}
