package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewControlStructureBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewJumpTargetBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewLocalBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.IOException

class ConditionalIntraproceduralTest {
    private lateinit var testResourcePath: String

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}conditional"

        init {
            val testFileUrl = ConditionalIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val extractor = Extractor(driver)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo
            .displayName
            .replace("[^0-9]".toRegex(), "")
        testResourcePath = "${PATH.absolutePath}${File.separator}Conditional$currentTestNumber"
        val testSourceFile = "$testResourcePath.java"
        // Load test resource and project + export graph
        val f = File(testSourceFile)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun conditional1Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(2, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
            graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<NewJumpTargetBuilder>().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.build().name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.build().name() == "FALSE" })
            }
        }
    }

    @Test
    fun conditional2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(2, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
            graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<NewJumpTargetBuilder>().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.build().name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.build().name() == "FALSE" })
            }
        }
    }

    @Test
    fun conditional3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(2, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "MUL" }
            .let { assertEquals(1, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
            graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<NewJumpTargetBuilder>().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.build().name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.build().name() == "FALSE" })
            }
        }
    }

    @Test
    fun conditional4Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(2, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "MUL" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(2, csv.size)
        }
    }

    @Test
    fun conditional5Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "MUL" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "DIV" }
            .let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(3, csv.size)
        }
    }

    @Test
    fun conditional6Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "MUL" }
            .let { assertEquals(1, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }
            .let { csv -> assertEquals(1, csv.size) }
    }

    @Test
    fun conditional7Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(2, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "MUL" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "DIV" }
            .let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(3, csv.size)
        }
    }

    @Test
    fun conditional8Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "SUB" }
            .let { assertEquals(2, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "MUL" }
            .let { assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "DIV" }
            .let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(3, csv.size)
        }
    }

    @Test
    fun conditional9Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(1, csv.size)
        }
    }

    @Test
    fun conditional10Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(2, csv.size)
        }
    }

    @Test
    fun conditional11Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            assertEquals(2, csv.size)
        }
    }
}