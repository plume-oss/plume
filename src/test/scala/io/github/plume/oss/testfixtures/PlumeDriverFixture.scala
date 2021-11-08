package io.github.plume.oss.testfixtures

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.NodeTypes._
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes, Operators}
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

  "should recursively delete a node and its children given the node type, key-value pair, and edge type to follow" in {
    val cpg       = Cpg.empty
    val keyPool   = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some basic method
    createSimpleGraph(diffGraph)
    val adg =
      DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)

    driver.deleteNodeWithChildren(METHOD, EdgeTypes.AST, NAME.name(), "foo")
    // A deletion should delete all nodes except for the MetaData one
    driver.propertyFromNodes(METHOD, NAME.name()) shouldBe List()
    driver.propertyFromNodes(BLOCK) shouldBe List()
    driver.propertyFromNodes(CALL, NAME.name()) shouldBe List()
    driver.propertyFromNodes(LOCAL, NAME.name()) shouldBe List()
    driver.propertyFromNodes(IDENTIFIER, NAME.name()) shouldBe List()
    driver.propertyFromNodes(META_DATA, LANGUAGE.name()) shouldBe List(
      Map(LANGUAGE.name() -> "PLUME", "id" -> 1)
    )
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
    driver.idInterval(1, 10).size shouldBe 8
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

    driver.astLinker()

    val List(m: Map[String, Any])  = driver.propertyFromNodes(METHOD)
    val List(td: Map[String, Any]) = driver.propertyFromNodes(TYPE_DECL)
    val List(n: Map[String, Any])  = driver.propertyFromNodes(NAMESPACE_BLOCK)
    println(m.getOrElse("id", -1L).asInstanceOf[Long])
    println(td.getOrElse("id", -1L).asInstanceOf[Long])
    println(n.getOrElse("id", -1L).asInstanceOf[Long])
    driver.exists(
      td.getOrElse("id", -1L).asInstanceOf[Long],
      m.getOrElse("id", -1L).asInstanceOf[Long],
      EdgeTypes.AST
    ) shouldBe true
    driver.exists(
      n.getOrElse("id", -1L).asInstanceOf[Long],
      td.getOrElse("id", -1L).asInstanceOf[Long],
      EdgeTypes.AST
    ) shouldBe true
  }

  override def afterAll(): Unit = {
    if (driver.isConnected) driver.close()
  }

  private def createSimpleGraph(dg: DiffGraph.Builder): Unit = {
    dg.addNode(meta)
      .addNode(t1)
      .addNode(n1)
      .addNode(m1)
      .addNode(b1)
      .addNode(c1)
      .addNode(l1)
      .addNode(i1)
      .addEdge(m1, b1, EdgeTypes.AST)
      .addEdge(b1, c1, EdgeTypes.AST)
      .addEdge(c1, l1, EdgeTypes.AST)
      .addEdge(c1, i1, EdgeTypes.AST)
      .addEdge(m1, c1, EdgeTypes.CFG)
  }

}

object PlumeDriverFixture {
  val meta: NewMetaData     = NewMetaData().language("PLUME").version("0.1")
  val n1: NewNamespaceBlock = NewNamespaceBlock().name("bar").fullName("bar")
  val t1: NewTypeDecl = NewTypeDecl()
    .name("Foo")
    .fullName("bar.Foo")
    .astParentType(NAMESPACE_BLOCK)
    .astParentFullName(n1.fullName)
  val m1: NewMethod =
    NewMethod()
      .name("foo")
      .order(1)
      .astParentType(TYPE_DECL)
      .astParentFullName(t1.fullName)
  val b1: NewBlock      = NewBlock().order(1)
  val c1: NewCall       = NewCall().name(Operators.assignment)
  val l1: NewLocal      = NewLocal().name("x")
  val i1: NewIdentifier = NewIdentifier().name("1")
}
