package za.ac.sun.plume.intraprocedural

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import java.io.File
import java.io.IOException

class LoopIntraproceduralTest {

    companion object {
        private val logger = LogManager.getLogger(LoopIntraproceduralTest::javaClass)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}loop"
        private val TEST_GRAPH = TestConstants.testGraph

        @AfterAll
        @JvmStatic
        @Throws(IOException::class)
        fun tearDownAll() {
            deleteClassFiles(PATH)
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear " + ConditionalIntraproceduralTest::class.java.name + "'s test resources.")
            }
        }

        init {
            val testFileUrl = LoopIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val fileCannon = Extractor(driver, CLS_PATH)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Loop$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        fileCannon.load(f)
        fileCannon.project()
        driver.exportGraph(TEST_GRAPH)
    }

    @Test
    fun loop1Test() {
    }

    @Test
    fun loop2Test() {

    }

    @Test
    fun loop3Test() {

    }

    @Test
    fun loop4Test() {

    }

    @Test
    fun loop5Test() {

    }

    @Test
    fun loop6Test() {

    }

    @Test
    fun loop7Test() {

    }

    @Test
    fun loop8Test() {

    }

    @Test
    fun loop9Test() {

    }

    @Test
    fun loop10Test() {

    }

    @Test
    fun loop11Test() {

    }

    @Test
    fun loop12Test() {

    }

    @Test
    fun loop13Test() {

    }
}