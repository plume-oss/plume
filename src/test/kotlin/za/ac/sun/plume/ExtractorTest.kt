package za.ac.sun.plume

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.*
import za.ac.sun.plume.TestConstants.testGraph
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import java.io.File

class ExtractorTest {
    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun validSourceFileTest() {
        extractor.load(validSourceFile)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun validClassFileTest() {
        extractor.load(validClassFile)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun validDirectoryTest() {
        extractor.load(validDirectory)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun validPy2Test() {
        extractor.load(validPy2File)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun validJsTest() {
        extractor.load(validJsFile)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun compileMultipleLanguagesTest() {
        extractor.load(polyglotDir)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun loadFileThatDoesNotExistTest() {
        Assertions.assertThrows(NullPointerException::class.java) { extractor.load(File("dne.class")) }
    }

    companion object {
        private val logger: Logger = LogManager.getLogger(ExtractorTest::class.java)
        private val TEST_GRAPH = testGraph
        private val TEST_PATH = "extractor_tests${File.separator}"
        private lateinit var CLS_PATH: File
        private lateinit var extractor: Extractor
        private lateinit var validSourceFile: File
        private lateinit var validClassFile: File
        private lateinit var validDirectory: File
        private lateinit var validJarFile: File
        private lateinit var validPy2File: File
        private lateinit var validJsFile: File
        private lateinit var polyglotDir: File
        private lateinit var driver: TinkerGraphDriver

        private fun getTestResource(dir: String): File {
            val resourceURL = ExtractorTest::class.java.classLoader.getResource(dir)
                    ?: throw java.lang.NullPointerException("Unable to obtain test resource")
            return File(resourceURL.file)
        }

        @BeforeAll
        @JvmStatic
        fun setUpAll() {
            validSourceFile = getTestResource("${TEST_PATH}Test1.java")
            validClassFile = getTestResource("${TEST_PATH}Test2.class")
            validJarFile = getTestResource("${TEST_PATH}Test3.jar")
            validPy2File = getTestResource("${TEST_PATH}Test4.py")
            validJsFile = getTestResource("${TEST_PATH}Test5.js")
            validDirectory = getTestResource("${TEST_PATH}dir_test")
            polyglotDir = getTestResource("${TEST_PATH}polyglot")
            CLS_PATH = File(getTestResource(TEST_PATH).absolutePath.replace(System.getProperty("user.dir") + File.separator, "").removeSuffix(TEST_PATH.replace(File.separator, "")))
            driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
            extractor = Extractor(driver, CLS_PATH)
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear ${ExtractorTest::javaClass.name}'s test resources.")
            }
        }
    }
}