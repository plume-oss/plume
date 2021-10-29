package io.joern.jimple2cpg.testfixtures

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.NodeTypes._
import io.shiftleft.codepropertygraph.generated.nodes.{Block, Method, NewBlock, NewMethod}
import io.shiftleft.passes.{DiffGraph, IntervalKeyPool}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class PlumeDriverFixture(val driver: IDriver) extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  import io.joern.jimple2cpg.testfixtures.PlumeDriverFixture._

  override protected def beforeAll(): Unit = {
    if (!driver.isConnected) driver.connect()
  }

  "should reflect node additions in bulk transactions" in {
    val cpg = Cpg.empty
    val keyPool = new IntervalKeyPool(1, 1000)
    val diffGraph = DiffGraph.newBuilder
    // Create some nodes
    diffGraph.addNode(m1)
    diffGraph.addNode(b1)
    val adg = DiffGraph.Applier.applyDiff(diffGraph.build(), cpg.graph, undoable = false, Option(keyPool))
    driver.bulkTx(adg)

    val List(m: Method) = driver.nodesByLabel(METHOD)
    m.name shouldBe "foo"
    m.order shouldBe 1
    val List(b: Block) = driver.nodesByLabel(BLOCK)
    b.order shouldBe 1
  }

  override def afterAll(): Unit = {
    if (driver.isConnected) driver.close()
  }

}

object PlumeDriverFixture {
  val m1: NewMethod = NewMethod().name("foo").order(1)
  val b1: NewBlock = NewBlock().order(1)
}