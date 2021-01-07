package io.github.plume.oss.extractor.intraprocedural

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.domain.models.vertices.*
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import java.io.File
import java.io.IOException

class SwitchIntraproceduralTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}switches"

        init {
            val testFileUrl = SwitchIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
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
        val testResourcePath = "${PATH.absolutePath}${File.separator}Switch$currentTestNumber"
        val testSourceFile = "$testResourcePath.java"
        // Load test resource and project + export graph
        val f = File(testSourceFile)
        extractor.load(f)
        extractor.project()
        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun switch1Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "i" })
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 0" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 2" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 3" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "DEFAULT" }.let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.code == "TABLE_SWITCH" }.let { csv ->
            val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
            assertTrue(graph.edgesOut(switchVert).containsKey(EdgeLabel.CONDITION))
            graph.edgesOut(switchVert)[EdgeLabel.CONDITION]!!.filterIsInstance<IdentifierVertex>().let {
                assertEquals(1, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "i" })
            }
            assertEquals(4, graph.edgesOut(switchVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().size)
        }
    }

    @Test
    fun switch2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "animal" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "result" })
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 0" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 1" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 2" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "DEFAULT" }.let { assertEquals(2, it.size) }
        assertEquals(14, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.code == "TABLE_SWITCH" }.let { csv ->
            val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
            assertTrue(graph.edgesOut(switchVert).containsKey(EdgeLabel.CONDITION))
            graph.edgesOut(switchVert)[EdgeLabel.CONDITION]!!.filterIsInstance<IdentifierVertex>().let {
                assertEquals(1, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "l4" })
            }
            assertEquals(4, graph.edgesOut(switchVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().size)
        }
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.code == "LOOKUP_SWITCH" }.let { csv ->
            val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
            assertTrue(graph.edgesOut(switchVert).containsKey(EdgeLabel.CONDITION))
            graph.edgesOut(switchVert)[EdgeLabel.CONDITION]!!.filterIsInstance<IdentifierVertex>().let {
                assertEquals(1, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "\$stack5" })
            }
            assertEquals(4, graph.edgesOut(switchVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().size)
        }
    }

    @Test
    fun switch3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "i" })
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 0" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 2" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 3" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "DEFAULT" }.let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.code == "TABLE_SWITCH" }.let { csv ->
            val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
            assertTrue(graph.edgesOut(switchVert).containsKey(EdgeLabel.CONDITION))
            graph.edgesOut(switchVert)[EdgeLabel.CONDITION]!!.filterIsInstance<IdentifierVertex>().let {
                assertEquals(1, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "i" })
            }
            assertEquals(4, graph.edgesOut(switchVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().size)
        }
    }

    @Test
    fun switch4Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "i" })
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 101" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 105" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 111" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 117" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "DEFAULT" }.let { assertEquals(1, it.size) }
        assertEquals(6, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.code == "LOOKUP_SWITCH" }.let { csv ->
            val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
            assertTrue(graph.edgesOut(switchVert).containsKey(EdgeLabel.CONDITION))
            graph.edgesOut(switchVert)[EdgeLabel.CONDITION]!!.filterIsInstance<IdentifierVertex>().let {
                assertEquals(1, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "i" })
            }
            assertEquals(6, graph.edgesOut(switchVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().size)
        }
    }

    @Test
    fun switch5Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "i" })
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 0" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 1" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "CASE 2" }.let { assertEquals(1, it.size) }
        vertices.filterIsInstance<JumpTargetVertex>().filter { it.name == "DEFAULT" }.let { assertEquals(1, it.size) }
        assertEquals(4, vertices.filterIsInstance<JumpTargetVertex>().size)
        vertices.filterIsInstance<ControlStructureVertex>().filter { it.code == "TABLE_SWITCH" }.let { csv ->
            val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
            assertTrue(graph.edgesOut(switchVert).containsKey(EdgeLabel.CONDITION))
            graph.edgesOut(switchVert)[EdgeLabel.CONDITION]!!.filterIsInstance<IdentifierVertex>().let {
                assertEquals(1, it.size)
                assertNotNull(it.find { jtv -> jtv.name == "\$stack5" })
            }
            assertEquals(4, graph.edgesOut(switchVert)[EdgeLabel.CFG]!!.filterIsInstance<JumpTargetVertex>().size)
        }
    }

}