package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._
import io.shiftleft.semanticcpg.language.types.structure.File

class NamespaceBlockTests extends PlumeCodeToCpgSuite {

  override val code: String =
    """
      |package foo.bar;
      |class A {
      | void foo() {}
      |}
      |""".stripMargin

  "should contain three namespace blocks in total (<global>, java.lang.object, foo.bar)" in {
    cpg.namespaceBlock.size shouldBe 3
    // There is no global namespace block in Java
  }

  "should contain correct namespace block for known file" in {
    val List(x) = cpg.namespaceBlock.filename("/foo/bar/A.class").l
    x.name shouldBe "foo.bar"
    x.filename should not be ""
    x.fullName shouldBe "/foo/bar/A.class:foo.bar"
    x.order shouldBe 1
  }

  "should allow traversing from namespace block to method" in {
    cpg.namespaceBlock.filename("/foo/bar/A.class").typeDecl.method.name.toSet shouldBe Set("foo", "<init>")
  }

  "should allow traversing from namespace block to type declaration" in {
    cpg.namespaceBlock.filename("/foo/bar/A.class").typeDecl.name.l shouldBe List("A")
  }

  "should allow traversing from namespace block to namespace" in {
    cpg.namespaceBlock.filename("/foo/bar/A.class").namespace.name.l shouldBe List("foo.bar")
  }

}
