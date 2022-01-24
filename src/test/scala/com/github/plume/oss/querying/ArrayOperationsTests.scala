package com.github.plume.oss.querying

import com.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.semanticcpg.language.{ICallResolver, NoResolve, toNodeTypeStarters, _}

class ArrayOperationsTests extends Jimple2CpgFixture {

  implicit val resolver: ICallResolver = NoResolve

  override val code: String =
    """
      | class Foo {
      |   static void main(int argc, char argv) {
      |     int[] a = new int[3];
      |     a[0] = 2;
      |     a[1] = 3;
      |     a[2] = a[0] + a[1];
      |   }
      | }
      |""".stripMargin

  "foo" in {
    println(cpg.method("main").dotAst.l)
  }

}
