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

class LoopIntraproceduralTest {

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}loop"

        init {
            val testFileUrl = LoopIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
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
        val resourceDir = "${PATH.absolutePath}${File.separator}Loop$currentTestNumber.java"
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
    fun loop1Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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
    fun loop2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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
    fun loop3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GTE" }
            .let { assertNotNull(it); assertEquals(2, it.size) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            csv.forEach { ifVert ->
                assertNotNull(ifVert)
                assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
                graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<NewJumpTargetBuilder>().let {
                    assertEquals(2, it.size)
                    assertNotNull(it.find { jtv -> jtv.build().name() == "TRUE" })
                    assertNotNull(it.find { jtv -> jtv.build().name() == "FALSE" })
                }
            }
        }
    }

    @Test
    fun loop4Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }
            .let { assertNotNull(it); assertEquals(2, it.size) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            csv.forEach { ifVert ->
                assertNotNull(ifVert)
                assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
                graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<NewJumpTargetBuilder>().let {
                    assertEquals(2, it.size)
                    assertNotNull(it.find { jtv -> jtv.build().name() == "TRUE" })
                    assertNotNull(it.find { jtv -> jtv.build().name() == "FALSE" })
                }
            }
        }
    }

    @Test
    fun loop5Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }
            .let { assertNotNull(it); assertEquals(1, it.size) }
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "GTE" }
            .let { assertNotNull(it); assertEquals(1, it.size) }
        vertices.filterIsInstance<NewControlStructureBuilder>().filter { it.build().code() == "IF" }.let { csv ->
            csv.forEach { ifVert ->
                assertNotNull(ifVert)
                assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
                graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<NewJumpTargetBuilder>().let {
                    assertEquals(2, it.size)
                    assertNotNull(it.find { jtv -> jtv.build().name() == "TRUE" })
                    assertNotNull(it.find { jtv -> jtv.build().name() == "FALSE" })
                }
            }
        }
    }

    @Test
    fun loop6Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(2, it.size) }
        assertEquals(2, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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
    fun loop7Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(4, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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
    fun loop8Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(4, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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
    fun loop9Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(4, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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
    fun loop10Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .let { assertEquals(2, it.size) }
        assertEquals(4, vertices.filterIsInstance<NewJumpTargetBuilder>().size)
        vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "LT" }.let { assertNotNull(it) }
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

}