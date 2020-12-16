package io.github.plume.oss.extractor.interprocedural

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.vertices.CallVertex
import io.github.plume.oss.domain.models.vertices.MethodVertex
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.options.ExtractorOptions
import java.io.File

class InheritanceInterproceduralTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "interprocedural${File.separator}inheritance"

        init {
            val testFileUrl = InheritanceInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun testNoneInheritance() {
        val extractor = Extractor(driver)
        ExtractorOptions.callGraphAlg = ExtractorOptions.CallGraphAlg.NONE
        // Load test resource and project + export graph
        val f = File(CLS_PATH.absolutePath + File.separator + TEST_PATH)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
        // Check calls
        val vertices = graph.vertices()
        vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Base.<init>" && it.signature == "void <init>()" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Derived.<init>" && it.signature == "void <init>()" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Base.show" && it.signature == "void show()" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Derived.show" && it.signature == "void show()" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Base: void <init>()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertFalse(graph.edgesOut(it).containsKey(EdgeLabel.CALL)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Derived: void <init>()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertFalse(graph.edgesOut(it).containsKey(EdgeLabel.CALL)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Base: void show()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertFalse(graph.edgesOut(it).containsKey(EdgeLabel.CALL)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Derived: void show()" }
                .apply { assertEquals(1, this.size) }
                .forEach { assertFalse(graph.edgesOut(it).containsKey(EdgeLabel.CALL)) }
    }

    @Test
    fun testCHAInheritance() {
        val extractor = Extractor(driver)
        ExtractorOptions.callGraphAlg = ExtractorOptions.CallGraphAlg.CHA
        // Load test resource and project + export graph
        val f = File(CLS_PATH.absolutePath + File.separator + TEST_PATH)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
        // Check calls
        val vertices = graph.vertices()
        val baseInit = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Base.<init>" && it.signature == "void <init>()" }.apply { assertNotNull(this) }
        val derivedInit = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Derived.<init>" && it.signature == "void <init>()" }.apply { assertNotNull(this) }
        val baseShow = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Base.show" && it.signature == "void show()" }.apply { assertNotNull(this) }
        val derivedShow = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Derived.show" && it.signature == "void show()" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Base: void <init>()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(baseInit)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Derived: void <init>()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedInit)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Base: void show()" }
                .apply { assertEquals(2, this.size) }
                .forEach {
                    if (graph.edgesOut(it)[EdgeLabel.CALL]!!.size > 1) {
                        assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedShow))
                        assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(baseShow))
                    } else {
                        assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedShow))
                    }
                }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Derived: void show()" }
                .apply { assertEquals(1, this.size) }
                .forEach { assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedShow)) }
    }

    @Test
    fun testSPARKInheritance() {
        val extractor = Extractor(driver)
        ExtractorOptions.callGraphAlg = ExtractorOptions.CallGraphAlg.SPARK
        // Load test resource and project + export graph
        val f = File(CLS_PATH.absolutePath + File.separator + TEST_PATH)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
        // Check calls
        val vertices = graph.vertices()
        val baseInit = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Base.<init>" && it.signature == "void <init>()" }.apply { assertNotNull(this) }
        val derivedInit = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Derived.<init>" && it.signature == "void <init>()" }.apply { assertNotNull(this) }
        val baseShow = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Base.show" && it.signature == "void show()" }.apply { assertNotNull(this) }
        val derivedShow = vertices.filterIsInstance<MethodVertex>().first { it.fullName == "interprocedural.inheritance.Derived.show" && it.signature == "void show()" }.apply { assertNotNull(this) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Base: void <init>()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(baseInit)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Derived: void <init>()" }
                .apply { assertEquals(2, this.size) }
                .forEach { assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedInit)) }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Base: void show()" }
                .apply { assertEquals(2, this.size) }
                .let { cvs ->
                    assertTrue(cvs.any { graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedShow) })
                    assertTrue(cvs.any { graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(baseShow) })
                }
        vertices.filterIsInstance<CallVertex>().filter { it.methodFullName == "interprocedural.inheritance.Derived: void show()" }
                .apply { assertEquals(1, this.size) }
                .forEach { assertTrue(graph.edgesOut(it)[EdgeLabel.CALL]!!.contains(derivedShow)) }
    }
}