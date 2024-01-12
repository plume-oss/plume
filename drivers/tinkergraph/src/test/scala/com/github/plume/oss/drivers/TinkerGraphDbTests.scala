package com.github.plume.oss.drivers

import com.github.plume.oss.testfixtures.PlumeDriverFixture.{b1, m1}
import com.github.plume.oss.testfixtures.PlumeDriverFixture
import io.shiftleft.codepropertygraph.generated.NodeTypes.{BLOCK, METHOD}
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import io.shiftleft.passes.IntervalKeyPool
import io.shiftleft.proto.cpg.Cpg.NodePropertyName.{NAME, ORDER}
import overflowdb.BatchedUpdate

import java.io.{File => JFile}

class TinkerGraphDbTests extends PlumeDriverFixture(new TinkerGraphDriver()) {

  private val graphML     = JFile.createTempFile("plume", ".xml")
  private val graphSON    = JFile.createTempFile("plume", ".json")
  private val gryo        = JFile.createTempFile("plume", ".kryo")
  private val invalidFile = JFile.createTempFile("plume", ".txt")

  "should be able to serialize and deserialize XML graphs" in {
    createSimpleGraph(driver)
    validateSimpleGraph(driver)
    val td = driver.asInstanceOf[TinkerGraphDriver]
    td.exportGraph(graphML.getAbsolutePath)
    td.clear()
    td.importGraph(graphML.getAbsolutePath)
    validateSimpleGraph(driver)
  }

  "should be able to serialize and deserialize JSON graphs" in {
    createSimpleGraph(driver)
    validateSimpleGraph(driver)
    val td = driver.asInstanceOf[TinkerGraphDriver]
    td.exportGraph(graphSON.getAbsolutePath)
    td.clear()
    td.importGraph(graphSON.getAbsolutePath)
    validateSimpleGraph(driver)
  }

  "should be able to serialize and deserialize KRYO graphs" in {
    val javaVersionMajorVersion = System.getProperty("java.version").split('.')(0).toInt
    assume(javaVersionMajorVersion < 17, "Kryo serialization requires Java version < 17")
    createSimpleGraph(driver)
    validateSimpleGraph(driver)
    val td = driver.asInstanceOf[TinkerGraphDriver]
    td.exportGraph(gryo.getAbsolutePath)
    td.clear()
    td.importGraph(gryo.getAbsolutePath)
    validateSimpleGraph(driver)
  }

  "should reject invalid file extension on export" in {
    val td = driver.asInstanceOf[TinkerGraphDriver]
    assertThrows[RuntimeException] {
      td.exportGraph(invalidFile.getAbsolutePath)
    }
  }

  "should reject invalid file extension on import" in {
    val td = driver.asInstanceOf[TinkerGraphDriver]
    assertThrows[RuntimeException] {
      td.importGraph(invalidFile.getAbsolutePath)
    }
  }

  private def createSimpleGraph(driver: IDriver): Unit = {
    val diffGraph = new BatchedUpdate.DiffGraphBuilder()
    diffGraph.addNode(m1).addNode(b1).addEdge(m1, b1, EdgeTypes.AST)
    driver.bulkTx(diffGraph)
  }

  private def validateSimpleGraph(driver: IDriver): Unit = {
    val List(m: Map[String, Any]) = driver.propertyFromNodes(METHOD, NAME.name(), ORDER.name())
    m.get(NAME.name()) shouldBe Some("foo")
    m.get(ORDER.name()) shouldBe Some(1)
    val List(b: Map[String, Any]) = driver.propertyFromNodes(BLOCK, ORDER.name())
    m.get(ORDER.name()) shouldBe Some(1)
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
