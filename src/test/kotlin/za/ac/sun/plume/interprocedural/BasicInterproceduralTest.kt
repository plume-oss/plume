package za.ac.sun.plume.interprocedural

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.*
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants.testGraph
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import java.io.File
import java.io.IOException
import java.lang.NullPointerException

class BasicInterproceduralTest {
    private lateinit var currentTestNumber: String

    companion object {
        private val logger = LogManager.getLogger(BasicInterproceduralTest::class.java)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "interprocedural${File.separator}basic"
        private val TEST_GRAPH = testGraph

        @AfterAll
        @JvmStatic
        @Throws(IOException::class)
        fun tearDownAll() {
            deleteClassFiles(PATH)
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear ${BasicInterproceduralTest::javaClass.name}'s test resources.")
            }
        }

        init {
            val testFileUrl = BasicInterproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val driver: TinkerGraphDriver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val extractor = Extractor(driver, CLS_PATH)
        // Select test resource based on integer in method name
        currentTestNumber = testInfo.displayName.replace("[^0-9]".toRegex(), "")
        val resourceDir ="${PATH.absolutePath}${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun basicCall1Test() {
    }

    @Test
    fun basicCall2Test() {
    }

    @Test
    fun basicCall3Test() {
    }

    @Test
    fun basicCall4Test() {
    }

    @Test
    fun basicCall5Test() {
    }
}