package com.github.plume.oss.querying

import com.github.plume.oss.drivers.OverflowDbDriver
import com.github.plume.oss.testfixtures.Jimple2CpgFixture
import io.shiftleft.codepropertygraph.{Cpg => CPG}
import io.shiftleft.semanticcpg.language._

class DataFlowTests extends Jimple2CpgFixture(Some(new OverflowDbDriver())) {

  override val code: String =
    """
      |class Foo {
      | public static int foo(int a) {
      |  taint(a);
      |  if (a < 10) {
      |    bar(a);
      |  }
      |  falseClean(a);
      |  baz(a);
      |  return 0;
      | }
      |
      | public static int taint(int z) {
      |   return z;
      | }
      |
      | public static void bar(int x) {
      |   System.out.println(x);
      | }
      |
      | public static int falseClean(int u) {
      |   return u;
      | }
      |
      | public static void baz(int y) {
      |   System.out.println(y);
      | }
      |}
    """.stripMargin

  override def afterAll(): Unit = {
    println("After all")
    driver.clear()
  }

  "should find that parameter 'a' in Foo(a) reaches call to a condition operator" in {
    val cpg = CPG(driver.cpg.graph)

    val r = driver
      .flowsBetween(cpg.parameter("a"), cpg.call("<operator>.*"))
    val List(v1) = r.map(r => r.path.map(x => (x.node.method.name, x.node.code)))

    v1.head shouldBe ("foo", "int a")
    v1.last shouldBe ("foo", "a >= 10")
  }

  "should find that parameter 'a' in Foo(a) reaches call to baz" in {
    val cpg = CPG(driver.cpg.graph)

    val r = driver
      .flowsBetween(cpg.parameter("a"), cpg.call("bar"))
    val List(v1) = r.map(r => r.path.map(x => (x.node.method.name, x.node.code)))

    v1.head shouldBe ("foo", "int a")
    v1.last shouldBe ("foo", "bar(a)")
  }

  "should find that parameter 'a' in Foo(a) should reach println(y) in bar and baz via two flows" in {
    val cpg = CPG(driver.cpg.graph)

    val r = driver
      .flowsBetween(cpg.parameter("a"), cpg.call("println"))

    r.map(r => r.path.map(x => (x.node.method.name, x.node.code))).foreach(println)

    r.size shouldBe 2

    val List(v1, v2) = r.map(r => r.path.map(x => (x.node.method.name, x.node.code)))

    v1.head shouldBe ("foo", "int a")
    v1.last shouldBe ("baz", "$stack1.println(y)")
    v2.head shouldBe ("foo", "int a")
    v2.last shouldBe ("bar", "$stack1.println(x)")
  }

  "should find that parameters tainted by calls to 'taint' is not reached if falseClean is set as a sanitizer" in {
    val cpg = CPG(driver.cpg.graph)

    def source = cpg.call("taint").argument
    def sink   = cpg.call("baz")

    val r1 = driver.flowsBetween(source, sink)
    r1.map(r => r.path.map(x => (x.node.method.name, x.node.code))).foreach(println)
    r1.size shouldBe 1

    val r2 = driver.flowsBetween(source, sink, Set("Foo.falseClean:int(int)"))
    r2.size shouldBe 0
  }

}
