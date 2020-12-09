package za.ac.sun.plume.extractor.interprocedural

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import java.io.File
import java.io.IOException

class TypeInterproceduralTest {

    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private lateinit var graph: PlumeGraph
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "interprocedural${File.separator}type"

        init {
            val testFileUrl = TypeInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
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
        val currentTestNumber = testInfo.displayName.replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Type$currentTestNumber.java"
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
    fun type1Test() {
        val vertices = graph.vertices()
        vertices.filterIsInstance<LocalVertex>().let { localList ->
            assertNotNull(localList.firstOrNull { it.name == "intList" && it.typeFullName == "java.util.LinkedList" })
            assertNotNull(localList.firstOrNull { it.name == "stringList" && it.typeFullName == "java.util.LinkedList" })
            assertNotNull(localList.firstOrNull { it.name == "\$stack3" && it.typeFullName == "java.util.LinkedList" })
            assertNotNull(localList.firstOrNull { it.name == "\$stack4" && it.typeFullName == "java.util.LinkedList" })
        }
        vertices.filterIsInstance<CallVertex>().filter { it.name == "<init>" }.let { callList ->
            assertNotNull(callList.firstOrNull { it.methodFullName == "java.util.LinkedList: void <init>()" })
            assertNotNull(callList.firstOrNull { it.methodFullName == "java.lang.Object: void <init>()" })
        }
        vertices.filterIsInstance<TypeRefVertex>().filter { it.typeFullName == "java.util.LinkedList" }
                .let { typeRefs -> assertEquals(2, typeRefs.size) }
    }

    @Test
    fun type2Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.filterIsInstance<LocalVertex>().firstOrNull { it.name == "intArray" && it.typeFullName == "int[]" })
        vertices.filterIsInstance<IdentifierVertex>().filter { it.name.contains("intArray") }.let { callList ->
            assertNotNull(callList.firstOrNull { it.argumentIndex == 0 })
            assertNotNull(callList.firstOrNull { it.argumentIndex == 4 })
        }
    }

    @Test
    fun type3Test() {
        val vertices = graph.vertices()
        assertNotNull(vertices.filterIsInstance<LocalVertex>().firstOrNull { it.name == "cls" && it.typeFullName == "java.lang.Class" })
        assertNotNull(vertices.filterIsInstance<LiteralVertex>().firstOrNull { it.code == "class \"Lintraprocedural/type/Type3;\"" && it.typeFullName == "java.lang.Class" })
    }
}