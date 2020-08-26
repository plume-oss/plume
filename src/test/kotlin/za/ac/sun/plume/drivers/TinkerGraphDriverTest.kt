package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import za.ac.sun.plume.TestDomainResources.Companion.INT_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_2
import za.ac.sun.plume.TestDomainResources.Companion.STRING_1
import za.ac.sun.plume.TestDomainResources.Companion.STRING_2
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.vertices.*
import java.io.File
import java.util.*

class TinkerGraphDriverTest {

    companion object {
        private val tempDir = System.getProperty("java.io.tmpdir")
        private val logger = LogManager.getLogger(TinkerGraphDriverTest::class.java)
        lateinit var driver: TinkerGraphDriver
        val testGraphML = "$tempDir/plume/plume_driver_test.xml"
        val testGraphSON = "$tempDir/plume/plume_driver_test.json"
        val testGryo = "$tempDir/plume/plume_driver_test.kryo"

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            val testFiles = arrayOf(File(testGraphML), File(testGraphSON), File(testGryo))
            Arrays.stream(testFiles).forEach { file: File ->
                try {
                    if (!file.delete()) logger.warn("Could not clear test resources.")
                } catch (e: Exception) {
                    logger.warn("Could not clear test resources.", e)
                }
            }
        }
    }

    @BeforeEach
    fun setUp() {
        driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
    }

    @AfterEach
    fun tearDown() = driver.clearGraph().close()

    @Nested
    @DisplayName("Test driver vertex find and exist methods")
    inner class VertexAddAndExistsTests {
        @Test
        fun findAstVertex() {
            val v1 = ArrayInitializerVertex(INT_1)
            val v2 = ArrayInitializerVertex(INT_2)
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            assertTrue(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
        }

        @Test
        fun findBindingVertex() {
            val v1 = BindingVertex(STRING_1, STRING_2)
            val v2 = BindingVertex(STRING_2, STRING_1)
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            assertTrue(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
        }

        @Test
        fun findMetaDataVertex() {
            val v1 = MetaDataVertex(STRING_1, STRING_2)
            val v2 = MetaDataVertex(STRING_2, STRING_1)
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            assertTrue(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
        }

        @Test
        fun findTypeVertex() {
            val v1 = TypeVertex(STRING_1, STRING_2, STRING_2)
            val v2 = TypeVertex(STRING_2, STRING_1, STRING_2)
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            assertTrue(driver.exists(v1))
            assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
        }
    }

    @Nested
    @DisplayName("Test driver edge find and exist methods")
    inner class EdgeAddAndExistsTests {
        private val v1 = ArrayInitializerVertex(INT_1)
        private val v2 = ArrayInitializerVertex(INT_2)

        @BeforeEach
        fun setUp() {
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v2))
        }

        @Test
        fun testEdgeWhenVerticesAreAlreadyPresent() {
            driver.addVertex(v1)
            driver.addVertex(v2)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            driver.addEdge(v1, v2, EdgeLabel.AST)
            assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            driver.addEdge(v1, v2, EdgeLabel.AST)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testEdgeDirection() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            assertFalse(driver.exists(v2, v1, EdgeLabel.AST))
            driver.addEdge(v1, v2, EdgeLabel.AST)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
            assertFalse(driver.exists(v2, v1, EdgeLabel.AST))
        }

        @Test
        fun testCfgEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.CFG))
            driver.addEdge(v1, v2, EdgeLabel.CFG)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.CFG))
        }

        @Test
        fun testCapturedByEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.CAPTURED_BY))
            driver.addEdge(v1, v2, EdgeLabel.CAPTURED_BY)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.CAPTURED_BY))
        }

        @Test
        fun testBindsToEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.BINDS_TO))
            driver.addEdge(v1, v2, EdgeLabel.BINDS_TO)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.REF))
            driver.addEdge(v1, v2, EdgeLabel.REF)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.RECEIVER))
            driver.addEdge(v1, v2, EdgeLabel.RECEIVER)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.CONDITION))
            driver.addEdge(v1, v2, EdgeLabel.CONDITION)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.BINDS))
            driver.addEdge(v1, v2, EdgeLabel.BINDS)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            assertFalse(driver.exists(v1, v2, EdgeLabel.ARGUMENT))
            driver.addEdge(v1, v2, EdgeLabel.ARGUMENT)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v1, v2, EdgeLabel.ARGUMENT))
        }
    }

    @Nested
    @DisplayName("Max order tests")
    inner class MaxOrderTests {
        @Test
        fun testMaxOrderOnEmptyGraph() = assertEquals(0, driver.maxOrder())

        @Test
        fun testMaxOrderOnGraphWithOneVertex() {
            val v1 = ArrayInitializerVertex(INT_2)
            driver.addVertex(v1)
            assertEquals(INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithMoreThanOneVertex() {
            val v1 = ArrayInitializerVertex(INT_2)
            val v2 = MetaDataVertex(STRING_1, STRING_2)
            driver.addVertex(v1)
            driver.addVertex(v2)
            assertEquals(INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithNoAstVertex() {
            val v1 = BindingVertex(STRING_1, STRING_2)
            val v2 = MetaDataVertex(STRING_1, STRING_2)
            driver.addVertex(v1)
            driver.addVertex(v2)
            assertEquals(0, driver.maxOrder())
        }
    }

    @Nested
    @DisplayName("Graph import/export from file tests")
    inner class ValidateGraphImportExportFromFiles {
        private val v1 = FileVertex(STRING_1, INT_1)
        private val v2 = NamespaceBlockVertex(STRING_1, STRING_2, INT_1)

        @BeforeEach
        fun setUp() {
            driver.addEdge(v1, v2, EdgeLabel.AST)
        }

        @Test
        fun testImportingGraphML() {
            driver.exportGraph(testGraphML)
            driver.clearGraph()
            driver.importGraph(testGraphML)
            assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testImportingGraphJSON() {
            driver.exportGraph(testGraphSON)
            driver.clearGraph()
            driver.importGraph(testGraphSON)
            assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testImportingGryo() {
            driver.exportGraph(testGryo)
            driver.clearGraph()
            driver.importGraph(testGryo)
            assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testImportingGraphThatDNE() {
            assertThrows(IllegalArgumentException::class.java) { driver.importGraph("/tmp/plume/DNE.kryo") }
        }

        @Test
        fun testImportingInvalidExtension() {
            assertThrows(IllegalArgumentException::class.java) { driver.importGraph("/tmp/plume/invalid.txt") }
        }

        @Test
        fun testExportingInvalidExtension() {
            assertThrows(IllegalArgumentException::class.java) { driver.exportGraph("/tmp/plume/invalid.txt") }
        }
    }
}