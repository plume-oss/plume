package io.github.plume.oss.extractor.interprocedural

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.vertices.BlockVertex
import io.github.plume.oss.domain.models.vertices.CallVertex
import io.github.plume.oss.domain.models.vertices.LiteralVertex
import io.github.plume.oss.domain.models.vertices.LocalVertex
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import java.io.File
import java.io.IOException

class ExceptionInterproceduralTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "interprocedural${File.separator}exception"

        init {
            val testFileUrl = ExceptionInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver, CLS_PATH)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo.displayName.replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Exception$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        graph = driver.getMethod("interprocedural.exception.Exception$currentTestNumber.main", "void main(java.lang.String[])")
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun exception1Test() {
        val vertices = graph.vertices()
        val localV = vertices.filterIsInstance<LocalVertex>()

        val mtdV = vertices.filterIsInstance<BlockVertex>().firstOrNull()?.apply { assertNotNull(this) }
        assertNotNull(localV.firstOrNull { it.name == "e" && it.typeFullName == "java.lang.Exception" })
        assertNotNull(localV.firstOrNull { it.name == "a" && it.typeFullName == "int" })
        assertNotNull(localV.firstOrNull { it.name == "\$stack4" && it.typeFullName == "java.lang.Exception" })
        assertNotNull(localV.firstOrNull { it.name == "e#3" && it.typeFullName == "int" })
        assertEquals(2, graph.edgesOut(mtdV!!)[EdgeLabel.CFG]?.size)

        val parseIntCall = vertices.filterIsInstance<CallVertex>().filter { it.name == "parseInt" }
                .apply { assertEquals(1, this.size) }.firstOrNull().apply { assertNotNull(this) }
        parseIntCall!!
        graph.edgesOut(parseIntCall)[EdgeLabel.AST]?.filterIsInstance<LiteralVertex>()?.firstOrNull()?.let { assertEquals("\"2\"", it.code) }
    }

    @Test
    fun exception2Test() {
        val vertices = graph.vertices()
        val localV = vertices.filterIsInstance<LocalVertex>()

        val mtdV = vertices.filterIsInstance<BlockVertex>().firstOrNull()?.apply { assertNotNull(this) }
        assertNotNull(localV.firstOrNull { it.name == "e" && it.typeFullName == "java.lang.Exception" })
        assertNotNull(localV.firstOrNull { it.name == "a" && it.typeFullName == "int" })
        assertNotNull(localV.firstOrNull { it.name == "\$stack5" && it.typeFullName == "java.lang.Exception" })
        assertNotNull(localV.firstOrNull { it.name == "e#3" && it.typeFullName == "int" })
        assertNotNull(localV.firstOrNull { it.name == "b" && it.typeFullName == "byte" })
        assertEquals(2, graph.edgesOut(mtdV!!)[EdgeLabel.CFG]?.size)

        val parseIntCall = vertices.filterIsInstance<CallVertex>().filter { it.name == "parseInt" }
                .apply { assertEquals(1, this.size) }.firstOrNull().apply { assertNotNull(this) }
        parseIntCall!!
        graph.edgesOut(parseIntCall)[EdgeLabel.AST]?.filterIsInstance<LiteralVertex>()?.firstOrNull()?.let { assertEquals("\"2\"", it.code) }
    }

}