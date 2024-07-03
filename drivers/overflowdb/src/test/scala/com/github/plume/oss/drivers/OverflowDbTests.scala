package com.github.plume.oss.drivers

import better.files.File
import com.github.plume.oss.testfixtures.PlumeDriverFixture
import com.github.plume.oss.testfixtures.PlumeDriverFixture.{b1, m1}
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import io.shiftleft.passes.IntervalKeyPool
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import overflowdb.BatchedUpdate

import java.io.{File => JFile}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.util.{Failure, Try}

class OverflowDbTests extends PlumeDriverFixture(new OverflowDbDriver()) {

  private val methodSemantics     = JFile.createTempFile("method", ".semantics")
  private val methodSemanticsPath = Paths.get(methodSemantics.getAbsolutePath)

  Files.write(methodSemanticsPath, "\"Foo.bar\" 1->-1\n".getBytes(StandardCharsets.UTF_8))

  "should be able to serialize and deserialize XML graphs without throwing an exception" in {
    createSimpleGraph(driver)
    val td      = driver.asInstanceOf[OverflowDbDriver]
    val outFile = Paths.get("./odbGraph.xml").toFile
    td.exportAsGraphML(outFile)
    // Should be valid if TinkerGraph can accept it
    Try {
      val graph = TinkerGraph.open()
      graph.traversal().io[Any](outFile.getAbsolutePath).read().iterate()
      graph.close()
    } match {
      case Failure(e) => fail("TinkerGraph could not import ODB generated XML", e)
      case _          =>
    }
    outFile.delete()
  }

  private def createSimpleGraph(driver: IDriver): Unit = {
    val diffGraph = new BatchedUpdate.DiffGraphBuilder()
    diffGraph.addNode(m1.copy).addNode(b1.copy).addEdge(m1.copy, b1.copy, EdgeTypes.AST)
    driver.bulkTx(diffGraph)
  }

}
