package io.github.plume.oss.extractor.interprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.graphio.GraphMLWriter
import io.shiftleft.codepropertygraph.generated.nodes.NewBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewLiteralBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewLocalBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.FileWriter
import java.io.IOException

class ExceptionInterproceduralTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private val TEST_PATH = "interprocedural${File.separator}exception"

        init {
            val testFileUrl = ExceptionInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo.displayName.replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Exception$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        graph = driver.getMethod(
            "intraprocedural.exception.Exception$currentTestNumber.main",
            "void main(java.lang.String[])",
            includeBody = true
        )
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun exception1Test() {
        val vertices = graph.vertices()
        val localV = vertices.filterIsInstance<NewLocalBuilder>()
        val mtdV = vertices.filterIsInstance<NewBlockBuilder>().firstOrNull()?.apply { assertNotNull(this) }
        assertNotNull(localV.firstOrNull {
            it.build().name() == "e" && it.build().typeFullName() == "java.lang.Exception"
        })
        assertNotNull(localV.firstOrNull { it.build().name() == "a" && it.build().typeFullName() == "int" })
        assertNotNull(localV.firstOrNull {
            it.build().name() == "\$stack4" && it.build().typeFullName() == "java.lang.Exception"
        })
        assertNotNull(localV.firstOrNull { it.build().name() == "e#3" && it.build().typeFullName() == "int" })
        assertEquals(2, graph.edgesOut(mtdV!!)[EdgeLabel.CFG]?.size)

        val parseIntCall = vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "parseInt" }
            .apply { assertEquals(1, this.size) }.firstOrNull().apply { assertNotNull(this) }
        parseIntCall!!
        graph.edgesOut(parseIntCall)[EdgeLabel.AST]?.filterIsInstance<NewLiteralBuilder>()?.firstOrNull()
            ?.let { assertEquals("\"2\"", it.build().code()) }
    }

    @Test
    fun exception2Test() {
        val vertices = graph.vertices()
        val localV = vertices.filterIsInstance<NewLocalBuilder>()

        val mtdV = vertices.filterIsInstance<NewBlockBuilder>().firstOrNull()?.apply { assertNotNull(this) }
        assertNotNull(localV.firstOrNull {
            it.build().name() == "e" && it.build().typeFullName() == "java.lang.Exception"
        })
        assertNotNull(localV.firstOrNull { it.build().name() == "a" && it.build().typeFullName() == "int" })
        assertNotNull(localV.firstOrNull {
            it.build().name() == "\$stack5" && it.build().typeFullName() == "java.lang.Exception"
        })
        assertNotNull(localV.firstOrNull { it.build().name() == "e#3" && it.build().typeFullName() == "int" })
        assertNotNull(localV.firstOrNull { it.build().name() == "b" && it.build().typeFullName() == "byte" })
        assertEquals(2, graph.edgesOut(mtdV!!)[EdgeLabel.CFG]?.size)

        val parseIntCall = vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "parseInt" }
            .apply { assertEquals(1, this.size) }.firstOrNull().apply { assertNotNull(this) }
        parseIntCall!!
        graph.edgesOut(parseIntCall)[EdgeLabel.AST]?.filterIsInstance<NewLiteralBuilder>()?.firstOrNull()
            ?.let { assertEquals("\"2\"", it.build().code()) }
    }

}