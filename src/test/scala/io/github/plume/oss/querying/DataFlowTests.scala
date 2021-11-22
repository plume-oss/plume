package io.github.plume.oss.querying

import io.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.{CfgNode, MethodParameterIn}
import io.shiftleft.proto.cpg.Cpg.CpgStruct.Node.NodeType
import io.shiftleft.semanticcpg.language.{toControlStructure, toNodeTypeStarters}

class DataFlowTests extends Jimple2CpgFixture {

  override val code: String =
    """
      |class Foo {
      | int foo(int x, int y) {
      |  if (y < 10)
      |    return -1;
      |  if (x < 10) {
      |   sink(x);
      |  }
      |  System.out.println("foo");
      |  return 0;
      | }
      |
      | void sink(int x) {
      |   System.out.println(x);
      |   return;
      | }
      |}
    """.stripMargin

  "should find that parameter x reaches call to sink" in {
    import io.shiftleft.codepropertygraph.{Cpg => CPG}
    val cpg = CPG(driver.cpg.graph)
    val List(x: MethodParameterIn) = driver.getPath(cpg.parameter("x"), cpg.call("sink"))
    x.name shouldBe "x"
    x._methodViaAstIn.name shouldBe "foo"
  }

  "should find that parameter y reaches call to a condition operator" in {
    import io.shiftleft.codepropertygraph.{Cpg => CPG}
    val cpg = CPG(driver.cpg.graph)
    val List(x: MethodParameterIn) = driver.getPath(cpg.parameter("y"), cpg.call(Operators.greaterEqualsThan))
    x.name shouldBe "y"
    x._methodViaAstIn.name shouldBe "foo"
  }

}
