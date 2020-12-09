package za.ac.sun.plume.intraprocedural

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.vertices.CallVertex
import za.ac.sun.plume.domain.models.vertices.ControlStructureVertex
import za.ac.sun.plume.domain.models.vertices.JumpTargetVertex
import za.ac.sun.plume.domain.models.vertices.LocalVertex
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import java.io.File
import java.io.IOException

class ConditionalIntraproceduralTest {
    private lateinit var testResourcePath: String

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}conditional"

        init {
            val testFileUrl = ConditionalIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
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
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(2, it.size) }
        assertEquals(2, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
            graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name == "FALSE" })
            }
        }
    }

    @Test
    fun conditional2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(2, it.size) }
        assertEquals(2, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
            graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name == "FALSE" })
            }
        }
    }

    @Test
    fun conditional3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(2, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "MUL" }.let { assertEquals(1, it.size) }
        assertEquals(2, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(graph.edgesOut(ifVert).containsKey(EdgeLabel.CFG))
            graph.edgesOut(ifVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name == "FALSE" })
            }
        }
    }

    @Test
    fun conditional4Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(2, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "MUL" }.let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(2, csv.size)
        }
    }

    @Test
    fun conditional5Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "MUL" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "DIV" }.let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(3, csv.size)
        }
    }

    @Test
    fun conditional6Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "MUL" }.let { assertEquals(1, it.size) }
        assertEquals(2, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv -> assertEquals(1, csv.size) }
    }

    @Test
    fun conditional7Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(2, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "MUL" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "DIV" }.let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(3, csv.size)
        }
    }

    @Test
    fun conditional8Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "SUB" }.let { assertEquals(2, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "MUL" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "DIV" }.let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "EQ" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(3, csv.size)
        }
    }

    @Test
    fun conditional9Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertEquals(2, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(1, csv.size)
        }
    }

    @Test
    fun conditional10Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertEquals(4, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(2, csv.size)
        }
    }

    @Test
    fun conditional11Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertEquals(4, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<CallVertex>().filter { it.name == "GT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "LT" }.let { assertNotNull(it) }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.name == "IF" }.let { csv ->
            assertEquals(2, csv.size)
        }
    }
}