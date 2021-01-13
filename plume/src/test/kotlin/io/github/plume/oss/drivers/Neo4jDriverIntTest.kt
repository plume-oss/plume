package io.github.plume.oss.drivers

import io.github.plume.oss.TestDomainResources.Companion.INT_1
import io.github.plume.oss.TestDomainResources.Companion.INT_2
import io.github.plume.oss.TestDomainResources.Companion.STRING_1
import io.github.plume.oss.TestDomainResources.Companion.STRING_2
import io.github.plume.oss.TestDomainResources.Companion.bindingVertex
import io.github.plume.oss.TestDomainResources.Companion.blockVertex
import io.github.plume.oss.TestDomainResources.Companion.callVertex
import io.github.plume.oss.TestDomainResources.Companion.controlStructureVertex
import io.github.plume.oss.TestDomainResources.Companion.fieldIdentifierVertex
import io.github.plume.oss.TestDomainResources.Companion.fileVertex
import io.github.plume.oss.TestDomainResources.Companion.generateSimpleCPG
import io.github.plume.oss.TestDomainResources.Companion.identifierVertex
import io.github.plume.oss.TestDomainResources.Companion.jumpTargetVertex
import io.github.plume.oss.TestDomainResources.Companion.literalVertex
import io.github.plume.oss.TestDomainResources.Companion.localVertex
import io.github.plume.oss.TestDomainResources.Companion.metaDataVertex
import io.github.plume.oss.TestDomainResources.Companion.methodParameterInVertex
import io.github.plume.oss.TestDomainResources.Companion.methodRefVertex
import io.github.plume.oss.TestDomainResources.Companion.methodReturnVertex
import io.github.plume.oss.TestDomainResources.Companion.methodVertex
import io.github.plume.oss.TestDomainResources.Companion.modifierVertex
import io.github.plume.oss.TestDomainResources.Companion.namespaceBlockVertex1
import io.github.plume.oss.TestDomainResources.Companion.namespaceBlockVertex2
import io.github.plume.oss.TestDomainResources.Companion.returnVertex
import io.github.plume.oss.TestDomainResources.Companion.typeArgumentVertex
import io.github.plume.oss.TestDomainResources.Companion.typeDeclVertex
import io.github.plume.oss.TestDomainResources.Companion.typeParameterVertex
import io.github.plume.oss.TestDomainResources.Companion.typeRefVertex
import io.github.plume.oss.TestDomainResources.Companion.unknownVertex
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import scala.Option
import kotlin.properties.Delegates

class Neo4jDriverIntTest {

    companion object {
        lateinit var driver: Neo4jDriver
        private var testStartTime by Delegates.notNull<Long>()

        @JvmStatic
        @BeforeAll
        fun setUpAll() = run { testStartTime = System.nanoTime() }

        @JvmStatic
        @AfterAll
        fun tearDownAll() =
            println("${Neo4jDriverIntTest::class.java.simpleName} completed in ${(System.nanoTime() - testStartTime) / 1e6} ms")
    }

    @BeforeEach
    fun setUp() {
        driver = (DriverFactory(GraphDatabase.NEO4J) as Neo4jDriver).apply {
            this.hostname("localhost")
                .port(7687)
                .username("neo4j")
                .password("neo4j123")
                .database("neo4j")
                .connect()
        }
        assertEquals("localhost", driver.hostname)
        assertEquals(7687, driver.port)
        assertEquals("neo4j", driver.username)
        assertEquals("neo4j123", driver.password)
        assertEquals("neo4j", driver.database)
    }

    @AfterEach
    fun tearDown() = driver.clearGraph().close()

    @Nested
    @DisplayName("Test driver vertex find and exist methods")
    inner class VertexAddAndExistsTests {
        @Test
        fun findAstVertex() {
            val v1 = NewArrayInitializerBuilder().order(INT_1).build()
            val v2 = NewArrayInitializerBuilder().order(INT_2).build()
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
            val v1 = NewBindingBuilder().name(STRING_1).signature(STRING_2).build()
            val v2 = NewBindingBuilder().name(STRING_2).signature(STRING_1).build()
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
        fun findFieldIdentifierVertex() {
            val v1 = NewFieldIdentifierBuilder().canonicalname(STRING_1).code(STRING_2).argumentindex(INT_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
            val v2 = NewFieldIdentifierBuilder().canonicalname(STRING_2).code(STRING_1).argumentindex(INT_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
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
            val v1 = NewMetaDataBuilder().language(STRING_1).version(STRING_2).build()
            val v2 = NewMetaDataBuilder().language(STRING_2).version(STRING_1).build()
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
        fun findMethodRefVertex() {
            val v1 = NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_1)).methodfullname(STRING_2)
                .code(STRING_1).order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build()
            val v2 = NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_2)).methodfullname(STRING_1)
                .code(STRING_1).order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build()
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
            val v1 = NewTypeBuilder().name(STRING_1).fullname(STRING_2).typedeclfullname(STRING_2).build()
            val v2 = NewTypeBuilder().name(STRING_2).fullname(STRING_1).typedeclfullname(STRING_2).build()
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
        fun findTypeRefVertex() {
            val v1 = NewTypeRefBuilder().typefullname(STRING_1).dynamictypehintfullname(
                SootToPlumeUtil.createSingleItemScalaList(
                    STRING_2
                ) as scala.collection.immutable.List<String>
            ).code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build()
            val v2 = NewTypeRefBuilder().typefullname(STRING_2).dynamictypehintfullname(
                SootToPlumeUtil.createSingleItemScalaList(
                    STRING_1
                ) as scala.collection.immutable.List<String>
            ).code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build()
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
        fun findUnknownVertex() {
            val v1 = NewUnknownBuilder().typefullname(STRING_1).code(STRING_2).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
            val v2 = NewUnknownBuilder().typefullname(STRING_2).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
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
            assertFalse(driver.exists(literalVertex))
            assertFalse(driver.exists(identifierVertex))
        }

        @Test
        fun testEdgeWhenVerticesAreAlreadyPresent() {
            driver.addVertex(typeDeclVertex)
            driver.addVertex(typeParameterVertex)
            assertTrue(driver.exists(typeDeclVertex))
            assertTrue(driver.exists(typeParameterVertex))
            assertFalse(driver.exists(typeDeclVertex, typeParameterVertex, EdgeLabel.AST))
            driver.addEdge(typeDeclVertex, typeParameterVertex, EdgeLabel.AST)
            assertTrue(driver.exists(typeDeclVertex, typeParameterVertex, EdgeLabel.AST))
        }

        @Test
        fun testEdgeWhenItWillViolateSchema() {
            driver.addVertex(literalVertex)
            driver.addVertex(identifierVertex)
            assertTrue(driver.exists(literalVertex))
            assertTrue(driver.exists(identifierVertex))
            assertFalse(driver.exists(literalVertex, identifierVertex, EdgeLabel.AST))
            assertThrows(PlumeSchemaViolationException::class.java) {
                driver.addEdge(
                    literalVertex,
                    identifierVertex,
                    EdgeLabel.AST
                )
            }
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            assertFalse(driver.exists(callVertex, identifierVertex, EdgeLabel.AST))
            driver.addEdge(callVertex, identifierVertex, EdgeLabel.AST)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(callVertex, identifierVertex, EdgeLabel.AST))
        }

        @Test
        fun testEdgeDirection() {
            assertFalse(driver.exists(callVertex, identifierVertex, EdgeLabel.AST))
            assertFalse(driver.exists(identifierVertex, callVertex, EdgeLabel.AST))
            driver.addEdge(callVertex, identifierVertex, EdgeLabel.AST)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(callVertex, identifierVertex, EdgeLabel.AST))
            assertFalse(driver.exists(identifierVertex, callVertex, EdgeLabel.AST))
        }

        @Test
        fun testCfgEdgeCreation() {
            assertFalse(driver.exists(literalVertex, identifierVertex, EdgeLabel.CFG))
            driver.addEdge(literalVertex, identifierVertex, EdgeLabel.CFG)
            assertTrue(driver.exists(literalVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(literalVertex, identifierVertex, EdgeLabel.CFG))
        }

        @Test
        fun testCapturedByEdgeCreation() {
            assertFalse(driver.exists(localVertex, bindingVertex, EdgeLabel.CAPTURED_BY))
            driver.addEdge(localVertex, bindingVertex, EdgeLabel.CAPTURED_BY)
            assertTrue(driver.exists(bindingVertex))
            assertTrue(driver.exists(localVertex))
            assertTrue(driver.exists(localVertex, bindingVertex, EdgeLabel.CAPTURED_BY))
        }

        @Test
        fun testBindsToEdgeCreation() {
            assertFalse(driver.exists(typeArgumentVertex, typeParameterVertex, EdgeLabel.BINDS_TO))
            driver.addEdge(typeArgumentVertex, typeParameterVertex, EdgeLabel.BINDS_TO)
            assertTrue(driver.exists(typeArgumentVertex))
            assertTrue(driver.exists(typeParameterVertex))
            assertTrue(driver.exists(typeArgumentVertex, typeParameterVertex, EdgeLabel.BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            assertFalse(driver.exists(bindingVertex, methodVertex, EdgeLabel.REF))
            driver.addEdge(bindingVertex, methodVertex, EdgeLabel.REF)
            assertTrue(driver.exists(bindingVertex))
            assertTrue(driver.exists(methodVertex))
            assertTrue(driver.exists(bindingVertex, methodVertex, EdgeLabel.REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            assertFalse(driver.exists(callVertex, identifierVertex, EdgeLabel.RECEIVER))
            driver.addEdge(callVertex, identifierVertex, EdgeLabel.RECEIVER)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(callVertex, identifierVertex, EdgeLabel.RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            assertFalse(driver.exists(controlStructureVertex, jumpTargetVertex, EdgeLabel.CONDITION))
            driver.addEdge(controlStructureVertex, jumpTargetVertex, EdgeLabel.CONDITION)
            assertTrue(driver.exists(controlStructureVertex))
            assertTrue(driver.exists(jumpTargetVertex))
            assertTrue(driver.exists(controlStructureVertex, jumpTargetVertex, EdgeLabel.CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            assertFalse(driver.exists(typeDeclVertex, bindingVertex, EdgeLabel.BINDS))
            driver.addEdge(typeDeclVertex, bindingVertex, EdgeLabel.BINDS)
            assertTrue(driver.exists(typeDeclVertex))
            assertTrue(driver.exists(bindingVertex))
            assertTrue(driver.exists(typeDeclVertex, bindingVertex, EdgeLabel.BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            assertFalse(driver.exists(callVertex, jumpTargetVertex, EdgeLabel.ARGUMENT))
            driver.addEdge(callVertex, jumpTargetVertex, EdgeLabel.ARGUMENT)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(callVertex, jumpTargetVertex, EdgeLabel.ARGUMENT))
        }

        @Test
        fun testSourceFileEdgeCreation() {
            assertFalse(driver.exists(methodVertex, fileVertex, EdgeLabel.SOURCE_FILE))
            driver.addEdge(methodVertex, fileVertex, EdgeLabel.SOURCE_FILE)
            assertTrue(driver.exists(methodVertex))
            assertTrue(driver.exists(fileVertex))
            assertTrue(driver.exists(methodVertex, fileVertex, EdgeLabel.SOURCE_FILE))
        }
    }

    @Nested
    @DisplayName("Max order tests")
    inner class MaxOrderTests {
        @Test
        fun testMaxOrderOnEmptyGraph() = assertEquals(0, driver.maxOrder())

        @Test
        fun testMaxOrderOnGraphWithOneVertex() {
            val v1 = NewArrayInitializerBuilder().order(INT_2).build()
            driver.addVertex(v1)
            assertEquals(INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithMoreThanOneVertex() {
            val v1 = NewArrayInitializerBuilder().order(INT_2).build()
            val v2 = NewMetaDataBuilder().language(STRING_1).version(STRING_2).build()
            driver.addVertex(v1)
            driver.addVertex(v2)
            assertEquals(INT_2, driver.maxOrder())
        }

        @Test
        fun testMaxOrderOnGraphWithNoAstVertex() {
            val v1 = NewBindingBuilder().name(STRING_1).signature(STRING_2).build()
            val v2 = NewMetaDataBuilder().language(STRING_1).version(STRING_2).build()
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
            generateSimpleCPG(driver)
        }

        @Test
        fun testGetWholeGraph() {
            val plumeGraph = driver.getWholeGraph()
            assertEquals("PlumeGraph(vertices:21, edges:30)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(21, graphVertices.size)
            // Check program structure
            assertTrue(plumeGraph.edgesOut(fileVertex)[EdgeLabel.AST]?.contains(namespaceBlockVertex1) ?: false)
            assertTrue(
                plumeGraph.edgesOut(namespaceBlockVertex1)[EdgeLabel.AST]?.contains(namespaceBlockVertex2) ?: false
            )

            assertTrue(plumeGraph.edgesIn(namespaceBlockVertex1)[EdgeLabel.AST]?.contains(fileVertex) ?: false)
            assertTrue(
                plumeGraph.edgesIn(namespaceBlockVertex2)[EdgeLabel.AST]?.contains(namespaceBlockVertex1) ?: false
            )
            // Check method head
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodParameterInVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(localVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodReturnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.CFG]?.contains(blockVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(methodParameterInVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(localVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(blockVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(methodReturnVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(blockVertex)[EdgeLabel.CFG]?.contains(methodVertex) ?: false)
            // Check method body AST
            assertTrue(plumeGraph.edgesOut(blockVertex)[EdgeLabel.AST]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.AST]?.contains(identifierVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.AST]?.contains(literalVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(blockVertex)[EdgeLabel.AST]?.contains(returnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodReturnVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(callVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(identifierVertex)[EdgeLabel.AST]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(literalVertex)[EdgeLabel.AST]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(returnVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(methodReturnVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            // Check method body CFG
            assertTrue(plumeGraph.edgesOut(blockVertex)[EdgeLabel.CFG]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.CFG]?.contains(fieldIdentifierVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(returnVertex)[EdgeLabel.CFG]?.contains(methodReturnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(fieldIdentifierVertex)[EdgeLabel.CFG]?.contains(methodRefVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodRefVertex)[EdgeLabel.CFG]?.contains(typeRefVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(typeRefVertex)[EdgeLabel.CFG]?.contains(controlStructureVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(controlStructureVertex)[EdgeLabel.CFG]?.contains(jumpTargetVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(jumpTargetVertex)[EdgeLabel.CFG]?.contains(returnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(returnVertex)[EdgeLabel.CFG]?.contains(methodReturnVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(callVertex)[EdgeLabel.CFG]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(returnVertex)[EdgeLabel.CFG]?.contains(jumpTargetVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(methodReturnVertex)[EdgeLabel.CFG]?.contains(returnVertex) ?: false)
            // Check method body misc. edges
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.ARGUMENT]?.contains(identifierVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.ARGUMENT]?.contains(literalVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(identifierVertex)[EdgeLabel.REF]?.contains(localVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(identifierVertex)[EdgeLabel.ARGUMENT]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(literalVertex)[EdgeLabel.ARGUMENT]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(localVertex)[EdgeLabel.REF]?.contains(identifierVertex) ?: false)
            assertTrue(plumeGraph.vertices().contains(unknownVertex))
        }

        @Test
        fun testGetEmptyMethodBody() {
            driver.clearGraph()
            val plumeGraph = driver.getMethod(methodVertex.fullName(), methodVertex.signature())
            assertEquals("PlumeGraph(vertices:0, edges:0)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(0, graphVertices.size)
        }

        @Test
        fun testGetMethodHeadOnly() {
            val plumeGraph = driver.getMethod(methodVertex.fullName(), methodVertex.signature(), false)
            assertEquals("PlumeGraph(vertices:6, edges:5)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(6, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertFalse(graphVertices.contains(metaDataVertex))
            assertFalse(graphVertices.contains(namespaceBlockVertex2))
            assertFalse(graphVertices.contains(namespaceBlockVertex1))
            assertFalse(graphVertices.contains(fileVertex))
            // Check method head
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodParameterInVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(localVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodReturnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(modifierVertex) ?: false)
            // Check that none of the other vertices exist
            assertFalse(graphVertices.contains(callVertex))
            assertFalse(graphVertices.contains(identifierVertex))
            assertFalse(graphVertices.contains(typeDeclVertex))
            assertFalse(graphVertices.contains(literalVertex))
            assertFalse(graphVertices.contains(returnVertex))
        }

        @Test
        fun testGetMethodBody() {
            val plumeGraph = driver.getMethod(methodVertex.fullName(), methodVertex.signature(), true)
            assertEquals("PlumeGraph(vertices:15, edges:26)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(15, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertFalse(graphVertices.contains(metaDataVertex))
            assertFalse(graphVertices.contains(namespaceBlockVertex2))
            assertFalse(graphVertices.contains(namespaceBlockVertex1))
            assertFalse(graphVertices.contains(fileVertex))
            // Check method head
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodParameterInVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(localVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodReturnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.CFG]?.contains(blockVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(methodParameterInVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(localVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(blockVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(methodReturnVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(blockVertex)[EdgeLabel.CFG]?.contains(methodVertex) ?: false)

            assertTrue(plumeGraph.edgesOut(blockVertex)[EdgeLabel.AST]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.AST]?.contains(identifierVertex) ?: false)

            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.AST]?.contains(literalVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(blockVertex)[EdgeLabel.AST]?.contains(returnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.AST]?.contains(methodReturnVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(callVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(identifierVertex)[EdgeLabel.AST]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(literalVertex)[EdgeLabel.AST]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(returnVertex)[EdgeLabel.AST]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(methodReturnVertex)[EdgeLabel.AST]?.contains(methodVertex) ?: false)
            // Check method body CFG
            assertTrue(plumeGraph.edgesOut(blockVertex)[EdgeLabel.CFG]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.CFG]?.contains(fieldIdentifierVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(returnVertex)[EdgeLabel.CFG]?.contains(methodReturnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(fieldIdentifierVertex)[EdgeLabel.CFG]?.contains(methodRefVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(methodRefVertex)[EdgeLabel.CFG]?.contains(typeRefVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(typeRefVertex)[EdgeLabel.CFG]?.contains(controlStructureVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(controlStructureVertex)[EdgeLabel.CFG]?.contains(jumpTargetVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(jumpTargetVertex)[EdgeLabel.CFG]?.contains(returnVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(returnVertex)[EdgeLabel.CFG]?.contains(methodReturnVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(callVertex)[EdgeLabel.CFG]?.contains(blockVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(returnVertex)[EdgeLabel.CFG]?.contains(jumpTargetVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(methodReturnVertex)[EdgeLabel.CFG]?.contains(returnVertex) ?: false)
            // Check method body misc. edges
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.ARGUMENT]?.contains(identifierVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(callVertex)[EdgeLabel.ARGUMENT]?.contains(literalVertex) ?: false)
            assertTrue(plumeGraph.edgesOut(identifierVertex)[EdgeLabel.REF]?.contains(localVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(identifierVertex)[EdgeLabel.ARGUMENT]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(literalVertex)[EdgeLabel.ARGUMENT]?.contains(callVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(localVertex)[EdgeLabel.REF]?.contains(identifierVertex) ?: false)
        }

        @Test
        fun testGetProgramStructure() {
            val plumeGraph = driver.getProgramStructure()
            assertEquals("PlumeGraph(vertices:3, edges:2)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(3, graphVertices.size)
            // Assert no program structure vertices part of the method body
            assertTrue(graphVertices.contains(namespaceBlockVertex2))
            assertTrue(graphVertices.contains(namespaceBlockVertex1))
            assertTrue(graphVertices.contains(fileVertex))
            // Check that vertices are connected by AST edges
            assertTrue(plumeGraph.edgesOut(fileVertex)[EdgeLabel.AST]?.contains(namespaceBlockVertex1) ?: false)
            assertTrue(
                plumeGraph.edgesOut(namespaceBlockVertex1)[EdgeLabel.AST]?.contains(namespaceBlockVertex2) ?: false
            )

            assertTrue(plumeGraph.edgesIn(namespaceBlockVertex1)[EdgeLabel.AST]?.contains(fileVertex) ?: false)
            assertTrue(
                plumeGraph.edgesIn(namespaceBlockVertex2)[EdgeLabel.AST]?.contains(namespaceBlockVertex1) ?: false
            )
        }

        @Test
        fun testGetNeighbours() {
            val plumeGraph = driver.getNeighbours(fileVertex)
            assertEquals("PlumeGraph(vertices:3, edges:2)", plumeGraph.toString())
            val graphVertices = plumeGraph.vertices()
            assertEquals(3, graphVertices.size)
            // Check that vertices are connected by AST edges
            assertTrue(plumeGraph.edgesOut(fileVertex)[EdgeLabel.AST]?.contains(namespaceBlockVertex1) ?: false)
            assertTrue(plumeGraph.edgesOut(methodVertex)[EdgeLabel.SOURCE_FILE]?.contains(fileVertex) ?: false)

            assertTrue(plumeGraph.edgesIn(namespaceBlockVertex1)[EdgeLabel.AST]?.contains(fileVertex) ?: false)
            assertTrue(plumeGraph.edgesIn(fileVertex)[EdgeLabel.SOURCE_FILE]?.contains(methodVertex) ?: false)
        }
    }

    @Nested
    @DisplayName("Delete operation tests")
    inner class DriverDeleteTests {

        @BeforeEach
        fun setUp() {
            generateSimpleCPG(driver)
        }

        @Test
        fun testVertexDelete() {
            assertTrue(driver.exists(methodVertex))
            driver.deleteVertex(methodVertex)
            assertFalse(driver.exists(methodVertex))
            // Try delete vertex which doesn't exist, should not throw error
            driver.deleteVertex(methodVertex)
            assertFalse(driver.exists(methodVertex))
            // Delete metadata
            assertTrue(driver.exists(metaDataVertex))
            driver.deleteVertex(metaDataVertex)
            assertFalse(driver.exists(metaDataVertex))
        }

        @Test
        fun testMethodDelete() {
            assertTrue(driver.exists(methodVertex))
            driver.deleteMethod(methodVertex.fullName(), methodVertex.signature())
            assertFalse(driver.exists(methodVertex))
            assertFalse(driver.exists(literalVertex))
            assertFalse(driver.exists(returnVertex))
            assertFalse(driver.exists(methodReturnVertex))
            assertFalse(driver.exists(localVertex))
            assertFalse(driver.exists(blockVertex))
            assertFalse(driver.exists(callVertex))
            // Check that deleting a method doesn't throw any error
            driver.deleteMethod(methodVertex.fullName(), methodVertex.signature())
        }
    }
}
