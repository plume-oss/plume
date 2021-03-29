package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes.IF
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes.WHILE
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
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
        private const val TEST_PATH = "intraprocedural/loop"

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
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }.let { assertEquals(1, it.size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { gte ->
            assertNotNull(gte); gte!!
            assertTrue(g.V(gte.id()).hasNext())
            assertTrue(g.V(gte.id()).next().outE(CFG).hasNext())
            g.V(gte.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(gte.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun loop2Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }.let { assertEquals(1, it.size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessThan }.let { le ->
            assertNotNull(le); le!!
            assertTrue(g.V(le.id()).hasNext())
            assertTrue(g.V(le.id()).next().outE(CFG).hasNext())
            g.V(le.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(le.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun loop3Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }.let { assertEquals(1, it.size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.lessThan }.let { les ->
            assertEquals(2, les.size)
            les.forEach { le ->
                assertTrue(g.V(le.id()).hasNext())
                assertTrue(g.V(le.id()).next().outE(CFG).hasNext())
                g.V(le.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
                assertNotNull(g.V(le.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                    .firstOrNull { it.controlStructureType() == IF })
            }
        }
    }

    @Test
    fun loop4Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(2, calls.filter { it.name() == Operators.addition }.size)
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { get ->
            assertNotNull(get); get!!
            assertTrue(g.V(get.id()).hasNext())
            assertTrue(g.V(get.id()).next().outE(CFG).hasNext())
            g.V(get.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(get.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun loop5Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(4, calls.filter { it.name() == Operators.addition }.size)
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { get ->
            assertNotNull(get); get!!
            assertTrue(g.V(get.id()).hasNext())
            assertTrue(g.V(get.id()).next().outE(CFG).hasNext())
            g.V(get.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(get.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessThan }.let { lt ->
            assertNotNull(lt); lt!!
            assertTrue(g.V(lt.id()).hasNext())
            assertTrue(g.V(lt.id()).next().outE(CFG).hasNext())
            g.V(lt.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(lt.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun loop6Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertEquals(2, calls.filter { it.name() == Operators.addition }.size)
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { get ->
            assertNotNull(get); get!!
            assertTrue(g.V(get.id()).hasNext())
            assertTrue(g.V(get.id()).next().outE(CFG).hasNext())
            g.V(get.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(get.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.notEquals }.let { nq ->
            assertNotNull(nq); nq!!
            assertTrue(g.V(nq.id()).hasNext())
            assertTrue(g.V(nq.id()).next().outE(CFG).hasNext())
            g.V(nq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(nq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

}