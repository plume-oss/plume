package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.nodes.Call
import io.shiftleft.codepropertygraph.generated.nodes.Local
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import overflowdb.Graph
import java.io.File
import java.io.IOException

class ArithmeticTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var g: Graph
        private var PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}arithmetic"

        init {
            val testFileUrl = ArithmeticTest::class.java.classLoader.getResource(TEST_PATH)
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
        val resourceDir = "${PATH.absolutePath}${File.separator}Arithmetic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        g = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun arithmetic1Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        assertNotNull(ns.find { it is Local && it.name() == "e" })
        assertNotNull(ns.find { it is Local && it.name() == "f" })
        val add = ns.filterIsInstance<Call>().find { it.name() == "ADD" }.apply { assertNotNull(this) }
        val mul = ns.filterIsInstance<Call>().find { it.name() == "MUL" }.apply { assertNotNull(this) }
        val sub = ns.filterIsInstance<Call>().find { it.name() == "SUB" }.apply { assertNotNull(this) }
        val div = ns.filterIsInstance<Call>().find { it.name() == "DIV" }.apply { assertNotNull(this) }
        add!!; mul!!; sub!!; div!!
        assertTrue(add.id() < sub.id())
        assertTrue(sub.id() < mul.id())
        assertTrue(mul.id() < div.id())
    }

    @Test
    fun arithmetic2Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        val add = ns.filterIsInstance<Call>().find { it.name() == "ADD" }.apply { assertNotNull(this) }
        val mul = ns.filterIsInstance<Call>().find { it.name() == "MUL" }.apply { assertNotNull(this) }
        add!!; mul!!
        assertTrue(mul.id() < add.id())
    }

    @Test
    fun arithmetic3Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        val add = ns.filterIsInstance<Call>().find { it.name() == "ADD" }.apply { assertNotNull(this) }
        val mul = ns.filterIsInstance<Call>().find { it.name() == "MUL" }.apply { assertNotNull(this) }
        val sub = ns.filterIsInstance<Call>().find { it.name() == "SUB" }.apply { assertNotNull(this) }
        add!!; mul!!; sub!!
        assertTrue(mul.id() < add.id())
        assertTrue(add.id() < sub.id())
    }

    @Test
    fun arithmetic4Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        val addList = ns.filterIsInstance<Call>().filter { it.name() == "ADD" }
            .apply { assertFalse(this.toList().isNullOrEmpty()) }
        assertEquals(2, addList.toList().size)
    }

    @Test
    fun arithmetic5Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        assertNotNull(ns.find { it is Local && it.name() == "e" })
        assertNotNull(ns.find { it is Local && it.name() == "f" })
        assertNotNull(ns.find { it is Local && it.name() == "g" })
        val and = ns.filterIsInstance<Call>().find { it.name() == "AND" }.apply { assertNotNull(this) }
        val or = ns.filterIsInstance<Call>().find { it.name() == "OR" }.apply { assertNotNull(this) }
        val shl = ns.filterIsInstance<Call>().find { it.name() == "SHL" }.apply { assertNotNull(this) }
        val shr = ns.filterIsInstance<Call>().find { it.name() == "SHR" }.apply { assertNotNull(this) }
        val rem = ns.filterIsInstance<Call>().find { it.name() == "REM" }.apply { assertNotNull(this) }
        and!!; or!!; shl!!; shr!!; rem!!
        assertTrue(and.id() < or.id())
        assertTrue(or.id() < shl.id())
        assertTrue(shl.id() < shr.id())
        assertTrue(shr.id() < rem.id())
    }

    @Test
    fun arithmetic6Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        val xor = ns.filterIsInstance<Call>().find { it.name() == "XOR" }.apply { assertNotNull(this) }
        val ushr = ns.filterIsInstance<Call>().find { it.name() == "USHR" }.apply { assertNotNull(this) }
        xor!!; ushr!!
        assertTrue(xor.id() < ushr.id())
    }

    @Test
    fun arithmetic7Test() {
        val ns = g.nodes().asSequence()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        val addList = ns.filterIsInstance<Call>().filter { it.name() == "ADD" }.apply {
            assertFalse(this.toList().isNullOrEmpty())
        }
        assertEquals(4, addList.toList().size)
    }
}