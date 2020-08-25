package za.ac.sun.plume.drivers

import org.junit.jupiter.api.*
import za.ac.sun.plume.TestDomainResources
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.vertices.ArrayInitializerVertex
import za.ac.sun.plume.domain.models.vertices.BindingVertex
import za.ac.sun.plume.domain.models.vertices.MetaDataVertex
import za.ac.sun.plume.domain.models.vertices.TypeVertex

class JanusGraphDriverIntTest  {

    companion object {
        lateinit var driver: JanusGraphDriver
    }

    @BeforeEach
    fun setUp() {
        driver = DriverFactory(GraphDatabase.JANUS_GRAPH) as JanusGraphDriver
        driver.remoteConfig("src/test/resources/conf/remote-graph.properties")
        driver.connect()
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
        driver.close()
    }

    @Nested
    @DisplayName("Test driver vertex find and exist methods")
    inner class VertexAddAndExistsTests {
        @Test
        fun findAstVertex() {
            val v1 = ArrayInitializerVertex(TestDomainResources.INT_1)
            val v2 = ArrayInitializerVertex(TestDomainResources.INT_2)
            Assertions.assertFalse(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
        }

        @Test
        fun findBindingVertex() {
            val v1 = BindingVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            val v2 = BindingVertex(TestDomainResources.STRING_2, TestDomainResources.STRING_1)
            Assertions.assertFalse(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
        }

        @Test
        fun findMetaDataVertex() {
            val v1 = MetaDataVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            val v2 = MetaDataVertex(TestDomainResources.STRING_2, TestDomainResources.STRING_1)
            Assertions.assertFalse(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
        }

        @Test
        fun findTypeVertex() {
            val v1 = TypeVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2, TestDomainResources.STRING_2)
            val v2 = TypeVertex(TestDomainResources.STRING_2, TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            Assertions.assertFalse(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v1)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
            driver.addVertex(v2)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
        }
    }

    @Nested
    @DisplayName("Test driver edge find and exist methods")
    inner class EdgeAddAndExistsTests {
        private val v1 = ArrayInitializerVertex(TestDomainResources.INT_1)
        private val v2 = ArrayInitializerVertex(TestDomainResources.INT_2)

        @BeforeEach
        fun setUp() {
            Assertions.assertFalse(driver.exists(v1))
            Assertions.assertFalse(driver.exists(v2))
        }

        @Test
        fun testEdgeWhenVerticesAreAlreadyPresent() {
            driver.addVertex(v1)
            driver.addVertex(v2)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            driver.addEdge(v1, v2, EdgeLabel.AST)
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            driver.addEdge(v1, v2, EdgeLabel.AST)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
        }

        @Test
        fun testEdgeDirection() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.AST))
            Assertions.assertFalse(driver.exists(v2, v1, EdgeLabel.AST))
            driver.addEdge(v1, v2, EdgeLabel.AST)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.AST))
            Assertions.assertFalse(driver.exists(v2, v1, EdgeLabel.AST))
        }

        @Test
        fun testCfgEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.CFG))
            driver.addEdge(v1, v2, EdgeLabel.CFG)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.CFG))
        }

        @Test
        fun testCapturedByEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.CAPTURED_BY))
            driver.addEdge(v1, v2, EdgeLabel.CAPTURED_BY)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.CAPTURED_BY))
        }

        @Test
        fun testBindsToEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.BINDS_TO))
            driver.addEdge(v1, v2, EdgeLabel.BINDS_TO)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.REF))
            driver.addEdge(v1, v2, EdgeLabel.REF)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.RECEIVER))
            driver.addEdge(v1, v2, EdgeLabel.RECEIVER)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.CONDITION))
            driver.addEdge(v1, v2, EdgeLabel.CONDITION)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.BINDS))
            driver.addEdge(v1, v2, EdgeLabel.BINDS)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            Assertions.assertFalse(driver.exists(v1, v2, EdgeLabel.ARGUMENT))
            driver.addEdge(v1, v2, EdgeLabel.ARGUMENT)
            Assertions.assertTrue(driver.exists(v1))
            Assertions.assertTrue(driver.exists(v2))
            Assertions.assertTrue(driver.exists(v1, v2, EdgeLabel.ARGUMENT))
        }
    }

    @Nested
    @DisplayName("Max order tests")
    inner class MaxOrderTests {
        @Test
        fun testMaxOrderOnEmptyGraph() = Assertions.assertEquals(0, driver.maxOrder())

        @Test
        fun testMaxOrderOnGraphWithOneVertex() {
            val v1 = ArrayInitializerVertex(TestDomainResources.INT_2)
            driver.addVertex(v1)
            Assertions.assertEquals(TestDomainResources.INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithMoreThanOneVertex() {
            val v1 = ArrayInitializerVertex(TestDomainResources.INT_2)
            val v2 = MetaDataVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            driver.addVertex(v1)
            driver.addVertex(v2)
            Assertions.assertEquals(TestDomainResources.INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithNoAstVertex() {
            val v1 = BindingVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            val v2 = MetaDataVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            driver.addVertex(v1)
            driver.addVertex(v2)
            Assertions.assertEquals(0, driver.maxOrder())
        }
    }
}