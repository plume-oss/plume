package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes.SWITCH
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CFG
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CONDITION
import io.shiftleft.codepropertygraph.generated.nodes.ControlStructure
import io.shiftleft.codepropertygraph.generated.nodes.Identifier
import io.shiftleft.codepropertygraph.generated.nodes.JumpTarget
import io.shiftleft.codepropertygraph.generated.nodes.Local
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import overflowdb.Graph
import java.io.File
import java.io.IOException

class SwitchIntraproceduralTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "intraprocedural/switches"

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
        extractor.load(f).project()
        g = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        LocalCache.clear()
        driver.close()
        g.close()
    }

    @Test
    fun switch1Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "i" })
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 0" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 2" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 3" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "DEFAULT" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == SWITCH }
            .let { csv ->
                val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
                assertTrue(g.V(switchVert.id()).next().outE(CONDITION).hasNext())
                g.V(switchVert.id()).next().out(CONDITION).asSequence().filterIsInstance<Identifier>().toList().let {
                    assertEquals(1, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "i" })
                }
                assertEquals(
                    4,
                    g.V(switchVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().toList().size
                )
            }
    }

    @Test
    fun switch2Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "animal" })
        assertNotNull(ns.find { it is Local && it.name() == "result" })
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 0" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 1" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 2" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "DEFAULT" }
            .let { assertEquals(2, it.size) }
        assertEquals(14, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == SWITCH && it.order() == 17}
            .let { csv ->
                val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
                assertTrue(g.V(switchVert.id()).next().outE(CONDITION).hasNext())
                g.V(switchVert.id()).next().out(CONDITION).asSequence().filterIsInstance<Identifier>().toList().let {
                    assertEquals(1, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "l4" })
                }
                assertEquals(
                    4,
                    g.V(switchVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().toList().size
                )
            }
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == SWITCH && it.order() == 5 }
            .let { csv ->
                val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
                assertTrue(g.V(switchVert.id()).next().outE(CONDITION).hasNext())
                g.V(switchVert.id()).next().out(CONDITION).asSequence().filterIsInstance<Identifier>().toList().let {
                    assertEquals(1, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "\$stack5" })
                }
                assertEquals(
                    4,
                    g.V(switchVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().toList().size
                )
            }
    }

    @Test
    fun switch3Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "i" })
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 0" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 2" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 3" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "DEFAULT" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == SWITCH }
            .let { csv ->
                val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
                assertTrue(g.V(switchVert.id()).next().outE(CONDITION).hasNext())
                g.V(switchVert.id()).next().out(CONDITION).asSequence().filterIsInstance<Identifier>().toList().let {
                    assertEquals(1, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "i" })
                }
                assertEquals(
                    4,
                    g.V(switchVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().toList().size
                )
            }
    }

    @Test
    fun switch4Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "i" })
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 101" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 105" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 111" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 117" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "DEFAULT" }
            .let { assertEquals(1, it.size) }
        assertEquals(6, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == SWITCH }
            .let { csv ->
                val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
                assertTrue(g.V(switchVert.id()).next().outE(CONDITION).hasNext())
                g.V(switchVert.id()).next().out(CONDITION).asSequence().filterIsInstance<Identifier>().toList().let {
                    assertEquals(1, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "i" })
                }
                assertEquals(
                    6,
                    g.V(switchVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().toList().size
                )
            }
    }

    @Test
    fun switch5Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "i" })
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 0" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 1" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "CASE 2" }
            .let { assertEquals(1, it.size) }
        ns.filterIsInstance<JumpTarget>().filter { it.name() == "DEFAULT" }
            .let { assertEquals(1, it.size) }
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == SWITCH }
            .let { csv ->
                val switchVert = csv.firstOrNull(); assertNotNull(switchVert); switchVert!!
                assertTrue(g.V(switchVert.id()).next().outE(CONDITION).hasNext())
                g.V(switchVert.id()).next().out(CONDITION).asSequence().filterIsInstance<Identifier>().toList().let {
                    assertEquals(1, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "\$stack5" })
                }
                assertEquals(
                    4,
                    g.V(switchVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().toList().size
                )
            }
    }

}