package za.ac.sun.plume.drivers

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import za.ac.sun.plume.TestDomainResources
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
import za.ac.sun.plume.domain.models.vertices.ArrayInitializerVertex
import za.ac.sun.plume.domain.models.vertices.BindingVertex
import za.ac.sun.plume.domain.models.vertices.MetaDataVertex
import za.ac.sun.plume.domain.models.vertices.TypeVertex
import kotlin.properties.Delegates

class OverflowDbDriverIntTest {

    companion object {
        private var testStartTime by Delegates.notNull<Long>()
        lateinit var driver: OverflowDbDriver

        @JvmStatic
        @BeforeAll
        fun setUpAll() = run { testStartTime = System.nanoTime() }

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            println("${OverflowDbDriverIntTest::class.java.simpleName} completed in ${(System.nanoTime() - testStartTime) / 1e6} ms")
        }
    }

    @BeforeEach
    fun setUp() {
        driver = (DriverFactory(GraphDatabase.OVERFLOWDB) as OverflowDbDriver).apply { connect() }
    }

    @AfterEach
    fun tearDown() = driver.clearGraph().close()

    @Nested
    @DisplayName("Test driver vertex find and exist methods")
    inner class VertexAddAndExistsTests {
        @Test
        fun findAstVertex() {
            val v1 = ArrayInitializerVertex(TestDomainResources.INT_1)
            val v2 = ArrayInitializerVertex(TestDomainResources.INT_2)
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
            val v1 = BindingVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            val v2 = BindingVertex(TestDomainResources.STRING_2, TestDomainResources.STRING_1)
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
            val v1 = MetaDataVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            val v2 = MetaDataVertex(TestDomainResources.STRING_2, TestDomainResources.STRING_1)
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
            val v1 =
                    TypeVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2, TestDomainResources.STRING_2)
            val v2 =
                    TypeVertex(TestDomainResources.STRING_2, TestDomainResources.STRING_1, TestDomainResources.STRING_2)
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
            assertFalse(driver.exists(TestDomainResources.v8))
            assertFalse(driver.exists(TestDomainResources.v6))
        }

        @Test
        fun testEdgeWhenVerticesAreAlreadyPresent() {
            driver.addVertex(TestDomainResources.v7)
            driver.addVertex(TestDomainResources.v19)
            assertTrue(driver.exists(TestDomainResources.v7))
            assertTrue(driver.exists(TestDomainResources.v19))
            assertFalse(driver.exists(TestDomainResources.v7, TestDomainResources.v19, EdgeLabel.AST))
            driver.addEdge(TestDomainResources.v7, TestDomainResources.v19, EdgeLabel.AST)
            assertTrue(driver.exists(TestDomainResources.v7, TestDomainResources.v19, EdgeLabel.AST))
        }

        @Test
        fun testEdgeWhenItWillViolateSchema() {
            driver.addVertex(TestDomainResources.v8)
            driver.addVertex(TestDomainResources.v6)
            assertTrue(driver.exists(TestDomainResources.v8))
            assertTrue(driver.exists(TestDomainResources.v6))
            assertFalse(driver.exists(TestDomainResources.v8, TestDomainResources.v6, EdgeLabel.AST))
            assertThrows(PlumeSchemaViolationException::class.java) {
                driver.addEdge(
                        TestDomainResources.v8,
                        TestDomainResources.v6,
                        EdgeLabel.AST
                )
            }
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            assertFalse(driver.exists(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.AST))
            driver.addEdge(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.AST)
            assertTrue(driver.exists(TestDomainResources.v4))
            assertTrue(driver.exists(TestDomainResources.v6))
            assertTrue(driver.exists(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.AST))
        }

        @Test
        fun testEdgeDirection() {
            assertFalse(driver.exists(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.AST))
            assertFalse(driver.exists(TestDomainResources.v6, TestDomainResources.v4, EdgeLabel.AST))
            driver.addEdge(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.AST)
            assertTrue(driver.exists(TestDomainResources.v4))
            assertTrue(driver.exists(TestDomainResources.v6))
            assertTrue(driver.exists(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.AST))
            assertFalse(driver.exists(TestDomainResources.v6, TestDomainResources.v4, EdgeLabel.AST))
        }

        @Test
        fun testCfgEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v8, TestDomainResources.v6, EdgeLabel.CFG))
            driver.addEdge(TestDomainResources.v8, TestDomainResources.v6, EdgeLabel.CFG)
            assertTrue(driver.exists(TestDomainResources.v8))
            assertTrue(driver.exists(TestDomainResources.v6))
            assertTrue(driver.exists(TestDomainResources.v8, TestDomainResources.v6, EdgeLabel.CFG))
        }

        @Test
        fun testCapturedByEdgeCreation() {
            // TODO CAPTURED_BY edges from Local to Binding are not permited. Commenting
            // out this test for now
//            assertFalse(
//                    driver.exists(
//                            TestDomainResources.v5,
//                            TestDomainResources.v17,
//                            EdgeLabel.CAPTURED_BY
//                    )
//            )
//            driver.addEdge(TestDomainResources.v5, TestDomainResources.v17, EdgeLabel.CAPTURED_BY)
//            assertTrue(driver.exists(TestDomainResources.v17))
//            assertTrue(driver.exists(TestDomainResources.v5))
//            assertTrue(driver.exists(TestDomainResources.v5, TestDomainResources.v17, EdgeLabel.CAPTURED_BY))
        }

        @Test
        fun testBindsToEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v18, TestDomainResources.v19, EdgeLabel.BINDS_TO))
            driver.addEdge(TestDomainResources.v18, TestDomainResources.v19, EdgeLabel.BINDS_TO)
            assertTrue(driver.exists(TestDomainResources.v18))
            assertTrue(driver.exists(TestDomainResources.v19))
            assertTrue(driver.exists(TestDomainResources.v18, TestDomainResources.v19, EdgeLabel.BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v17, TestDomainResources.v1, EdgeLabel.REF))
            driver.addEdge(TestDomainResources.v17, TestDomainResources.v1, EdgeLabel.REF)
            assertTrue(driver.exists(TestDomainResources.v17))
            assertTrue(driver.exists(TestDomainResources.v1))
            assertTrue(driver.exists(TestDomainResources.v17, TestDomainResources.v1, EdgeLabel.REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.RECEIVER))
            driver.addEdge(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.RECEIVER)
            assertTrue(driver.exists(TestDomainResources.v4))
            assertTrue(driver.exists(TestDomainResources.v6))
            assertTrue(driver.exists(TestDomainResources.v4, TestDomainResources.v6, EdgeLabel.RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v15, TestDomainResources.v16, EdgeLabel.CONDITION))
            driver.addEdge(TestDomainResources.v15, TestDomainResources.v16, EdgeLabel.CONDITION)
            assertTrue(driver.exists(TestDomainResources.v15))
            assertTrue(driver.exists(TestDomainResources.v16))
            assertTrue(driver.exists(TestDomainResources.v15, TestDomainResources.v16, EdgeLabel.CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v7, TestDomainResources.v17, EdgeLabel.BINDS))
            driver.addEdge(TestDomainResources.v7, TestDomainResources.v17, EdgeLabel.BINDS)
            assertTrue(driver.exists(TestDomainResources.v7))
            assertTrue(driver.exists(TestDomainResources.v17))
            assertTrue(driver.exists(TestDomainResources.v7, TestDomainResources.v17, EdgeLabel.BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            assertFalse(driver.exists(TestDomainResources.v4, TestDomainResources.v16, EdgeLabel.ARGUMENT))
            driver.addEdge(TestDomainResources.v4, TestDomainResources.v16, EdgeLabel.ARGUMENT)
            assertTrue(driver.exists(TestDomainResources.v4))
            assertTrue(driver.exists(TestDomainResources.v4))
            assertTrue(driver.exists(TestDomainResources.v4, TestDomainResources.v16, EdgeLabel.ARGUMENT))
        }

        @Test
        fun testSourceFileEdgeCreation() {
            assertFalse(
                    driver.exists(
                            TestDomainResources.v1,
                            TestDomainResources.v11,
                            EdgeLabel.SOURCE_FILE
                    )
            )
            driver.addEdge(TestDomainResources.v1, TestDomainResources.v11, EdgeLabel.SOURCE_FILE)
            assertTrue(driver.exists(TestDomainResources.v1))
            assertTrue(driver.exists(TestDomainResources.v11))
            assertTrue(driver.exists(TestDomainResources.v1, TestDomainResources.v11, EdgeLabel.SOURCE_FILE))
        }
    }

    @Nested
    @DisplayName("Max order tests")
    inner class MaxOrderTests {
        @Test
        fun testMaxOrderOnEmptyGraph() = assertEquals(0, driver.maxOrder())

        @Test
        fun testMaxOrderOnGraphWithOneVertex() {
            val v1 = ArrayInitializerVertex(TestDomainResources.INT_2)
            driver.addVertex(v1)
            assertEquals(TestDomainResources.INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithMoreThanOneVertex() {
            val v1 = ArrayInitializerVertex(TestDomainResources.INT_2)
            val v2 = MetaDataVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            driver.addVertex(v1)
            driver.addVertex(v2)
            assertEquals(TestDomainResources.INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithNoAstVertex() {
            val v1 = BindingVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
            val v2 = MetaDataVertex(TestDomainResources.STRING_1, TestDomainResources.STRING_2)
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
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v11)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v12
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v12)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v13
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v12)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v11
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v13)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v12
                    ) ?: false
            )
            // Check method head
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v2
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v5
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v2)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v5)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v3)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v10)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v3)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            // Check method body AST
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v3)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v6
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v8
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v3)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v9
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v4)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v6)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v8)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v9)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v10)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            // Check method body CFG
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v3)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v9
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v9)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v4)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v9)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v10)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v9
                    ) ?: false
            )
            // Check method body misc. edges
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v6
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v8
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v6)[EdgeLabel.REF]?.contains(
                            TestDomainResources.v5
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v6)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v8)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v5)[EdgeLabel.REF]?.contains(
                            TestDomainResources.v6
                    ) ?: false
            )
        }

        @Test
        fun testGetEmptyMethodBody() {
            driver.clearGraph()
            val plumeGraph = driver.getMethod(TestDomainResources.v1.fullName, TestDomainResources.v1.signature)
            assertEquals("PlumeGraph(vertices:0, edges:0)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(0, graphVertices.size)
        }

        @Test
        fun testGetMethodHeadOnly() {
            val plumeGraph = driver.getMethod(TestDomainResources.v1.fullName, TestDomainResources.v1.signature, false)
            assertEquals("PlumeGraph(vertices:5, edges:4)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(5, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertFalse(graphVertices.contains(TestDomainResources.v14))
            assertFalse(graphVertices.contains(TestDomainResources.v13))
            assertFalse(graphVertices.contains(TestDomainResources.v12))
            assertFalse(graphVertices.contains(TestDomainResources.v11))
            // Check method head
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v2
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v5
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )
            // Check that none of the other vertices exist
            assertFalse(graphVertices.contains(TestDomainResources.v4))
            assertFalse(graphVertices.contains(TestDomainResources.v6))
            assertFalse(graphVertices.contains(TestDomainResources.v7))
            assertFalse(graphVertices.contains(TestDomainResources.v8))
            assertFalse(graphVertices.contains(TestDomainResources.v9))
        }

        @Test
        fun testGetMethodBody() {
            val plumeGraph = driver.getMethod(TestDomainResources.v1.fullName, TestDomainResources.v1.signature, true)
            assertEquals("PlumeGraph(vertices:9, edges:15)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(9, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertFalse(graphVertices.contains(TestDomainResources.v14))
            assertFalse(graphVertices.contains(TestDomainResources.v13))
            assertFalse(graphVertices.contains(TestDomainResources.v12))
            assertFalse(graphVertices.contains(TestDomainResources.v11))
            // Check method head
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v2
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v5
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v2)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v5)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v3)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v10)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v3)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            println(plumeGraph.edgesOut(TestDomainResources.v3))
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v3)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v6
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v8
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v3)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v9
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v4)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v6)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v8)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v9)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v10)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
            // Check method body CFG
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v3)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v9
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v9)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v10
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v4)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v3
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v9)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v10)[EdgeLabel.CFG]?.contains(
                            TestDomainResources.v9
                    ) ?: false
            )
            // Check method body misc. edges
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v6
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v4)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v8
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v6)[EdgeLabel.REF]?.contains(
                            TestDomainResources.v5
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v6)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v8)[EdgeLabel.ARGUMENT]?.contains(
                            TestDomainResources.v4
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v5)[EdgeLabel.REF]?.contains(
                            TestDomainResources.v6
                    ) ?: false
            )
        }

        @Test
        fun testGetProgramStructure() {
            val plumeGraph = driver.getProgramStructure()
            assertEquals("PlumeGraph(vertices:3, edges:2)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(3, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertTrue(graphVertices.contains(TestDomainResources.v13))
            assertTrue(graphVertices.contains(TestDomainResources.v12))
            assertTrue(graphVertices.contains(TestDomainResources.v11))
            // Check that vertices are connected by AST edges
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v11)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v12
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v12)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v13
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v12)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v11
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v13)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v12
                    ) ?: false
            )
        }

        @Test
        fun testGetNeighbours() {
            val plumeGraph = driver.getNeighbours(TestDomainResources.v11)
            assertEquals("PlumeGraph(vertices:3, edges:2)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(3, graphVertices.size)
            // Check that vertices are connected by AST edges
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v11)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v12
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesOut(TestDomainResources.v1)[EdgeLabel.SOURCE_FILE]?.contains(
                            TestDomainResources.v11
                    ) ?: false
            )

            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v12)[EdgeLabel.AST]?.contains(
                            TestDomainResources.v11
                    ) ?: false
            )
            assertTrue(
                    plumeGraph.edgesIn(TestDomainResources.v11)[EdgeLabel.SOURCE_FILE]?.contains(
                            TestDomainResources.v1
                    ) ?: false
            )
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
            assertTrue(driver.exists(TestDomainResources.v1))
            driver.deleteVertex(TestDomainResources.v1)
            assertFalse(driver.exists(TestDomainResources.v1))
            // Try delete vertex which doesn't exist, should not throw error
            driver.deleteVertex(TestDomainResources.v1)
            assertFalse(driver.exists(TestDomainResources.v1))
            // Delete metadata
            assertTrue(driver.exists(TestDomainResources.v14))
            driver.deleteVertex(TestDomainResources.v14)
            assertFalse(driver.exists(TestDomainResources.v14))
        }

        @Test
        fun testMethodDelete() {
            assertTrue(driver.exists(TestDomainResources.v1))
            driver.deleteMethod(TestDomainResources.v1.fullName, TestDomainResources.v1.signature)
            assertFalse(driver.exists(TestDomainResources.v1))
            assertFalse(driver.exists(TestDomainResources.v8))
            assertFalse(driver.exists(TestDomainResources.v9))
            assertFalse(driver.exists(TestDomainResources.v10))
            assertFalse(driver.exists(TestDomainResources.v5))
            assertFalse(driver.exists(TestDomainResources.v3))
            assertFalse(driver.exists(TestDomainResources.v4))
            // Check that deleting a method doesn't throw any error
            driver.deleteMethod(TestDomainResources.v1.fullName, TestDomainResources.v1.signature)
        }
    }
}
