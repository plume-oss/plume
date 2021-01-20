package io.github.plume.oss.extractor.intraprocedural

import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewLocalBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.io.IOException

class ArithmeticTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
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
        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.close()
    }

    @Test
    fun arithmetic1Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "d" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "e" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "f" })
        val add = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "ADD" }
            .apply { assertNotNull(this) }
        val mul = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "MUL" }
            .apply { assertNotNull(this) }
        val sub = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "SUB" }
            .apply { assertNotNull(this) }
        val div = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "DIV" }
            .apply { assertNotNull(this) }
        add!!; mul!!; sub!!; div!!
        assertTrue(add.build().order() < sub.build().order())
        assertTrue(sub.build().order() < mul.build().order())
        assertTrue(mul.build().order() < div.build().order())
    }

    @Test
    fun arithmetic2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        val add = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "ADD" }
            .apply { assertNotNull(this) }
        val mul = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "MUL" }
            .apply { assertNotNull(this) }
        add!!; mul!!
        assertTrue(mul.build().order() < add.build().order())
    }

    @Test
    fun arithmetic3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        val add = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "ADD" }
            .apply { assertNotNull(this) }
        val mul = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "MUL" }
            .apply { assertNotNull(this) }
        val sub = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "SUB" }
            .apply { assertNotNull(this) }
        add!!; mul!!; sub!!
        assertTrue(mul.build().order() < add.build().order())
        assertTrue(add.build().order() < sub.build().order())
    }

    @Test
    fun arithmetic4Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "d" })
        val addList = vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .apply { assertFalse(this.isNullOrEmpty()) }
        assertEquals(2, addList.size)
    }

    @Test
    fun arithmetic5Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "d" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "e" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "f" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "g" })
        val and = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "AND" }
            .apply { assertNotNull(this) }
        val or =
            vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "OR" }.apply { assertNotNull(this) }
        val shl = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "SHL" }
            .apply { assertNotNull(this) }
        val shr = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "SHR" }
            .apply { assertNotNull(this) }
        val rem = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "REM" }
            .apply { assertNotNull(this) }
        and!!; or!!; shl!!; shr!!; rem!!
        assertTrue(and.build().order() < or.build().order())
        assertTrue(or.build().order() < shl.build().order())
        assertTrue(shl.build().order() < shr.build().order())
        assertTrue(shr.build().order() < rem.build().order())
    }

    @Test
    fun arithmetic6Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "c" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "d" })
        val xor = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "XOR" }
            .apply { assertNotNull(this) }
        val ushr = vertices.filterIsInstance<NewCallBuilder>().find { it.build().name() == "USHR" }
            .apply { assertNotNull(this) }
        xor!!; ushr!!
        assertTrue(xor.build().order() < ushr.build().order())
    }

    @Test
    fun arithmetic7Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "a" })
        assertNotNull(vertices.find { it is NewLocalBuilder && it.build().name() == "b" })
        val addList = vertices.filterIsInstance<NewCallBuilder>().filter { it.build().name() == "ADD" }
            .apply { assertFalse(this.isNullOrEmpty()) }
        assertEquals(4, addList.size)
    }
}