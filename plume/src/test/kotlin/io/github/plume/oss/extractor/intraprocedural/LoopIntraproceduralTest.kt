package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes.IF
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CFG
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.ControlStructure
import io.shiftleft.codepropertygraph.generated.nodes.JumpTarget
import io.shiftleft.codepropertygraph.generated.nodes.Local
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import overflowdb.Graph
import java.io.File
import java.io.IOException

class LoopIntraproceduralTest {

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "intraprocedural/loop"

        init {
            val testFileUrl = LoopIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
        }

        @AfterAll
        fun tearDownAll() {
            driver.close()
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
    fun loop1Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.plus }.let { assertEquals(1, it.size) }
        assertEquals(2, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<Call>().filter { it.name() == Operators.lessThan }.let { assertNotNull(it) }
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

    @Test
    fun loop2Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.plus }.let { assertEquals(1, it.size) }
        assertEquals(2, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<Call>().filter { it.name() == Operators.lessThan }.let { assertNotNull(it) }
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

    @Test
    fun loop3Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.plus }.let { assertEquals(1, it.size) }
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        ns.filterIsInstance<Call>().filter { it.name() == Operators.greaterEqualsThan }.let { assertNotNull(it); assertEquals(2, it.size) }
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.let { csv ->
            csv.forEach { ifVert ->
                assertNotNull(ifVert)
                assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
                g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                    assertEquals(2, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                    assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
                }
            }
        }
    }

    @Test
    fun loop4Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(1, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(4, ns.filterIsInstance<JumpTarget>().size)
        calls.filter { it.name() == Operators.lessThan }.let { assertNotNull(it); assertEquals(2, it.size) }
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.toList().let { csv ->
            csv.forEach { ifVert ->
                assertNotNull(ifVert)
                assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
                g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                    assertEquals(2, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                    assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
                }
            }
        }
    }

    @Test
    fun loop5Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(1, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        calls.filter { it.name() == Operators.lessThan }.let { assertNotNull(it); assertEquals(1, it.size) }
        calls.filter { it.name() == Operators.greaterEqualsThan }.let { assertNotNull(it); assertEquals(1, it.size) }
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.toList().let { csv ->
            csv.forEach { ifVert ->
                assertNotNull(ifVert)
                assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
                g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                    assertEquals(2, it.size)
                    assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                    assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
                }
            }
        }
    }

    @Test
    fun loop6Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(2, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(2, ns.filterIsInstance<JumpTarget>().toList().size)
        assertNotNull(calls.filter { it.name() == Operators.lessThan })
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.toList().let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

    @Test
    fun loop7Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(4, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        assertNotNull(calls.filter { it.name() == Operators.lessThan })
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

    @Test
    fun loop8Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(4, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        assertNotNull(calls.filter { it.name() == Operators.lessThan })
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.toList().let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

    @Test
    fun loop9Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(4, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        assertNotNull(calls.filter { it.name() == Operators.lessThan })
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.toList().let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

    @Test
    fun loop10Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(2, calls.filter { it.name() == Operators.plus }.size)
        assertEquals(4, ns.filterIsInstance<JumpTarget>().toList().size)
        assertNotNull(calls.filter { it.name() == Operators.lessThan })
        ns.filterIsInstance<ControlStructure>().filter { it.controlStructureType() == IF }.toList().let { csv ->
            val ifVert = csv.firstOrNull(); assertNotNull(ifVert); ifVert!!
            assertTrue(g.V(ifVert.id()).next().outE(CFG).hasNext())
            g.V(ifVert.id()).next().out(CFG).asSequence().filterIsInstance<JumpTarget>().toList().let {
                assertEquals(2, it.size)
                assertNotNull(it.find { jtv -> jtv.name() == "TRUE" })
                assertNotNull(it.find { jtv -> jtv.name() == "FALSE" })
            }
        }
    }

}