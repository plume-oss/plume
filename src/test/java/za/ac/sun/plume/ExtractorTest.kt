package za.ac.sun.plume

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import za.ac.sun.plume.TestConstants.testGraph
import za.ac.sun.plume.domain.enums.EdgeLabels
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.intraprocedural.ArithmeticTest
import za.ac.sun.plume.intraprocedural.BasicIntraproceduralTest
import za.ac.sun.plume.intraprocedural.BasicIntraproceduralTest.Companion.testBasic1Structure
import za.ac.sun.plume.intraprocedural.ConditionalIntraproceduralTest
import za.ac.sun.plume.util.TestQueryBuilderUtil
import java.io.File
import java.io.IOException
import java.util.*

class ExtractorTest {
    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    @Throws(IOException::class)
    fun validSourceFileTest() {
        extractor.load(validSourceFile)
        extractor.project()
        driver.exportCurrentGraph(TEST_GRAPH)
    }

    @Test
    @Throws(IOException::class)
    fun validClassFileTest() {
        extractor.load(validClassFile)
        extractor.project()
        driver.exportCurrentGraph(TEST_GRAPH)
    }

    @Test
    @Throws(IOException::class)
    fun validDirectoryTest() {
        extractor.load(validDirectory)
        extractor.project()
        driver.exportCurrentGraph(TEST_GRAPH)
    }

    @Disabled
    @Test
    @Throws(IOException::class)
    fun validJarTest() {
        val g = TinkerGraph.open().traversal()
        extractor.load(validJarFile)
        extractor.project()
        driver.exportCurrentGraph(TEST_GRAPH)
        g.io<Any>(TEST_GRAPH).read().iterate()

        // This is za.ac.sun.plume.intraprocedural.Basic6's test in a JAR
        val intraNamespaceTraversal = g.V().has(VertexLabels.NAMESPACE_BLOCK.toString(), "fullName", "intraprocedural")
        assertTrue(intraNamespaceTraversal.hasNext())
        val intraNamespaceVertex = intraNamespaceTraversal.next()
        val basicNamespaceTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabels.AST, intraNamespaceVertex, VertexLabels.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic")
        assertTrue(basicNamespaceTraversal.hasNext())
        val basicNamespaceVertex = basicNamespaceTraversal.next()
        val basic6NamespaceTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabels.AST, basicNamespaceVertex, VertexLabels.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic.basic6")
        assertTrue(basic6NamespaceTraversal.hasNext())
        val basic6NamespaceVertex = basic6NamespaceTraversal.next()
        val basicMethodTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabels.AST, basicNamespaceVertex, VertexLabels.METHOD, "name", "main")
        assertTrue(basicMethodTraversal.hasNext())
        val basic6MethodTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabels.AST, basic6NamespaceVertex, VertexLabels.METHOD, "name", "main")
        assertTrue(basic6MethodTraversal.hasNext())
        assertEquals(6, TestQueryBuilderUtil.buildStoreTraversal(g, EdgeLabels.AST, intraNamespaceVertex).count().next())
        testBasic1Structure(g, basicNamespaceVertex)
        testBasic1Structure(g, basic6NamespaceVertex)
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
            validDirectory = getTestResource("${TEST_PATH}dir_test")
            CLS_PATH = File(getTestResource(TEST_PATH).absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
            driver = TinkerGraphDriver.Builder().build()
            extractor = Extractor(driver, CLS_PATH)
        }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear ${ConditionalIntraproceduralTest::javaClass.name}'s test resources.")
            }
        }
    }
}