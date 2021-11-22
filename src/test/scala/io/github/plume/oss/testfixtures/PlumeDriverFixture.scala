package io.github.plume.oss.testfixtures

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.NodeTypes._
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.codepropertygraph.generated.{Cpg, DispatchTypes, EdgeTypes}
import io.shiftleft.passes.{DiffGraph, IntervalKeyPool}
import io.shiftleft.proto.cpg.Cpg.NodePropertyName._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.language.postfixOps

class PlumeDriverFixture(val driver: IDriver)
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfter {

  import io.github.plume.oss.testfixtures.PlumeDriverFixture._

  override protected def beforeAll(): Unit = {
    if (!driver.isConnected) fail("The driver needs to be connected before the tests can be run.")
  }

  after {
    driver.clear()
  }

  "should reflect node additions in bulk transactions" in {
    val cpg       = Cpg.empty
    val keyPool   = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some nodes
    diffGraph.addNode(m1).addNode(b1)
    val adg =
      DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)
    val List(m: Map[String, Any]) = driver.propertyFromNodes(METHOD, NAME.name(), ORDER.name())
    m.get(NAME.name()) shouldBe Some("foo")
    m.get(ORDER.name()) shouldBe Some(1)
    val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    b.get(ORDER.name()) shouldBe Some(1)
  }

  "should reflect node subtractions in bulk transactions" in {
    val cpg        = Cpg.empty
    val keyPool    = new IntervalKeyPool(1, 1000)
    val diffGraph1 = DiffGraph.newBuilder
    val diffGraph2 = DiffGraph.newBuilder
    // Create some nodes
    diffGraph1.addNode(m1).addNode(b1)
    val adg1 =
      DiffGraph.Applier.applyDiff(diffGraph1.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg1)

    val List(m: Map[String, Any]) = driver.propertyFromNodes(METHOD, NAME.name(), ORDER.name())
    m.get(NAME.name()) shouldBe Some("foo")
    m.get(ORDER.name()) shouldBe Some(1)
    val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    b.get(ORDER.name()) shouldBe Some(1)

    // Remove one node
    diffGraph2.removeNode(m.getOrElse("id", -1L).toString.toLong)
    val adg2 =
      DiffGraph.Applier.applyDiff(diffGraph2.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg2)

    driver.propertyFromNodes(METHOD) shouldBe List()
  }

  "should reflect edge additions in bulk transactions" in {
    val cpg        = Cpg.empty
    val keyPool    = new IntervalKeyPool(1, 1000)
    val diffGraph1 = DiffGraph.newBuilder
    val diffGraph2 = DiffGraph.newBuilder
    // Create some nodes
    diffGraph1.addNode(m1).addNode(b1)
    val adg1 =
      DiffGraph.Applier.applyDiff(diffGraph1.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg1)

    val List(m: Map[String, Any]) = driver.propertyFromNodes(METHOD, NAME.name(), ORDER.name())
    m.get(NAME.name()) shouldBe Some("foo")
    m.get(ORDER.name()) shouldBe Some(1)
    val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    b.get(ORDER.name()) shouldBe Some(1)

    // Add an edge
    diffGraph2.addEdge(
      cpg.graph.nodes(m.getOrElse("id", -1L).toString.toLong).next().asInstanceOf[AbstractNode],
      cpg.graph.nodes(b.getOrElse("id", -1L).toString.toLong).next().asInstanceOf[AbstractNode],
      EdgeTypes.AST
    )
    val adg2 =
      DiffGraph.Applier.applyDiff(diffGraph2.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg2)

    driver.exists(
      m.getOrElse("id", -1L).toString.toLong,
      b.getOrElse("id", -1L).toString.toLong,
      EdgeTypes.AST
    ) shouldBe true
    driver.exists(
      b.getOrElse("id", -1L).toString.toLong,
      m.getOrElse("id", -1L).toString.toLong,
      EdgeTypes.AST
    ) shouldBe false
  }

  "should reflect edge removal in bulk transactions" in {
    val cpg        = Cpg.empty
    val keyPool    = new IntervalKeyPool(1, 1000)
    val diffGraph1 = DiffGraph.newBuilder
    val diffGraph2 = DiffGraph.newBuilder
    // Create some nodes and an edge
    diffGraph1.addNode(m1).addNode(b1).addEdge(m1, b1, EdgeTypes.AST)
    val adg1 =
      DiffGraph.Applier.applyDiff(diffGraph1.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg1)

    val List(m: Map[String, Any]) = driver.propertyFromNodes(METHOD, NAME.name(), ORDER.name())
    m.get(NAME.name()) shouldBe Some("foo")
    m.get(ORDER.name()) shouldBe Some(1)
    val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    b.get(ORDER.name()) shouldBe Some(1)

    driver.exists(
      m.getOrElse("id", -1L).toString.toLong,
      b.getOrElse("id", -1L).toString.toLong,
      EdgeTypes.AST
    ) shouldBe true

    diffGraph2.removeEdge(
      cpg.graph.node(m.getOrElse("id", -1L).toString.toLong).outE(EdgeTypes.AST).next()
    )
    val adg2 =
      DiffGraph.Applier.applyDiff(diffGraph2.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg2)

    driver.exists(
      m.getOrElse("id", -1L).toString.toLong,
      b.getOrElse("id", -1L).toString.toLong,
      EdgeTypes.AST
    ) shouldBe false
  }

  "should accurately report which IDs have been taken" in {
    val cpg       = Cpg.empty
    val keyPool   = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some basic method
    createSimpleGraph(diffGraph)
    val adg =
      DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)

    driver.idInterval(1, 6).size shouldBe 6
    driver.idInterval(1, 20).size shouldBe 16
    driver.idInterval(1, 3).size shouldBe 3
    driver.idInterval(1001, 2000).size shouldBe 0
  }

  "should link AST children automatically" in {
    val cpg       = Cpg.empty
    val keyPool   = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some basic method
    createSimpleGraph(diffGraph)
    val adg =
      DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)

    driver.buildInterproceduralEdges()

    val List(t1: Map[String, Any], t2: Map[String, Any]) =
      driver
        .propertyFromNodes(TYPE, FULL_NAME.name())
        .sortBy { a => a(FULL_NAME.name()).asInstanceOf[String] }
        .reverse
    t1.get(FULL_NAME.name()) shouldBe Some("bar.Foo")
    t2.get(FULL_NAME.name()) shouldBe Some("bar.Bar")
    val List(td1: Map[String, Any], td2: Map[String, Any]) =
      driver
        .propertyFromNodes(TYPE_DECL, FULL_NAME.name())
        .sortBy { a => a(FULL_NAME.name()).asInstanceOf[String] }
        .reverse
    td1.get(FULL_NAME.name()) shouldBe Some("bar.Foo")
    td2.get(FULL_NAME.name()) shouldBe Some("bar.Bar")
    driver.exists(
      t1.getOrElse("id", -1L).asInstanceOf[Long],
      td1.getOrElse("id", -1L).asInstanceOf[Long],
      EdgeTypes.REF
    ) shouldBe true
    driver.exists(
      t2.getOrElse("id", -1L).asInstanceOf[Long],
      td2.getOrElse("id", -1L).asInstanceOf[Long],
      EdgeTypes.REF
    ) shouldBe true
    driver.exists(
      td1.getOrElse("id", -1L).asInstanceOf[Long],
      t2.getOrElse("id", -1L).asInstanceOf[Long],
      EdgeTypes.INHERITS_FROM
    ) shouldBe true
  }

  "should delete a source file's nodes precisely" in {
    val cpg       = Cpg.empty
    val keyPool   = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some basic method
    createSimpleGraph(diffGraph)
    val adg =
      DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)

    driver.buildInterproceduralEdges()
    // remove f1 nodes
    driver.removeSourceFiles(f1.name)

    val List(m: Map[String, Any])  = driver.propertyFromNodes(METHOD, NAME.name())
    val List(td: Map[String, Any]) = driver.propertyFromNodes(TYPE_DECL, NAME.name())
    val List(n: Map[String, Any])  = driver.propertyFromNodes(NAMESPACE_BLOCK, NAME.name())
    m.getOrElse(NAME.name(), -1L).asInstanceOf[String] shouldBe m2.name
    td.getOrElse(NAME.name(), -1L).asInstanceOf[String] shouldBe td2.name
    n.getOrElse(NAME.name(), -1L).asInstanceOf[String] shouldBe n2.name
  }

  override def afterAll(): Unit = {
    if (driver.isConnected) driver.close()
  }

  private def createSimpleGraph(dg: DiffGraph.Builder): Unit = {
    dg.addNode(meta)
      .addNode(f1)
      .addNode(f2)
      .addNode(td1)
      .addNode(td2)
      .addNode(t1)
      .addNode(t2)
      .addNode(n1)
      .addNode(n2)
      .addNode(m1)
      .addNode(m2)
      .addNode(b1)
      .addNode(c1)
      .addNode(li1)
      .addNode(l1)
      .addNode(i1)
      .addEdge(m1, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(m2, f2, EdgeTypes.SOURCE_FILE)
      .addEdge(td1, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(td2, f2, EdgeTypes.SOURCE_FILE)
      .addEdge(n1, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(n2, f2, EdgeTypes.SOURCE_FILE)
      .addEdge(t1, td1, EdgeTypes.REF)
      .addEdge(t2, td2, EdgeTypes.REF)
      .addEdge(n1, td1, EdgeTypes.AST)
      .addEdge(n2, td2, EdgeTypes.AST)
      .addEdge(td1, m1, EdgeTypes.AST)
      .addEdge(td2, m2, EdgeTypes.AST)
      .addEdge(m1, b1, EdgeTypes.AST)
      .addEdge(b1, c1, EdgeTypes.AST)
      .addEdge(b1, l1, EdgeTypes.AST)
      .addEdge(c1, li1, EdgeTypes.AST)
      .addEdge(c1, i1, EdgeTypes.AST)
      .addEdge(m1, c1, EdgeTypes.CFG)
  }

}

object PlumeDriverFixture {
  val meta: NewMetaData = NewMetaData().language("PLUME").version("0.1")
  val n1: NewNamespaceBlock = NewNamespaceBlock()
    .name("bar")
    .fullName("bar")
    .filename("/bar/Foo.class")
  val n2: NewNamespaceBlock = NewNamespaceBlock()
    .name("bar")
    .fullName("bar")
    .filename("/bar/Bar.class")
  val td1: NewTypeDecl = NewTypeDecl()
    .name("Foo")
    .fullName("bar.Foo")
    .filename("/bar/Foo.class")
    .inheritsFromTypeFullName(List("bar.Bar"))
    .astParentType(NAMESPACE_BLOCK)
    .astParentFullName(n1.fullName)
  val t1: NewType = NewType()
    .name("Foo")
    .fullName("bar.Foo")
    .typeDeclFullName("bar.Foo")
  val td2: NewTypeDecl = NewTypeDecl()
    .name("Bar")
    .fullName("bar.Bar")
    .filename("/bar/Bar.class")
    .astParentType(NAMESPACE_BLOCK)
    .astParentFullName(n1.fullName)
  val t2: NewType = NewType()
    .name("Bar")
    .fullName("bar.Bar")
    .typeDeclFullName("bar.Bar")
  val m1: NewMethod =
    NewMethod()
      .name("foo")
      .order(1)
      .astParentType(TYPE_DECL)
      .astParentFullName(td1.fullName)
  val m2: NewMethod = NewMethod()
    .name("bar")
    .fullName("bar.Bar:bar(int,int):int")
    .order(1)
    .astParentType(TYPE_DECL)
    .astParentType(td2.fullName)
  val f1: NewFile  = NewFile().name("/bar/Foo.class").order(1)
  val f2: NewFile  = NewFile().name("/bar/Bar.class").order(1)
  val b1: NewBlock = NewBlock().order(1)
  val c1: NewCall = NewCall()
    .name("bar")
    .methodFullName("bar.Bar:bar(int,int):int")
    .dispatchType(DispatchTypes.STATIC_DISPATCH)
  val l1: NewLocal      = NewLocal().name("x").typeFullName("int")
  val li1: NewLiteral   = NewLiteral().code("1").typeFullName("int")
  val i1: NewIdentifier = NewIdentifier().name("x").typeFullName("int")
}
