package za.ac.sun.plume.intraprocedural

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.vertices.CallVertex
import za.ac.sun.plume.domain.models.vertices.LocalVertex
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import java.io.File
import java.io.IOException

class ArithmeticTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}arithmetic"

        init {
            val testFileUrl = ArithmeticTest::class.java.classLoader.getResource(TEST_PATH)
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
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "d" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "e" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "f" })
        val add = vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.apply { assertNotNull(this) }
        val mul = vertices.filterIsInstance<CallVertex>().find { it.name == "MUL" }.apply { assertNotNull(this) }
        val sub = vertices.filterIsInstance<CallVertex>().find { it.name == "SUB" }.apply { assertNotNull(this) }
        val div = vertices.filterIsInstance<CallVertex>().find { it.name == "DIV" }.apply { assertNotNull(this) }
        add!!; mul!!; sub!!; div!!
        assertTrue(add.order < sub.order)
        assertTrue(sub.order < mul.order)
        assertTrue(mul.order < div.order)
    }

    @Test
    fun arithmetic2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        val add = vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.apply { assertNotNull(this) }
        val mul = vertices.filterIsInstance<CallVertex>().find { it.name == "MUL" }.apply { assertNotNull(this) }
        add!!; mul!!
        assertTrue(mul.order < add.order)
    }

    @Test
    fun arithmetic3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        val add = vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.apply { assertNotNull(this) }
        val mul = vertices.filterIsInstance<CallVertex>().find { it.name == "MUL" }.apply { assertNotNull(this) }
        val sub = vertices.filterIsInstance<CallVertex>().find { it.name == "SUB" }.apply { assertNotNull(this) }
        add!!; mul!!; sub!!
        assertTrue(mul.order < add.order)
        assertTrue(add.order < sub.order)
    }

    @Test
    fun arithmetic4Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "d" })
        val addList = vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.apply { assertFalse (this.isNullOrEmpty()) }
        assertEquals(2, addList.size)
    }

    @Test
    fun arithmetic5Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "d" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "e" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "f" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "g" })
        val and = vertices.filterIsInstance<CallVertex>().find { it.name == "AND" }.apply { assertNotNull(this) }
        val or = vertices.filterIsInstance<CallVertex>().find { it.name == "OR" }.apply { assertNotNull(this) }
        val shl = vertices.filterIsInstance<CallVertex>().find { it.name == "SHL" }.apply { assertNotNull(this) }
        val shr = vertices.filterIsInstance<CallVertex>().find { it.name == "SHR" }.apply { assertNotNull(this) }
        val rem = vertices.filterIsInstance<CallVertex>().find { it.name == "REM" }.apply { assertNotNull(this) }
        and!!; or!!; shl!!; shr!!; rem!!
        assertTrue(and.order < or.order)
        assertTrue(or.order < shl.order)
        assertTrue(shl.order < shr.order)
        assertTrue(shr.order < rem.order)
    }

    @Test
    fun arithmetic6Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "c" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "d" })
        val xor = vertices.filterIsInstance<CallVertex>().find { it.name == "XOR" }.apply { assertNotNull(this) }
        val ushr = vertices.filterIsInstance<CallVertex>().find { it.name == "USHR" }.apply { assertNotNull(this) }
        xor!!; ushr!!
        assertTrue(xor.order < ushr.order)
    }

    @Test
    fun arithmetic7Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.find { it is LocalVertex && it.name == "a" })
        assertNotNull(vertices.find { it is LocalVertex && it.name == "b" })
        val addList = vertices.filterIsInstance<CallVertex>().filter { it.name == "ADD" }.apply { assertFalse (this.isNullOrEmpty()) }
        assertEquals(4, addList.size)
    }
}