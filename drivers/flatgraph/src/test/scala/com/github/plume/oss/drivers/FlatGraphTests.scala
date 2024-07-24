package com.github.plume.oss.drivers

import com.github.plume.oss.testfixtures.PlumeDriverFixture
import com.github.plume.oss.testfixtures.PlumeDriverFixture.{b1, m1}
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import overflowdb.BatchedUpdate

import java.io.File as JFile
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.util.{Failure, Try}

class FlatGraphTests extends PlumeDriverFixture(new FlatGraphDriver()) {

  private val methodSemantics     = JFile.createTempFile("method", ".semantics")
  private val methodSemanticsPath = Paths.get(methodSemantics.getAbsolutePath)

  Files.write(methodSemanticsPath, "\"Foo.bar\" 1->-1\n".getBytes(StandardCharsets.UTF_8))

}
