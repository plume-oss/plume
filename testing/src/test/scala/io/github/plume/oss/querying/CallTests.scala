package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.codepropertygraph.generated.nodes
import io.shiftleft.semanticcpg.language.NoResolve
import io.shiftleft.semanticcpg.language._

class CallTests extends PlumeCodeToCpgSuite {

  implicit val resolver: ICallResolver = NoResolve

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
    x.code shouldBe "add(argc, 3)"
    x.name shouldBe "add"
    x.order shouldBe 2
    x.methodInstFullName shouldBe None // Deprecated
    x.methodFullName shouldBe "Foo.add:int(int,int)"
    x.signature shouldBe "int(int,int)"
    x.argumentIndex shouldBe 2
    // x.typeFullName : deprecated
    x.lineNumber shouldBe Some(8)
  }

  "should allow traversing from call to arguments" in {
    cpg.call("add").argument.size shouldBe 3
    val List(arg0) = cpg.call("add").argument(0).l
    arg0.isInstanceOf[nodes.Identifier] shouldBe true
    arg0.asInstanceOf[nodes.Identifier].name shouldBe "this"
    arg0.code shouldBe "this"
    arg0.order shouldBe 0
    arg0.argumentIndex shouldBe 0

    val List(arg1) = cpg.call("add").argument(1).l
    arg1.isInstanceOf[nodes.Identifier] shouldBe true
    arg1.asInstanceOf[nodes.Identifier].name shouldBe "argc"
    arg1.code shouldBe "argc"
    arg1.order shouldBe 1
    arg1.argumentIndex shouldBe 1

    val List(arg2) = cpg.call("add").argument(2).l
    arg2.asInstanceOf[nodes.Literal].code shouldBe "3"
    arg2.isInstanceOf[nodes.Literal] shouldBe true
    arg2.code shouldBe "3"
    arg2.order shouldBe 2
    arg2.argumentIndex shouldBe 2
  }

  // TODO: This requires contains pass to work
//  "should allow traversing from call to surrounding method" in {
//    val List(x) = cpg.call("add").method.l
//    x.name shouldBe "main"
//  }

  "should allow traversing from call to callee method" in {
    val List(x) = cpg.call("add").callee.l
    x.name shouldBe "add"
  }

  "should allow traversing from argument to parameter" in {
    val List(x) = cpg.call("add").argument(1).parameter.l
    x.name shouldBe "x"
  }

}
