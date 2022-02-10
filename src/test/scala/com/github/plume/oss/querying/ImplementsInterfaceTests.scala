package com.github.plume.oss.querying

import com.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.semanticcpg.language._
import io.shiftleft.semanticcpg.language.types.structure.FileTraversal

import java.io.{File => JFile}

class ImplementsInterfaceTests extends Jimple2CpgFixture {

  override val code: String =
    """
      |import java.io.Serializable;
      |
      |class Foo implements Serializable {
      |
      |   public int add(int x, int y) {
      |     return x + y;
      |   }
      |
      |}
      |""".stripMargin

  "should contain a type decl for `Foo` with correct fields" in {
    val List(x) = cpg.typeDecl.name("Foo").l
    x.name shouldBe "Foo"
    x.code shouldBe "Foo"
    x.fullName shouldBe "Foo"
    x.isExternal shouldBe false
    x.inheritsFromTypeFullName shouldBe List("java.lang.Object", "java.io.Serializable")
    x.aliasTypeFullName shouldBe None
    x.order shouldBe 1
    x.filename.startsWith(JFile.separator) shouldBe true
    x.filename.endsWith(".class") shouldBe true
  }

  "should contain a type decl for `Serializable` with correct fields" in {
    val List(x) = cpg.typeDecl.name("Serializable").l
    x.name shouldBe "Serializable"
    x.fullName shouldBe "java.io.Serializable"
    x.isExternal shouldBe true
    x.aliasTypeFullName shouldBe None
    x.order shouldBe -1
    x.filename shouldBe FileTraversal.UNKNOWN
  }

}
