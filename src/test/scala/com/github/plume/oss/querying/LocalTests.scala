package com.github.plume.oss.querying

import com.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.codepropertygraph.generated.nodes.Local
import io.shiftleft.semanticcpg.language._

class LocalTests extends Jimple2CpgFixture {

  override val code: String =
    """
      | @SuppressWarnings("deprecation")
      | class Foo {
      |   Integer foo() {
      |     int x;
      |     Integer y = null;
      |     x = 1;
      |     y = new Integer(x);
      |     return y;
      |   }
      | }
      |""".stripMargin

  "should contain locals `x` and `y` with correct fields set" in {
    val List(x: Local) = cpg.local("\\$stack3").l
    val List(y: Local) = cpg.local("y").l
    x.name shouldBe "$stack3"
    x.code shouldBe "java.lang.Integer $stack3"
    x.typeFullName shouldBe "java.lang.Integer"
    x.order shouldBe 1

    y.name shouldBe "y"
    y.code shouldBe "java.lang.Integer y"
    y.typeFullName shouldBe "java.lang.Integer"
    y.order shouldBe 3
  }
}
