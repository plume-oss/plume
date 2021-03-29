package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes.IF
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CFG
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.ControlStructure
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

class ConditionalIntraproceduralTest {
    private lateinit var testResourcePath: String

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private const val TEST_PATH = "intraprocedural/conditional"

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
    fun conditional1Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.subtraction }
            .let { assertEquals(2, it.toList().size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional2Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.subtraction }
            .let { assertEquals(2, it.toList().size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional3Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.subtraction }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.multiplication }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.notEquals }.let { neq ->
            assertNotNull(neq); neq!!
            assertTrue(g.V(neq.id()).hasNext())
            assertTrue(g.V(neq.id()).next().outE(CFG).hasNext())
            g.V(neq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(neq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional4Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.addition }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.subtraction }
            .let { assertEquals(2, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.multiplication }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.notEquals }.let { neq ->
            assertNotNull(neq); neq!!
            assertTrue(g.V(neq.id()).hasNext())
            assertTrue(g.V(neq.id()).next().outE(CFG).hasNext())
            g.V(neq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(neq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional5Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.subtraction }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.multiplication }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().filter { it.name() == Operators.division }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.notEquals }.let { neq ->
            assertNotNull(neq); neq!!
            assertTrue(g.V(neq.id()).hasNext())
            assertTrue(g.V(neq.id()).next().outE(CFG).hasNext())
            g.V(neq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(neq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { geq ->
            assertNotNull(geq); geq!!
            assertTrue(g.V(geq.id()).hasNext())
            assertTrue(g.V(geq.id()).next().outE(CFG).hasNext())
            g.V(geq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(geq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
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
    fun conditional6Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        ns.filterIsInstance<Call>().filter { it.name() == Operators.multiplication }
            .let { assertEquals(1, it.toList().size) }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.notEquals }.let { neq ->
            assertNotNull(neq); neq!!
            assertTrue(g.V(neq.id()).hasNext())
            assertTrue(g.V(neq.id()).next().outE(CFG).hasNext())
            g.V(neq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(neq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional7Test() {
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional8Test() {
        val ns = g.nodes().asSequence().toList().distinct()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { geq ->
            assertNotNull(geq); geq!!
            assertTrue(g.V(geq.id()).hasNext())
            assertTrue(g.V(geq.id()).next().outE(CFG).hasNext())
            g.V(geq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(geq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }

    @Test
    fun conditional9Test() {
        driver.exportGraph("/tmp/plume/c11.xml")
        val ns = g.nodes().asSequence().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.lessEqualsThan }.let { leq ->
            assertNotNull(leq); leq!!
            assertTrue(g.V(leq.id()).hasNext())
            assertTrue(g.V(leq.id()).next().outE(CFG).hasNext())
            g.V(leq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(leq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
        ns.filterIsInstance<Call>().firstOrNull { it.name() == Operators.greaterEqualsThan }.let { geq ->
            assertNotNull(geq); geq!!
            assertTrue(g.V(geq.id()).hasNext())
            assertTrue(g.V(geq.id()).next().outE(CFG).hasNext())
            g.V(geq.id()).next().out(CFG).asSequence().toList().size.let { assertEquals(2, it) }
            assertNotNull(g.V(geq.id()).next().`in`(AST).asSequence().filterIsInstance<ControlStructure>()
                .firstOrNull { it.controlStructureType() == IF })
        }
    }
}