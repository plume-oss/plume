package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.github.plume.oss.store.LocalCache
import io.shiftleft.codepropertygraph.generated.Operators
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
        private val TEST_PATH = "intraprocedural/arithmetic"

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
    fun arithmetic1Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        assertNotNull(ns.find { it is Local && it.name() == "e" })
        assertNotNull(ns.find { it is Local && it.name() == "f" })
        val add = calls.find { it.name() == Operators.plus }.apply { assertNotNull(this) }
        val mul = calls.find { it.name() == Operators.multiplication }.apply { assertNotNull(this) }
        val sub = calls.find { it.name() == Operators.minus }.apply { assertNotNull(this) }
        val div = calls.find { it.name() == Operators.division }.apply { assertNotNull(this) }
        add!!; mul!!; sub!!; div!!
        assertTrue(add.id() < sub.id())
        assertTrue(sub.id() < mul.id())
        assertTrue(mul.id() < div.id())
    }

    @Test
    fun arithmetic2Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        val add = calls.find { it.name() == Operators.plus }.apply { assertNotNull(this) }
        val mul = calls.find { it.name() == Operators.multiplication }.apply { assertNotNull(this) }
        add!!; mul!!
        assertTrue(mul.id() < add.id())
    }

    @Test
    fun arithmetic3Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        val add = calls.find { it.name() == Operators.plus }.apply { assertNotNull(this) }
        val mul = calls.find { it.name() == Operators.multiplication }.apply { assertNotNull(this) }
        val sub = calls.find { it.name() == Operators.minus }.apply { assertNotNull(this) }
        add!!; mul!!; sub!!
        assertTrue(mul.id() < add.id())
        assertTrue(add.id() < sub.id())
    }

    @Test
    fun arithmetic4Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        val addList = calls.filter { it.name() == Operators.plus }
            .apply { assertFalse(this.toList().isNullOrEmpty()) }
        assertEquals(2, addList.toList().size)
    }

    @Test
    fun arithmetic5Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        assertNotNull(ns.find { it is Local && it.name() == "e" })
        assertNotNull(ns.find { it is Local && it.name() == "f" })
        assertNotNull(ns.find { it is Local && it.name() == "g" })
        val and = calls.find { it.name() == Operators.logicalAnd }.apply { assertNotNull(this) }
        val or = calls.find { it.name() == Operators.logicalOr }.apply { assertNotNull(this) }
        val shl = calls.find { it.name() == Operators.shiftLeft }.apply { assertNotNull(this) }
        val shr = calls.find { it.name() == Operators.logicalShiftRight }.apply { assertNotNull(this) }
        val rem = calls.find { it.name() == Operators.modulo }.apply { assertNotNull(this) }
        and!!; or!!; shl!!; shr!!; rem!!
        assertTrue(and.id() < or.id())
        assertTrue(or.id() < shl.id())
        assertTrue(shl.id() < shr.id())
        assertTrue(shr.id() < rem.id())
    }

    @Test
    fun arithmetic6Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        assertNotNull(ns.find { it is Local && it.name() == "c" })
        assertNotNull(ns.find { it is Local && it.name() == "d" })
        val xor = calls.find { it.name() == Operators.xor }.apply { assertNotNull(this) }
        val ushr = calls.find { it.name() == Operators.arithmeticShiftRight }.apply { assertNotNull(this) }
        xor!!; ushr!!
        assertTrue(xor.id() < ushr.id())
    }

    @Test
    fun arithmetic7Test() {
        val ns = g.nodes().asSequence().toList()
        val calls = ns.filterIsInstance<Call>().toList()
        assertNotNull(ns.find { it is Local && it.name() == "a" })
        assertNotNull(ns.find { it is Local && it.name() == "b" })
        val addList = calls.filter { it.name() == Operators.plus }.apply {
            assertFalse(this.toList().isNullOrEmpty())
        }
        assertEquals(4, addList.toList().size)
    }
}