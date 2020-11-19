package za.ac.sun.plume

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import java.io.File

class BasicExtractorTest {
    companion object {
        private val driver = DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver
        private val TEST_PATH = "extractor_tests${File.separator}"
        private lateinit var CLS_PATH: File
        private lateinit var extractor: Extractor
        private lateinit var validSourceFile: File
        private lateinit var validClassFile: File
        private lateinit var validDirectory: File
        private lateinit var validJarFile: File

        private fun getTestResource(dir: String): File {
            val resourceURL = BasicExtractorTest::class.java.classLoader.getResource(dir)
                    ?: throw java.lang.NullPointerException("Unable to obtain test resource")
            return File(resourceURL.file)
        }

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            validSourceFile = getTestResource("${TEST_PATH}Test1.java")
            validClassFile = getTestResource("${TEST_PATH}Test2.class")
            validJarFile = getTestResource("${TEST_PATH}Test3.jar")
            validDirectory = getTestResource("${TEST_PATH}dir_test")
            CLS_PATH = File(getTestResource(TEST_PATH).absolutePath.replace(System.getProperty("user.dir") + File.separator, "").removeSuffix(TEST_PATH.replace(File.separator, "")))
            extractor = Extractor(driver, CLS_PATH)
        }
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun validSourceFileTest() {
        extractor.load(validSourceFile)
        extractor.project()
        val graph = driver.getWholeGraph()
        val vertices = graph.vertices()
        assertNotNull(vertices.filterIsInstance<NamespaceBlockVertex>().find { it.name == "extractor_tests" })
        vertices.filterIsInstance<FileVertex>().find { it.name == "extractor_tests.Test1" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<LocalVertex>().find { it.name == "a" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find { it.name == "b" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find { it.name == "c" }.let { assertNotNull(it); assertEquals("int", it!!.typeFullName) }
        vertices.filterIsInstance<CallVertex>().find { it.name == "ADD" }.let { assertNotNull(it) }
    }

    @Test
    fun validClassFileTest() {
        extractor.load(validClassFile)
        extractor.project()
        val graph = driver.getWholeGraph()
        val vertices = graph.vertices()
        assertNotNull(vertices.filterIsInstance<NamespaceBlockVertex>().find { it.name == "extractor_tests" })
        vertices.filterIsInstance<FileVertex>().find { it.name == "extractor_tests.Test2" }.let { assertNotNull(it) }
        vertices.filterIsInstance<MethodVertex>().find { it.name == "main" }.let { assertNotNull(it) }
        vertices.filterIsInstance<LocalVertex>().find { it.name == "l1" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find { it.name == "l2" }.let { assertNotNull(it); assertEquals("byte", it!!.typeFullName) }
        vertices.filterIsInstance<LocalVertex>().find { it.name == "l3" }.let { assertNotNull(it); assertEquals("int", it!!.typeFullName) }
        vertices.filterIsInstance<CallVertex>().find { it.name == "SUB" }.let { assertNotNull(it) }
    }

    @Test
    fun validDirectoryTest() {
        extractor.load(validDirectory)
        extractor.project()
        val graph = driver.getProgramStructure()
        val vertices = graph.vertices()
        vertices.filterIsInstance<FileVertex>().let { fileList ->
            assertNotNull(fileList.firstOrNull { it.name == "extractor_tests.dir_test.Dir1" })
            assertNotNull(fileList.firstOrNull { it.name == "extractor_tests.dir_test.pack.Dir2" })
        }
        vertices.filterIsInstance<NamespaceBlockVertex>().let { fileList ->
            assertNotNull(fileList.firstOrNull { it.name == "dir_test" })
            assertNotNull(fileList.firstOrNull { it.name == "extractor_tests" })
            assertNotNull(fileList.firstOrNull { it.name == "pack" })
        }
    }

    @Test
    fun loadFileThatDoesNotExistTest() {
        Assertions.assertThrows(NullPointerException::class.java) { extractor.load(File("dne.class")) }
    }
}