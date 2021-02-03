package io.github.plume.oss.querying

import io.github.plume.oss.PlumeCodeToCpgSuite
import io.shiftleft.semanticcpg.language._
import io.shiftleft.semanticcpg.language.types.structure.File

class FileTests extends PlumeCodeToCpgSuite {

  override val code: String =
    """
      | package a.b;
      | class Foo { int bar() { return 1; } }
      |""".stripMargin

  "should contain one file node in total with order=0" in {
    cpg.file.order.l shouldBe List(0)
    cpg.file.nameNot(File.UNKNOWN).size shouldBe 1
  }

  "should contain exactly one non-placeholder file with absolute path in `name`" in {
    val List(x) = cpg.file.nameNot(File.UNKNOWN).l
    x.name should startWith("/")
    x.hash.isDefined shouldBe true
  }

  "should allow traversing from file to its namespace blocks" in {
    cpg.file.nameNot(File.UNKNOWN).namespaceBlock.name.toSet shouldBe Set("a.b")
  }

  "should allow traversing from file to its methods via namespace block" in {
    cpg.file.nameNot(File.UNKNOWN).method.name.toSet shouldBe Set("<init>", "bar")
  }

  "should allow traversing from file to its type declarations via namespace block" in {
    cpg.file.nameNot(File.UNKNOWN).typeDecl.name.toSet shouldBe Set("Foo")
  }

}

