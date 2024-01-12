package com.github.plume.oss

import com.github.plume.oss.drivers.OverflowDbDriver
import io.joern.dataflowengineoss.queryengine.QueryEngineStatistics
import io.shiftleft.codepropertygraph.generated.{Cpg, NodeTypes, Operators, PropertyNames}
import io.shiftleft.semanticcpg.language._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Paths}
import scala.util.Using

/** Used to make sure that incremental change detection works correctly.
  */
class DiffTests extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private val logger = LoggerFactory.getLogger(classOf[DiffTests])

  private def getTestResource(dir: String): File = {
    val resourceURL = getClass.getResource(dir)
    if (resourceURL == null) throw new NullPointerException("Unable to obtain test resource")
    new File(resourceURL.getFile)
  }

  private def rewriteFileContents(tgt: File, incoming: File): File = {
    if (!tgt.exists()) tgt.createNewFile()
    Using.resources(new FileOutputStream(tgt, false), new FileInputStream(incoming)) { case (fos, fis) =>
      val buf = new Array[Byte](4096)
      Iterator
        .continually(fis.read(buf))
        .takeWhile(_ != -1)
        .foreach(fos.write(buf, 0, _))
    }
    new File(tgt.getAbsolutePath)
  }

  private val foo1: File       = getTestResource("/diff/Foo1.java")
  private val foo2: File       = getTestResource("/diff/Foo2.java")
  private val bar1: File       = getTestResource("/diff/Bar1.java")
  private val sandboxDir: File = Files.createTempDirectory("plume").toFile
  private val foo              = new File(s"${sandboxDir.getAbsolutePath}${File.separator}Foo.java")
  private val bar              = new File(s"${sandboxDir.getAbsolutePath}${File.separator}Bar.java")
  private var driver           = new OverflowDbDriver()
  private var cpg: Option[Cpg] = None
  private val storage          = Some("./cpg_test.odb")

  override def beforeAll(): Unit = {
    rewriteFileContents(foo, foo1)
    rewriteFileContents(bar, bar1)
    JavaCompiler.compileJava(foo, bar)
    driver.clear()
    driver.close()
    driver = new OverflowDbDriver(storage)
    cpg = Some(new Jimple2Cpg().createCpg(sandboxDir.getAbsolutePath, driver = driver, parseJavaSource = false))
    sandboxDir.listFiles().foreach(_.delete())
  }

  override def afterAll(): Unit = {
    driver.clear()
    driver.close()
    Paths.get(storage.get).toFile.delete()
  }

  "should have all necessary nodes on first pass" in {
    driver.propertyFromNodes(NodeTypes.TYPE_DECL).size shouldBe 7
    driver.propertyFromNodes(NodeTypes.METHOD).size shouldBe 7
    driver.propertyFromNodes(NodeTypes.TYPE, PropertyNames.FULL_NAME).size shouldBe 7
    driver.propertyFromNodes(NodeTypes.NAMESPACE_BLOCK, PropertyNames.FULL_NAME).size shouldBe 3
    driver
      .propertyFromNodes(NodeTypes.METHOD_PARAMETER_IN, PropertyNames.FULL_NAME)
      .size shouldBe 10
    driver.propertyFromNodes(NodeTypes.LOCAL, PropertyNames.FULL_NAME).size shouldBe 2

    val List(barM, fooM) = driver
      .propertyFromNodes(NodeTypes.TYPE_DECL, PropertyNames.FULL_NAME, PropertyNames.IS_EXTERNAL)
      .filter(!_.getOrElse(PropertyNames.IS_EXTERNAL, true).toString.toBoolean)
      .sortWith { case (x, y) =>
        x(PropertyNames.FULL_NAME).toString < y(PropertyNames.FULL_NAME).toString
      }
    fooM.get(PropertyNames.FULL_NAME) shouldBe Some("diff.Foo")
    barM.get(PropertyNames.FULL_NAME) shouldBe Some("diff.Bar")
    val List(lb) = driver
      .propertyFromNodes(NodeTypes.LITERAL, PropertyNames.CODE)
    lb.get(PropertyNames.CODE) shouldBe Some("1")
    val paramA = driver
      .propertyFromNodes(NodeTypes.METHOD_PARAMETER_IN, PropertyNames.CODE)
      .filter(_(PropertyNames.CODE) == "int a")
    paramA.size shouldBe 1
  }

  "should update Foo on file changes" in {
    rewriteFileContents(bar, bar1)
    rewriteFileContents(foo, foo2)
    JavaCompiler.compileJava(foo, bar)
    cpg = Some(new Jimple2Cpg().createCpg(sandboxDir.getAbsolutePath, driver = driver, parseJavaSource = false))

    // Check the correct numbers of nodes are present
    driver.propertyFromNodes(NodeTypes.TYPE_DECL).size shouldBe 7
    driver.propertyFromNodes(NodeTypes.METHOD).size shouldBe 7
    driver.propertyFromNodes(NodeTypes.TYPE, PropertyNames.FULL_NAME).size shouldBe 7
    driver.propertyFromNodes(NodeTypes.NAMESPACE_BLOCK, PropertyNames.FULL_NAME).size shouldBe 3
    driver
      .propertyFromNodes(NodeTypes.METHOD_PARAMETER_IN, PropertyNames.FULL_NAME)
      .size shouldBe 10
    driver.propertyFromNodes(NodeTypes.LOCAL, PropertyNames.FULL_NAME).size shouldBe 2

    val List(barM, fooM) = driver
      .propertyFromNodes(NodeTypes.TYPE_DECL, PropertyNames.FULL_NAME, PropertyNames.IS_EXTERNAL)
      .filter(!_.getOrElse(PropertyNames.IS_EXTERNAL, true).toString.toBoolean)
      .sortWith { case (x, y) =>
        x(PropertyNames.FULL_NAME).toString < y(PropertyNames.FULL_NAME).toString
      }
    fooM.get(PropertyNames.FULL_NAME) shouldBe Some("diff.Foo")
    barM.get(PropertyNames.FULL_NAME) shouldBe Some("diff.Bar")
    val List(lb) = driver
      .propertyFromNodes(NodeTypes.LITERAL, PropertyNames.CODE)
    lb.get(PropertyNames.CODE) shouldBe Some("3")
    val paramA = driver
      .propertyFromNodes(NodeTypes.METHOD_PARAMETER_IN, PropertyNames.CODE)
      .filter(_(PropertyNames.CODE) == "int a")
    paramA.size shouldBe 1
  }

}
