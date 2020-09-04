package za.ac.sun.plume.intraprocedural

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.*
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import java.io.File
import java.io.IOException

class SwitchIntraproceduralTest {
    private lateinit var testResourcePath: String

    companion object {
        private val logger = LogManager.getLogger(SwitchIntraproceduralTest::javaClass)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}switches"
        private val TEST_GRAPH = TestConstants.testGraph

        @AfterAll
        @JvmStatic
        @Throws(IOException::class)
        fun tearDownAll() {
            deleteClassFiles(PATH)
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear ${SwitchIntraproceduralTest::class.java.name}'s test resources.")
            }
        }

        init {
            val testFileUrl = SwitchIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val hook = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val fileCannon = Extractor(hook, CLS_PATH)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        testResourcePath = "${PATH.absolutePath}${File.separator}Switch$currentTestNumber"
        val testSourceFile = "$testResourcePath.java"
        // Load test resource and project + export graph
        val f = File(testSourceFile)
        fileCannon.load(f)
        fileCannon.project()
        hook.exportGraph(TEST_GRAPH)
    }

    @Test
    fun switch1Test() {
    }

    @Test
    fun switch2Test() {
    }

    @Test
    fun switch3Test() {
    }

    @Test
    fun switch4Test() {
    }

    @Test
    fun switch5Test() {
    }

}