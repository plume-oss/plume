package com.github.plume.oss.drivers

import com.github.plume.oss.testfixtures.PlumeDriverFixture
import com.github.plume.oss.testfixtures.PlumeDriverFixture.{b1, m1}
import com.github.plume.oss.util.DataFlowCacheConfig
import io.joern.dataflowengineoss.semanticsloader.Parser
import io.shiftleft.codepropertygraph.generated.{Cpg, EdgeTypes}
import io.shiftleft.passes.IntervalKeyPool
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import overflowdb.BatchedUpdate

import java.io.{File => JFile}
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.io.Source
import scala.util.{Failure, Success, Try}

class OverflowDbTests extends PlumeDriverFixture(new OverflowDbDriver()) {

  private val methodSemantics     = JFile.createTempFile("method", ".semantics")
  private val methodSemanticsPath = Paths.get(methodSemantics.getAbsolutePath)

  Files.write(methodSemanticsPath, "\"Foo.bar\" 1->-1\n".getBytes(StandardCharsets.UTF_8))

  "should allow for custom method semantics to be defined" in {
    val parser = new Parser()
    val rawSemantics = Source
      .fromInputStream(Files.newInputStream(methodSemanticsPath))
      .getLines()
      .mkString("\n")
    val config = DataFlowCacheConfig(methodSemantics = Some(parser.parse(rawSemantics)))
    new OverflowDbDriver(cacheConfig = config).close()
  }

  "should handle the case where no default semantics can be retrieved" in {
    val field: Field = driver.getClass.getDeclaredField("defaultSemanticsFile")
    field.setAccessible(true)
    field.set(driver, null)
    new OverflowDbDriver().close()
  }

  "should be able to serialize and deserialize XML graphs without throwing an exception" in {
    createSimpleGraph(driver)
    val td      = driver.asInstanceOf[OverflowDbDriver]
    val outFile = Paths.get("./odbGraph.xml").toFile
    td.exportAsGraphML(outFile)
    // Should be valid if TinkerGraph can accept it
    Try({
      val graph = TinkerGraph.open()
      graph.traversal().io[Any](outFile.getAbsolutePath).read().iterate()
      graph.close()
    }) match {
      case Failure(e) => fail("TinkerGraph could not import ODB generated XML", e)
      case _          =>
    }
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
