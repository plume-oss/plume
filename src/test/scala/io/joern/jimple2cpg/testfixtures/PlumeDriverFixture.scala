package io.joern.jimple2cpg.testfixtures

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

  import io.joern.jimple2cpg.testfixtures.PlumeDriverFixture._

  override protected def beforeAll(): Unit = {
    if (!driver.isConnected) driver.connect()
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

    val List(m: Seq[String]) = driver.propertyFromNodes(METHOD, "ID", NAME.name(), ORDER.name())
    m(1) shouldBe "foo"
    m(2) shouldBe 1.toString
    val List(b: Seq[String]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    b.head shouldBe 1.toString
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

    val List(m: Seq[String]) = driver.propertyFromNodes(METHOD, "ID", NAME.name(), ORDER.name())
    m(1) shouldBe "foo"
    m(2) shouldBe 1.toString
    val List(b: Seq[String]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    b.head shouldBe 1.toString

    // Remove one node
    diffGraph2.removeNode(m.head.toLong)
    val adg2 =
      DiffGraph.Applier.applyDiff(diffGraph2.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg2)

    driver.propertyFromNodes(METHOD, "ID") shouldBe List()
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

    val List(m: Seq[String]) = driver.propertyFromNodes(METHOD, "ID", NAME.name(), ORDER.name())
    m(1) shouldBe "foo"
    m(2) shouldBe 1.toString
    val List(b: Seq[String]) = driver.propertyFromNodes(BLOCK, "ID", ORDER.name())
    b(1) shouldBe 1.toString

    // Add an edge
    diffGraph2.addEdge(
      cpg.graph.nodes(m.head.toLong).next().asInstanceOf[AbstractNode],
      cpg.graph.nodes(b.head.toLong).next().asInstanceOf[AbstractNode],
      EdgeTypes.AST)
    val adg2 = DiffGraph.Applier.applyDiff(diffGraph2.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg2)

    driver.exists(m.head.toLong, b.head.toLong, EdgeTypes.AST) shouldBe true
    driver.exists(b.head.toLong, m.head.toLong, EdgeTypes.AST) shouldBe false
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

    val List(m: Seq[String]) = driver.propertyFromNodes(METHOD, "ID", NAME.name(), ORDER.name())
    m(1) shouldBe "foo"
    m(2) shouldBe 1.toString
    val List(b: Seq[String]) = driver.propertyFromNodes(BLOCK, "ID", ORDER.name())
    b(1) shouldBe 1.toString
    driver.exists(m.head.toLong, b.head.toLong, EdgeTypes.AST) shouldBe true

    diffGraph2.removeEdge(cpg.graph.node(m.head.toLong).outE(EdgeTypes.AST).next())
    val adg2 =
      DiffGraph.Applier.applyDiff(diffGraph2.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg2)

    driver.exists(m.head.toLong, b.head.toLong, EdgeTypes.AST) shouldBe false
  }

  "should recursively delete a node and its children given the node type, key-value pair, and edge type to follow" in {
    val cpg        = Cpg.empty
    val keyPool    = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some basic method
    diffGraph
      .addNode(meta)
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
    val adg =
      DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)

    driver.deleteNodeWithChildren(METHOD, EdgeTypes.AST, NAME.name(), "foo")
    // A deletion should delete all nodes except for the MetaData one
    driver.propertyFromNodes(METHOD, NAME.name()) shouldBe List()
    driver.propertyFromNodes(BLOCK, "ID") shouldBe List()
    driver.propertyFromNodes(CALL, NAME.name()) shouldBe List()
    driver.propertyFromNodes(LOCAL, NAME.name()) shouldBe List()
    driver.propertyFromNodes(IDENTIFIER, NAME.name()) shouldBe List()
    driver.propertyFromNodes(META_DATA, LANGUAGE.name()) shouldBe List(Seq("PLUME"))
  }

  override def afterAll(): Unit = {
    if (driver.isConnected) driver.close()
  }

}

object PlumeDriverFixture {
  val meta: NewMetaData = NewMetaData().language("PLUME").version("0.1")
  val m1: NewMethod = NewMethod().name("foo").order(1)
  val b1: NewBlock  = NewBlock().order(1)
  val c1: NewCall = NewCall().name(Operators.assignment)
  val l1: NewLocal = NewLocal().name("x")
  val i1: NewIdentifier = NewIdentifier().name("1")
}
