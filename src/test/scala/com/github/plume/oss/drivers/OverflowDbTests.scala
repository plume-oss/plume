package com.github.plume.oss.drivers

import com.github.plume.oss.testfixtures.PlumeDriverFixture

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

}
