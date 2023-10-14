package com.github.plume.oss

import com.github.plume.oss.drivers.OverflowDbDriver
import io.shiftleft.codepropertygraph.generated.{Cpg, NodeTypes, PropertyNames}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.{Files, Paths}

class SourceSupportTests extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private def getTestResource(dir: String): File = {
    val resourceURL = getClass.getResource(dir)
    if (resourceURL == null) throw new NullPointerException("Unable to obtain test resource")
    new File(resourceURL.getFile)
  }

  private val foo: File = getTestResource("/source_support/Foo.java")
  private val bar: File = getTestResource("/source_support/Bar.java")

  private var driver           = new OverflowDbDriver()
  private var cpg: Option[Cpg] = None
  private val storage          = Some("./cpg_test.odb")
  private val sandboxDir: File = Files.createTempDirectory("plume").toFile

  override def beforeAll(): Unit = {
    Paths.get(storage.get).toFile.delete()
    driver.clear()
    driver.close()
    driver = new OverflowDbDriver(storage)
    cpg = Some(new Jimple2Cpg().createCpg(foo.getParent, driver = driver))
    sandboxDir.listFiles().foreach(_.delete())
  }

  override def afterAll(): Unit = {
    driver.clear()
    driver.close()
    Paths.get(storage.get).toFile.delete()
  }

  "should accept Java source files" in {
    val List(barM, fooM) = driver
      .propertyFromNodes(NodeTypes.TYPE_DECL, PropertyNames.NAME, PropertyNames.FULL_NAME, PropertyNames.IS_EXTERNAL)
      .filter(!_.getOrElse(PropertyNames.IS_EXTERNAL, true).toString.toBoolean)
      .sortWith { case (x, y) =>
        x(PropertyNames.FULL_NAME).toString < y(PropertyNames.FULL_NAME).toString
      }
    fooM.get(PropertyNames.FULL_NAME) shouldBe Some("Foo")
    barM.get(PropertyNames.FULL_NAME) shouldBe Some("Bar")
    fooM.get(PropertyNames.IS_EXTERNAL) shouldBe Some(false)
    barM.get(PropertyNames.IS_EXTERNAL) shouldBe Some(false)
  }

}
