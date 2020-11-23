package za.ac.sun.plume.drivers

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import za.ac.sun.plume.TestDomainResources
import za.ac.sun.plume.TestDomainResources.Companion.DISPATCH_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_1
import za.ac.sun.plume.TestDomainResources.Companion.INT_2
import za.ac.sun.plume.TestDomainResources.Companion.STRING_1
import za.ac.sun.plume.TestDomainResources.Companion.STRING_2
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
import za.ac.sun.plume.domain.models.vertices.*
import kotlin.properties.Delegates
import za.ac.sun.plume.TestDomainResources.Companion.v1
import za.ac.sun.plume.TestDomainResources.Companion.v10
import za.ac.sun.plume.TestDomainResources.Companion.v11
import za.ac.sun.plume.TestDomainResources.Companion.v12
import za.ac.sun.plume.TestDomainResources.Companion.v13
import za.ac.sun.plume.TestDomainResources.Companion.v14
import za.ac.sun.plume.TestDomainResources.Companion.v15
import za.ac.sun.plume.TestDomainResources.Companion.v16
import za.ac.sun.plume.TestDomainResources.Companion.v17
import za.ac.sun.plume.TestDomainResources.Companion.v18
import za.ac.sun.plume.TestDomainResources.Companion.v19
import za.ac.sun.plume.TestDomainResources.Companion.v2
import za.ac.sun.plume.TestDomainResources.Companion.v3
import za.ac.sun.plume.TestDomainResources.Companion.v4
import za.ac.sun.plume.TestDomainResources.Companion.v5
import za.ac.sun.plume.TestDomainResources.Companion.v6
import za.ac.sun.plume.TestDomainResources.Companion.v7
import za.ac.sun.plume.TestDomainResources.Companion.v8
import za.ac.sun.plume.TestDomainResources.Companion.v9

class JanusGraphDriverIntTest {

    companion object {
        lateinit var driver: JanusGraphDriver
        private var testStartTime by Delegates.notNull<Long>()

        @JvmStatic
        @BeforeAll
        fun setUpAll() = run { testStartTime = System.nanoTime() }

        @JvmStatic
        @AfterAll
        fun tearDownAll() = println("${JanusGraphDriverIntTest::class.java.simpleName} completed in ${(System.nanoTime() - testStartTime) / 1e6} ms")
    }

    @BeforeEach
    fun setUp() {
        driver = (DriverFactory(GraphDatabase.JANUS_GRAPH) as JanusGraphDriver).apply {
            remoteConfig("src/test/resources/conf/remote-graph.properties").connect()
        }
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
        @BeforeEach
        fun setUp() {
            assertFalse(driver.exists(v8))
            assertFalse(driver.exists(v6))
        }

        @Test
        fun testEdgeWhenVerticesAreAlreadyPresent() {
            driver.addVertex(v7)
            driver.addVertex(v19)
            assertTrue(driver.exists(v7))
            assertTrue(driver.exists(v19))
            assertFalse(driver.exists(v7, v19, EdgeLabel.AST))
            driver.addEdge(v7, v19, EdgeLabel.AST)
            assertTrue(driver.exists(v7, v19, EdgeLabel.AST))
        }

        @Test
        fun testEdgeWhenItWillViolateSchema() {
            driver.addVertex(v8)
            driver.addVertex(v6)
            assertTrue(driver.exists(v8))
            assertTrue(driver.exists(v6))
            assertFalse(driver.exists(v8, v6, EdgeLabel.AST))
            assertThrows(PlumeSchemaViolationException::class.java) { driver.addEdge(v8, v6, EdgeLabel.AST) }
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            assertFalse(driver.exists(v4, v6, EdgeLabel.AST))
            driver.addEdge(v4, v6, EdgeLabel.AST)
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v6))
            assertTrue(driver.exists(v4, v6, EdgeLabel.AST))
        }

        @Test
        fun testEdgeDirection() {
            assertFalse(driver.exists(v4, v6, EdgeLabel.AST))
            assertFalse(driver.exists(v6, v4, EdgeLabel.AST))
            driver.addEdge(v4, v6, EdgeLabel.AST)
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v6))
            assertTrue(driver.exists(v4, v6, EdgeLabel.AST))
            assertFalse(driver.exists(v6, v4, EdgeLabel.AST))
        }

        @Test
        fun testCfgEdgeCreation() {
            assertFalse(driver.exists(v8, v6, EdgeLabel.CFG))
            driver.addEdge(v8, v6, EdgeLabel.CFG)
            assertTrue(driver.exists(v8))
            assertTrue(driver.exists(v6))
            assertTrue(driver.exists(v8, v6, EdgeLabel.CFG))
        }

        @Test
        fun testCapturedByEdgeCreation() {
            assertFalse(driver.exists(v5, v17, EdgeLabel.CAPTURED_BY))
            driver.addEdge(v5, v17, EdgeLabel.CAPTURED_BY)
            assertTrue(driver.exists(v17))
            assertTrue(driver.exists(v5))
            assertTrue(driver.exists(v5, v17, EdgeLabel.CAPTURED_BY))
        }

        @Test
        fun testBindsToEdgeCreation() {
            assertFalse(driver.exists(v18, v19, EdgeLabel.BINDS_TO))
            driver.addEdge(v18, v19, EdgeLabel.BINDS_TO)
            assertTrue(driver.exists(v18))
            assertTrue(driver.exists(v19))
            assertTrue(driver.exists(v18, v19, EdgeLabel.BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            assertFalse(driver.exists(v17, v1, EdgeLabel.REF))
            driver.addEdge(v17, v1, EdgeLabel.REF)
            assertTrue(driver.exists(v17))
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v17, v1, EdgeLabel.REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            assertFalse(driver.exists(v4, v6, EdgeLabel.RECEIVER))
            driver.addEdge(v4, v6, EdgeLabel.RECEIVER)
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v6))
            assertTrue(driver.exists(v4, v6, EdgeLabel.RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            assertFalse(driver.exists(v15, v16, EdgeLabel.CONDITION))
            driver.addEdge(v15, v16, EdgeLabel.CONDITION)
            assertTrue(driver.exists(v15))
            assertTrue(driver.exists(v16))
            assertTrue(driver.exists(v15, v16, EdgeLabel.CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            assertFalse(driver.exists(v7, v17, EdgeLabel.BINDS))
            driver.addEdge(v7, v17, EdgeLabel.BINDS)
            assertTrue(driver.exists(v7))
            assertTrue(driver.exists(v17))
            assertTrue(driver.exists(v7, v17, EdgeLabel.BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            assertFalse(driver.exists(v4, v16, EdgeLabel.ARGUMENT))
            driver.addEdge(v4, v16, EdgeLabel.ARGUMENT)
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v4))
            assertTrue(driver.exists(v4, v16, EdgeLabel.ARGUMENT))
        }

        @Test
        fun testSourceFileEdgeCreation() {
            assertFalse(driver.exists(v1, v11, EdgeLabel.SOURCE_FILE))
            driver.addEdge(v1, v11, EdgeLabel.SOURCE_FILE)
            assertTrue(driver.exists(v1))
            assertTrue(driver.exists(v11))
            assertTrue(driver.exists(v1, v11, EdgeLabel.SOURCE_FILE))
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
    @DisplayName("Any PlumeGraph related tests based off of a test CPG")
    inner class PlumeGraphTests {

        @BeforeEach
        fun setUp() {
            TestDomainResources.generateSimpleCPG(driver)
        }

        @Test
        fun testGetWholeGraph() {
            val plumeGraph = driver.getWholeGraph()
            assertEquals("PlumeGraph(vertices:14, edges:19)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(14, graphVertices.size)
            // Check program structure
            assertTrue(plumeGraph.edgesOut(v11)[EdgeLabel.AST]?.contains(v12) ?: false)
            assertTrue(plumeGraph.edgesOut(v12)[EdgeLabel.AST]?.contains(v13) ?: false)

            assertTrue(plumeGraph.edgesIn(v12)[EdgeLabel.AST]?.contains(v11) ?: false)
            assertTrue(plumeGraph.edgesIn(v13)[EdgeLabel.AST]?.contains(v12) ?: false)
            // Check method head
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v2) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v5) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v10) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.CFG]?.contains(v3) ?: false)

            assertTrue(plumeGraph.edgesIn(v2)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v5)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v3)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v10)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v3)[EdgeLabel.CFG]?.contains(v1) ?: false)
            // Check method body AST
            assertTrue(plumeGraph.edgesOut(v3)[EdgeLabel.AST]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.AST]?.contains(v6) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.AST]?.contains(v8) ?: false)
            assertTrue(plumeGraph.edgesOut(v3)[EdgeLabel.AST]?.contains(v9) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v10) ?: false)

            assertTrue(plumeGraph.edgesIn(v4)[EdgeLabel.AST]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesIn(v6)[EdgeLabel.AST]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v8)[EdgeLabel.AST]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v9)[EdgeLabel.AST]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesIn(v10)[EdgeLabel.AST]?.contains(v1) ?: false)
            // Check method body CFG
            assertTrue(plumeGraph.edgesOut(v3)[EdgeLabel.CFG]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.CFG]?.contains(v9) ?: false)
            assertTrue(plumeGraph.edgesOut(v9)[EdgeLabel.CFG]?.contains(v10) ?: false)

            assertTrue(plumeGraph.edgesIn(v4)[EdgeLabel.CFG]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesIn(v9)[EdgeLabel.CFG]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v10)[EdgeLabel.CFG]?.contains(v9) ?: false)
            // Check method body misc. edges
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.ARGUMENT]?.contains(v6) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.ARGUMENT]?.contains(v8) ?: false)
            assertTrue(plumeGraph.edgesOut(v6)[EdgeLabel.REF]?.contains(v5) ?: false)

            assertTrue(plumeGraph.edgesIn(v6)[EdgeLabel.ARGUMENT]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v8)[EdgeLabel.ARGUMENT]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v5)[EdgeLabel.REF]?.contains(v6) ?: false)
        }

        @Test
        fun testGetEmptyMethodBody() {
            driver.clearGraph()
            val plumeGraph = driver.getMethod(v1.fullName, v1.signature)
            assertEquals("PlumeGraph(vertices:0, edges:0)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(0, graphVertices.size)
        }

        @Test
        fun testGetMethodBody() {
            val plumeGraph = driver.getMethod(v1.fullName, v1.signature)
            assertEquals("PlumeGraph(vertices:9, edges:15)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(9, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertFalse(graphVertices.contains(v14))
            assertFalse(graphVertices.contains(v13))
            assertFalse(graphVertices.contains(v12))
            assertFalse(graphVertices.contains(v11))
            // Check method head
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v2) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v5) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v10) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.CFG]?.contains(v3) ?: false)

            assertTrue(plumeGraph.edgesIn(v2)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v5)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v3)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v10)[EdgeLabel.AST]?.contains(v1) ?: false)
            assertTrue(plumeGraph.edgesIn(v3)[EdgeLabel.CFG]?.contains(v1) ?: false)
            // Check method body AST
            assertTrue(plumeGraph.edgesOut(v3)[EdgeLabel.AST]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.AST]?.contains(v6) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.AST]?.contains(v8) ?: false)
            assertTrue(plumeGraph.edgesOut(v3)[EdgeLabel.AST]?.contains(v9) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.AST]?.contains(v10) ?: false)

            assertTrue(plumeGraph.edgesIn(v4)[EdgeLabel.AST]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesIn(v6)[EdgeLabel.AST]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v8)[EdgeLabel.AST]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v9)[EdgeLabel.AST]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesIn(v10)[EdgeLabel.AST]?.contains(v1) ?: false)
            // Check method body CFG
            assertTrue(plumeGraph.edgesOut(v3)[EdgeLabel.CFG]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.CFG]?.contains(v9) ?: false)
            assertTrue(plumeGraph.edgesOut(v9)[EdgeLabel.CFG]?.contains(v10) ?: false)

            assertTrue(plumeGraph.edgesIn(v4)[EdgeLabel.CFG]?.contains(v3) ?: false)
            assertTrue(plumeGraph.edgesIn(v9)[EdgeLabel.CFG]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v10)[EdgeLabel.CFG]?.contains(v9) ?: false)
            // Check method body misc. edges
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.ARGUMENT]?.contains(v6) ?: false)
            assertTrue(plumeGraph.edgesOut(v4)[EdgeLabel.ARGUMENT]?.contains(v8) ?: false)
            assertTrue(plumeGraph.edgesOut(v6)[EdgeLabel.REF]?.contains(v5) ?: false)

            assertTrue(plumeGraph.edgesIn(v6)[EdgeLabel.ARGUMENT]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v8)[EdgeLabel.ARGUMENT]?.contains(v4) ?: false)
            assertTrue(plumeGraph.edgesIn(v5)[EdgeLabel.REF]?.contains(v6) ?: false)
        }

        @Test
        fun testGetProgramStructure() {
            val plumeGraph = driver.getProgramStructure()
            assertEquals("PlumeGraph(vertices:3, edges:2)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(3, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertTrue(graphVertices.contains(v13))
            assertTrue(graphVertices.contains(v12))
            assertTrue(graphVertices.contains(v11))
            // Check that vertices are connected by AST edges
            assertTrue(plumeGraph.edgesOut(v11)[EdgeLabel.AST]?.contains(v12) ?: false)
            assertTrue(plumeGraph.edgesOut(v12)[EdgeLabel.AST]?.contains(v13) ?: false)

            assertTrue(plumeGraph.edgesIn(v12)[EdgeLabel.AST]?.contains(v11) ?: false)
            assertTrue(plumeGraph.edgesIn(v13)[EdgeLabel.AST]?.contains(v12) ?: false)
        }

        @Test
        fun testGetNeighbours() {
            val plumeGraph = driver.getNeighbours(v11)
            assertEquals("PlumeGraph(vertices:3, edges:2)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(3, graphVertices.size)
            // Check that vertices are connected by AST edges
            assertTrue(plumeGraph.edgesOut(v11)[EdgeLabel.AST]?.contains(v12) ?: false)
            assertTrue(plumeGraph.edgesOut(v1)[EdgeLabel.SOURCE_FILE]?.contains(v11) ?: false)

            assertTrue(plumeGraph.edgesIn(v12)[EdgeLabel.AST]?.contains(v11) ?: false)
            assertTrue(plumeGraph.edgesIn(v11)[EdgeLabel.SOURCE_FILE]?.contains(v1) ?: false)
        }
    }

    @Nested
    @DisplayName("Delete operation tests")
    inner class DriverDeleteTests {

        @BeforeEach
        fun setUp() {
            TestDomainResources.generateSimpleCPG(driver)
        }

        @Test
        fun testVertexDelete() {
            assertTrue(driver.exists(v1))
            driver.deleteVertex(v1)
            assertFalse(driver.exists(v1))
            // Try delete vertex which doesn't exist, should not throw error
            driver.deleteVertex(v1)
            assertFalse(driver.exists(v1))
            // Delete metadata
            assertTrue(driver.exists(v14))
            driver.deleteVertex(v14)
            assertFalse(driver.exists(v14))
        }

        @Test
        fun testMethodDelete() {
            assertTrue(driver.exists(v1))
            driver.deleteMethod(v1.fullName, v1.signature)
            assertFalse(driver.exists(v1))
            assertFalse(driver.exists(v8))
            assertFalse(driver.exists(v9))
            assertFalse(driver.exists(v10))
            assertFalse(driver.exists(v5))
            assertFalse(driver.exists(v3))
            assertFalse(driver.exists(v4))
            // Check that deleting a method doesn't throw any error
            driver.deleteMethod(v1.fullName, v1.signature)
        }
    }
}