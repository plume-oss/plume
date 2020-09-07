package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import za.ac.sun.plume.TestDomainResources.Companion.DISPATCH_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_2
import za.ac.sun.plume.TestDomainResources.Companion.STRING_1
import za.ac.sun.plume.TestDomainResources.Companion.STRING_2
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
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
        private val v1 = LiteralVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        private val v2 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        private val v3 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        private val v4 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1)
        private val v5 = BindingVertex(STRING_1, STRING_2)
        private val v6 = MethodVertex(STRING_1, STRING_2, STRING_1, STRING_1, INT_1, INT_1, INT_1)
        private val v7 = LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1)
        private val v8 = CallVertex(STRING_1, STRING_2, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1)
        private val v9 = TypeArgumentVertex(INT_1)
        private val v10 = TypeParameterVertex(STRING_1, INT_1)
        private val v11 = TypeDeclVertex(STRING_1, STRING_1, STRING_1, INT_1)

        @BeforeEach
        fun setUp() {
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v2))
        }

        @Test
        fun testEdgeWhenVerticesAreAlreadyPresent() {
            driver.addVertex(v11)
            driver.addVertex(v10)
            assertTrue(driver.exists(v11))
            assertTrue(driver.exists(v10))
            assertFalse(driver.exists(v11, v10, EdgeLabel.AST))
            driver.addEdge(v11, v10, EdgeLabel.AST)
            assertTrue(driver.exists(v11, v10, EdgeLabel.AST))
        }

        @Test
        fun testEdgeWhenItWillViolateSchema() {
            driver.addVertex(v1)
            driver.addVertex(v2)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v2))
            assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            assertThrows(PlumeSchemaViolationException::class.java) { driver.addEdge(v1, v2, EdgeLabel.AST) }
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            assertFalse(driver.exists(v8, v2, EdgeLabel.AST))
            driver.addEdge(v8, v2, EdgeLabel.AST)
            assertTrue(driver.exists(v8))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v8, v2, EdgeLabel.AST))
        }

        @Test
        fun testEdgeDirection() {
            assertFalse(driver.exists(v8, v2, EdgeLabel.AST))
            assertFalse(driver.exists(v2, v8, EdgeLabel.AST))
            driver.addEdge(v8, v2, EdgeLabel.AST)
            assertTrue(driver.exists(v8))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v8, v2, EdgeLabel.AST))
            assertFalse(driver.exists(v2, v8, EdgeLabel.AST))
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
            assertFalse(driver.exists(v7, v5, EdgeLabel.CAPTURED_BY))
            driver.addEdge(v7, v5, EdgeLabel.CAPTURED_BY)
            assertTrue(driver.exists(v5))
            assertTrue(driver.exists(v7))
            assertTrue(driver.exists(v7, v5, EdgeLabel.CAPTURED_BY))
        }

        @Test
        fun testBindsToEdgeCreation() {
            assertFalse(driver.exists(v9, v10, EdgeLabel.BINDS_TO))
            driver.addEdge(v9, v10, EdgeLabel.BINDS_TO)
            assertTrue(driver.exists(v9))
            assertTrue(driver.exists(v10))
            assertTrue(driver.exists(v9, v10, EdgeLabel.BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            assertFalse(driver.exists(v5, v6, EdgeLabel.REF))
            driver.addEdge(v5, v6, EdgeLabel.REF)
            assertTrue(driver.exists(v5))
            assertTrue(driver.exists(v6))
            assertTrue(driver.exists(v5, v6, EdgeLabel.REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            assertFalse(driver.exists(v8, v2, EdgeLabel.RECEIVER))
            driver.addEdge(v8, v2, EdgeLabel.RECEIVER)
            assertTrue(driver.exists(v8))
            assertTrue(driver.exists(v2))
            assertTrue(driver.exists(v8, v2, EdgeLabel.RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            assertFalse(driver.exists(v3, v4, EdgeLabel.CONDITION))
            driver.addEdge(v3, v4, EdgeLabel.CONDITION)
            assertTrue(driver.exists(v3))
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v3, v4, EdgeLabel.CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            assertFalse(driver.exists(v11, v5, EdgeLabel.BINDS))
            driver.addEdge(v11, v5, EdgeLabel.BINDS)
            assertTrue(driver.exists(v11))
            assertTrue(driver.exists(v5))
            assertTrue(driver.exists(v11, v5, EdgeLabel.BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            assertFalse(driver.exists(v8, v4, EdgeLabel.ARGUMENT))
            driver.addEdge(v8, v4, EdgeLabel.ARGUMENT)
            assertTrue(driver.exists(v8))
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v8, v4, EdgeLabel.ARGUMENT))
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
        private val v1 = NamespaceBlockVertex(STRING_1, STRING_2, INT_1)
        private val v2 = FileVertex(STRING_1, INT_1)

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