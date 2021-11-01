package io.joern.jimple2cpg.testfixtures

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import io.shiftleft.codepropertygraph.generated.NodeTypes._
import io.shiftleft.codepropertygraph.generated.nodes.{AbstractNode, NewBlock, NewMethod}
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

  override def afterAll(): Unit = {
    if (driver.isConnected) driver.close()
  }

}

object PlumeDriverFixture {
  val m1: NewMethod = NewMethod().name("foo").order(1)
  val b1: NewBlock  = NewBlock().order(1)
}
