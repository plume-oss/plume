package com.github.plume.oss.drivers

import com.github.plume.oss.testfixtures.PlumeDriverFixture
import com.github.plume.oss.testfixtures.PlumeDriverFixture.{b1, m1}
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import io.shiftleft.passes.IntervalKeyPool
import overflowdb.BatchedUpdate

import java.io.{File => JFile}
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.util.Try

class OverflowDbTests extends PlumeDriverFixture(new OverflowDbDriver()) {

  private val methodSemantics     = JFile.createTempFile("method", ".semantics")
  private val methodSemanticsPath = Paths.get(methodSemantics.getAbsolutePath)

  Files.write(methodSemanticsPath, "\"Foo.bar\" 1->-1\n".getBytes(StandardCharsets.UTF_8))

  "should allow for custom method semantics to be defined" in {
    driver match {
      case x: OverflowDbDriver =>
        x.setDataflowContext(
          2,
          Some(Source.fromInputStream(Files.newInputStream(methodSemanticsPath)))
        )
    }
  }

  "should handle the case where no default semantics can be retrieved" in {
    val field: Field = driver.getClass.getDeclaredField("defaultSemantics")
    field.setAccessible(true)
    field.set(driver, Try.apply(throw new Exception("Foo")))

    driver match {
      case x: OverflowDbDriver =>
        x.setDataflowContext(
          2,
          Some(Source.fromInputStream(Files.newInputStream(methodSemanticsPath)))
        )
    }
  }

  // This is flimsy but it at least validates that things won't go wrong
  "should be able to serialize and deserialize XML graphs without throwing an exception" in {
    createSimpleGraph(driver)
    val td      = driver.asInstanceOf[OverflowDbDriver]
    val outFile = Paths.get("./odbGraph.xml").toFile
    td.exportAsGraphML(outFile)
    outFile.delete()
  }

  private def createSimpleGraph(driver: IDriver): Unit = {
    val cpg       = Cpg.empty
    val keyPool   = new IntervalKeyPool(1, 1000)
    val diffGraph = new BatchedUpdate.DiffGraphBuilder()
    diffGraph.addNode(m1).addNode(b1).addEdge(m1, b1, EdgeTypes.AST)
    val adg = BatchedUpdate.applyDiff(cpg.graph, diffGraph, keyPool, null)
    driver.bulkTx(adg)
  }

}
