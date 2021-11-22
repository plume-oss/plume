package io.github.plume.oss.querying

import io.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.codepropertygraph.generated.{Operators, PropertyNames}
import io.shiftleft.codepropertygraph.generated.nodes.MethodParameterIn
import io.shiftleft.codepropertygraph.{Cpg => CPG}
import io.shiftleft.semanticcpg.language.{toCfgNode, toNodeTypeStarters}
import org.apache.tinkerpop.shaded.jackson.databind.PropertyName

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

  "should find that parameter x in foo reaches call to sink" in {
    val cpg                        = CPG(driver.cpg.graph)
    val List(x: MethodParameterIn) = driver.nodesReachableBy(cpg.parameter("x").filter(_._methodViaAstIn.name == "foo"), cpg.call("sink"))
    x.name shouldBe "x"
    x._methodViaAstIn.name shouldBe "foo"
  }

  "should find that parameter y reaches call to a condition operator" in {
    val cpg = CPG(driver.cpg.graph)
    val List(x: MethodParameterIn) =
      driver.nodesReachableBy(cpg.parameter("y"), cpg.call(Operators.greaterEqualsThan))
    x.name shouldBe "y"
    x._methodViaAstIn.name shouldBe "foo"
  }

  "should find that System.out.println in sink is reached by both x parameters" in {
    val cpg                        = CPG(driver.cpg.graph)
    val List(fooX: MethodParameterIn, sinkX: MethodParameterIn) = driver.nodesReachableBy(cpg.parameter("x"), cpg.call("println"))
    fooX.name shouldBe "x"
    fooX._methodViaAstIn.name shouldBe "foo"
    sinkX.name shouldBe "x"
    sinkX._methodViaAstIn.name shouldBe "sink"
  }

}
