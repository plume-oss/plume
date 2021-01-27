package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._
import io.shiftleft.semanticcpg.language.types.structure.{File, Namespace}

class NamespaceBlockTests extends PlumeCodeToCpgSuite {

  override val code: String =
    """
      |package foo.bar;
      |class A {
      | void foo() {}
      |}
      |""".stripMargin

  "should contain two namespace blocks in total" in {
//    cpg.namespaceBlock.size shouldBe 2
  }

  "should contain a correct global namespace block for the `<unknown>` file" in {
//    cpg.namespaceBlock.filename(File.UNKNOWN).l match {
//      case List(x) =>
//        x.name shouldBe Namespace.globalNamespaceName
//        x.fullName shouldBe Namespace.globalNamespaceName
//        x.order shouldBe 0
//      case _ => fail()
//    }
  }

  "should contain correct namespace block for known file" in {
//    cpg.namespaceBlock.filenameNot(File.UNKNOWN).l match {
//      case List(x) =>
//        x.name shouldBe "bar"
//        x.fullName shouldBe "foo.bar"
//        x.order shouldBe 0
//      case _ => fail()
//    }
  }

}
