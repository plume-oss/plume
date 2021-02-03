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

  "should contain one namespace blocks in total" in {
    cpg.namespaceBlock.size shouldBe 1
    // There is no global namespace block in Java
  }

  "should contain correct namespace block for known file" in {
    val List(x) = cpg.namespaceBlock.filenameNot(File.UNKNOWN).l
    x.name shouldBe "foo.bar"
    x.filename should not be ""
    x.fullName shouldBe "foo.bar"
    x.order shouldBe 1
  }

  "should allow traversing from namespace block to method" in {
    cpg.namespaceBlock.filenameNot(File.UNKNOWN).typeDecl.method.name.toSet shouldBe Set("foo", "<init>")
  }

  "should allow traversing from namespace block to type declaration" in {
    cpg.namespaceBlock.filenameNot(File.UNKNOWN).typeDecl.name.l shouldBe List("A")
  }

  "should allow traversing from namespace block to namespace" in {
    cpg.namespaceBlock.filenameNot(File.UNKNOWN).namespace.name.l shouldBe List("foo.bar")
  }

}
