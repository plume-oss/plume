package io.github.plume.oss.extractor.intraprocedural

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.vertices.*
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import java.io.File
import java.io.IOException

class BasicIntraproceduralTest {
    private lateinit var currentTestNumber: String

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}basic"

        init {
            val testFileUrl = BasicIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver)
        // Select test resource based on integer in method name
        currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun basic1Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NamespaceBlockVertex>().let { nbv ->
            assertNotNull(nbv.find { it.name == "basic" })
            assertNotNull(nbv.find { it.name == "intraprocedural" })
        }
        vertices.filterIsInstance<FileVertex>().find { it.name == "intraprocedural.basic.Basic$currentTestNumber" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "a" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "b" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "c" }.let { assertNotNull(it); assertEquals("int", it!!.typeFullName) }
        vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.let { assertNotNull(it) }
    }

    @Test
    fun basic2Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NamespaceBlockVertex>().let { nbv ->
            assertNotNull(nbv.find { it.name == "basic" })
            assertNotNull(nbv.find { it.name == "intraprocedural" })
        }
        vertices.filterIsInstance<FileVertex>().find { it.name == "intraprocedural.basic.Basic$currentTestNumber" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "a" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "b" }.let { assertNotNull(it); assertEquals("double", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "c" }.let { assertNotNull(it); assertEquals("double", it!!.typeFullName) }
        vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.let { assertNotNull(it) }
    }

    @Test
    fun basic3Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NamespaceBlockVertex>().let { nbv ->
            assertNotNull(nbv.find { it.name == "basic" })
            assertNotNull(nbv.find { it.name == "intraprocedural" })
        }
        vertices.filterIsInstance<FileVertex>().find { it.name == "intraprocedural.basic.Basic$currentTestNumber" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "a" }.let { assertNotNull(it); assertEquals("long", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "b" }.let { assertNotNull(it); assertEquals("short", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find {  it.name == "c" }.let { assertNotNull(it); assertEquals("long", it!!.typeFullName) }
        vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.let { assertNotNull(it) }
    }

    @Test
    fun basic4Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<NamespaceBlockVertex>().let { nbv ->
            assertNotNull(nbv.find { it.name == "basic" })
            assertNotNull(nbv.find { it.name == "intraprocedural" })
        }
        vertices.filterIsInstance<FileVertex>().find { it.name == "intraprocedural.basic.Basic$currentTestNumber" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "Sally" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "John" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "Dick" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "Nigel" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodReturnVertex>().let { mrv ->
            assertNotNull(mrv.find { it.typeFullName == "int" })
            assertNotNull(mrv.find { it.typeFullName == "int[]" })
            assertNotNull(mrv.find { it.typeFullName == "double" })
            assertNotNull(mrv.find { it.typeFullName == "boolean" })
        }
    }

    @Test
    fun basic5Test() {
        val extractor = Extractor(driver)
        val resourceDir = "${PATH.absolutePath}${File.separator}basic5${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()

        val vertices = graph.vertices()
        vertices.filterIsInstance<NamespaceBlockVertex>().let { nbv ->
            assertNotNull(nbv.find { it.name == "basic" })
            assertNotNull(nbv.find { it.name == "intraprocedural" })
            assertNotNull(nbv.find { it.name == "basic5" })
            assertEquals(3, nbv.size)
        }
        vertices.filterIsInstance<TypeDeclVertex>().let { mrv ->
            assertNotNull(mrv.find { it.fullName == "intraprocedural.basic.Basic5" })
            assertNotNull(mrv.find { it.fullName == "intraprocedural.basic.basic5.Basic5" })
            assertEquals(2, mrv.size)
        }
        vertices.filterIsInstance<MethodVertex>().let { mv ->
            assertNotNull(mv.find { it.fullName == "intraprocedural.basic.Basic5.main" })
            assertNotNull(mv.find { it.fullName == "intraprocedural.basic.basic5.Basic5.main" })
            assertNotNull(mv.find { it.fullName == "intraprocedural.basic.Basic5.<init>" })
            assertNotNull(mv.find { it.fullName == "intraprocedural.basic.basic5.Basic5.<init>" })
            assertEquals(4, mv.size)
        }
    }
}