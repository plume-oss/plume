package com.github.plume.oss.testfixtures

import com.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.PropertyNames.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.codepropertygraph.generated.{DispatchTypes, EdgeTypes}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import overflowdb.BatchedUpdate.DiffGraphBuilder
import overflowdb.{BatchedUpdate, DetachedNodeGeneric}

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.language.postfixOps
import scala.util.Try

class PlumeDriverFixture(val driver: IDriver)
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfter {

  import PlumeDriverFixture.*

  override protected def beforeAll(): Unit = {
    if (!driver.isConnected) fail("The driver needs to be connected before the tests can be run.")
  }

  after {
    driver.clear()
  }

  def nodeToNodeCreate(n: NewNode): DetachedNodeGeneric = {
    val props: Array[Object] = n.properties.flatMap { case (k, v) =>
      Iterable(k.asInstanceOf[Object], v.asInstanceOf[Object])
    }.toArray
    new DetachedNodeGeneric(n.label(), props*)
  }

  "overflowdb.BatchedUpdate.DiffGraph based changes" should {

    "should reflect node additions in bulk transactions" in {
      val diffGraph = new DiffGraphBuilder
      // Create some nodes
      diffGraph.addNode(nodeToNodeCreate(m1)).addNode(nodeToNodeCreate(b1))
      driver.bulkTx(diffGraph.build())
      val List(m: Map[String, Any]) =
        driver.propertyFromNodes(METHOD, NAME, ORDER, DYNAMIC_TYPE_HINT_FULL_NAME)
      m.get(NAME) shouldBe Some("foo")
      m.get(ORDER) shouldBe Some(1)
      val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER)
      b.get(ORDER) shouldBe Some(1)
    }

    "should reflect edge additions in bulk transactions" in {
      val diffGraph1 = new DiffGraphBuilder
      val diffGraph2 = new DiffGraphBuilder
      // Create some nodes
      diffGraph1.addNode(nodeToNodeCreate(m1)).addNode(b1)
      driver.bulkTx(diffGraph1)
      val List(m: Map[String, Any]) = driver.propertyFromNodes(METHOD, NAME, ORDER)
      m.get(NAME) shouldBe Some("foo")
      m.get(ORDER) shouldBe Some(1)
      val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER)
      b.get(ORDER) shouldBe Some(1)

      // Add an edge
      val changes = diffGraph1.iterator.asScala.toList
      val srcNode = changes
        .collectFirst {
          case c: DetachedNodeGeneric
              if c.getRefOrId == m.getOrElse("id", -1L).toString.toLong || Try(
                c.getRefOrId
                  .asInstanceOf[StoredNode]
                  .id()
              ).map(_ == m.getOrElse("id", -1L).toString.toLong).getOrElse(false) =>
            c
        } match {
        case Some(src) => src
        case None      => fail("Unable to extract method node")
      }
      val dstNode = changes
        .collectFirst {
          case c: NewBlock
              if Try(c.getRefOrId().asInstanceOf[Long])
                .map(_ == b.getOrElse("id", -1L).toString.toLong)
                .getOrElse(false) ||
                Try(c.getRefOrId().asInstanceOf[StoredNode].id())
                  .map(_ == b.getOrElse("id", -1L).toString.toLong)
                  .getOrElse(false) =>
            c
        } match {
        case Some(dst) => dst
        case None      => fail("Unable to extract block node")
      }
      diffGraph2.addEdge(srcNode, dstNode, EdgeTypes.AST)
      driver.bulkTx(diffGraph2.build())

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
  }

  override def afterAll(): Unit = {
    if (driver.isConnected) driver.close()
  }

  private def createSimpleGraph(dg: DiffGraphBuilder): Unit = {
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
      .addNode(m3)
      .addNode(b1)
      .addNode(c1)
      .addNode(c2)
      .addNode(li1)
      .addNode(l1)
      .addNode(i1)
      .addEdge(m1, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(m2, f2, EdgeTypes.SOURCE_FILE)
      .addEdge(m3, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(td1, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(td2, f2, EdgeTypes.SOURCE_FILE)
      .addEdge(n1, f1, EdgeTypes.SOURCE_FILE)
      .addEdge(n2, f2, EdgeTypes.SOURCE_FILE)
      .addEdge(t1, td1, EdgeTypes.REF)
      .addEdge(t2, td2, EdgeTypes.REF)
      .addEdge(n1, td1, EdgeTypes.AST)
      .addEdge(n2, td2, EdgeTypes.AST)
      .addEdge(td1, m1, EdgeTypes.AST)
      .addEdge(td1, m3, EdgeTypes.AST)
      .addEdge(td2, m2, EdgeTypes.AST)
      .addEdge(m1, b1, EdgeTypes.AST)
      .addEdge(b1, c1, EdgeTypes.AST)
      .addEdge(b1, c2, EdgeTypes.AST)
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
      .fullName("bar.Foo:foo():int")
      .order(1)
      .astParentType(TYPE_DECL)
      .astParentFullName(td1.fullName)
  val m2: NewMethod = NewMethod()
    .name("bar")
    .fullName("bar.Bar:bar(int,int):int")
    .order(1)
    .astParentType(TYPE_DECL)
    .astParentType(td2.fullName)
  val m3: NewMethod = NewMethod()
    .name("bar")
    .fullName("bar.Foo:bar(int,int):int")
    .order(2)
    .astParentType(TYPE_DECL)
    .astParentType(td1.fullName)
  val f1: NewFile  = NewFile().name("/bar/Foo.class").order(1)
  val f2: NewFile  = NewFile().name("/bar/Bar.class").order(1)
  val b1: NewBlock = NewBlock().order(1)
  val c1: NewCall = NewCall()
    .name("bar")
    .methodFullName("bar.Bar:bar(int,int):int")
    .dispatchType(DispatchTypes.STATIC_DISPATCH)
  val c2: NewCall = NewCall()
    .name("bar")
    .methodFullName("bar.Foo:bar(int,int):int")
    .dispatchType(DispatchTypes.DYNAMIC_DISPATCH)
  val l1: NewLocal      = NewLocal().name("x").typeFullName("int")
  val li1: NewLiteral   = NewLiteral().code("1").typeFullName("int")
  val i1: NewIdentifier = NewIdentifier().name("x").typeFullName("int")
}
