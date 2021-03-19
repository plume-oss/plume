package io.github.plume.oss.drivers

import io.github.plume.oss.TestDomainResources
import io.github.plume.oss.TestDomainResources.Companion.BOOL_1
import io.github.plume.oss.TestDomainResources.Companion.INT_1
import io.github.plume.oss.TestDomainResources.Companion.INT_2
import io.github.plume.oss.TestDomainResources.Companion.STRING_1
import io.github.plume.oss.TestDomainResources.Companion.STRING_2
import io.github.plume.oss.TestDomainResources.Companion.bindingVertex
import io.github.plume.oss.TestDomainResources.Companion.blockVertex
import io.github.plume.oss.TestDomainResources.Companion.callVertex
import io.github.plume.oss.TestDomainResources.Companion.controlStructureVertex
import io.github.plume.oss.TestDomainResources.Companion.fileVertex
import io.github.plume.oss.TestDomainResources.Companion.fldIdentVertex
import io.github.plume.oss.TestDomainResources.Companion.generateSimpleCPG
import io.github.plume.oss.TestDomainResources.Companion.identifierVertex
import io.github.plume.oss.TestDomainResources.Companion.jumpTargetVertex
import io.github.plume.oss.TestDomainResources.Companion.literalVertex
import io.github.plume.oss.TestDomainResources.Companion.localVertex
import io.github.plume.oss.TestDomainResources.Companion.metaDataVertex
import io.github.plume.oss.TestDomainResources.Companion.methodRefVertex
import io.github.plume.oss.TestDomainResources.Companion.methodVertex
import io.github.plume.oss.TestDomainResources.Companion.modifierVertex
import io.github.plume.oss.TestDomainResources.Companion.mtdParamInVertex
import io.github.plume.oss.TestDomainResources.Companion.mtdRtnVertex
import io.github.plume.oss.TestDomainResources.Companion.namespaceBlockVertex1
import io.github.plume.oss.TestDomainResources.Companion.namespaceBlockVertex2
import io.github.plume.oss.TestDomainResources.Companion.returnVertex
import io.github.plume.oss.TestDomainResources.Companion.typeArgumentVertex
import io.github.plume.oss.TestDomainResources.Companion.typeDeclVertex
import io.github.plume.oss.TestDomainResources.Companion.typeParameterVertex
import io.github.plume.oss.TestDomainResources.Companion.typeRefVertex
import io.github.plume.oss.TestDomainResources.Companion.unknownVertex
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.ListMapper
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import overflowdb.Graph
import scala.Option
import kotlin.properties.Delegates

@Suppress("DEPRECATION")
class TigerGraphDriverIntTest {

    companion object {
        lateinit var driver: TigerGraphDriver
        private var testStartTime by Delegates.notNull<Long>()

        private fun testPayloadContents() {
            val payload = driver.buildSchemaPayload()
            NodeKeyNames.ALL.filterNot { it == NODE_LABEL }.map(payload::contains)
                .forEach(Assertions::assertTrue)
            EdgeTypes.ALL.map(payload::contains).forEach(Assertions::assertTrue)
        }

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            driver = (DriverFactory(GraphDatabase.TIGER_GRAPH) as TigerGraphDriver)
                .username("tigergraph")
                .password("tigergraph")
                .hostname("127.0.0.1")
                .restPpPort(9000)
                .gsqlPort(14240)
                .secure(false)
            assertEquals("127.0.0.1", driver.hostname)
            assertEquals(9000, driver.restPpPort)
            assertEquals(false, driver.secure)
            testPayloadContents()
            driver.buildSchema()
            testStartTime = System.nanoTime()
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            println("${TigerGraphDriverIntTest::class.java.simpleName} completed in ${(System.nanoTime() - testStartTime) / 1e6} ms")
            driver.close()
        }
    }

    @AfterEach
    fun tearDown() {
        TestDomainResources.simpleCpgVertices.forEach { it.id(-1) }
        driver.clearGraph()
    }

    @Nested
    @DisplayName("Test driver vertex find and exist methods")
    inner class VertexAddAndExistsTests {
        @Test
        fun findAstVertex() {
            val v1 = NewArrayInitializerBuilder().order(INT_1)
            val v2 = NewArrayInitializerBuilder().order(INT_2)
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
            val v1 = NewBindingBuilder().name(STRING_1).signature(STRING_2)
            val v2 = NewBindingBuilder().name(STRING_2).signature(STRING_1)
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
            val v1 = NewFieldIdentifierBuilder().canonicalName(STRING_1).code(STRING_2).argumentIndex(INT_1)
                .order(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
            val v2 = NewFieldIdentifierBuilder().canonicalName(STRING_2).code(STRING_1).argumentIndex(INT_1)
                .order(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
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
            val v1 = NewMetaDataBuilder().language(STRING_1).version(STRING_2)
            val v2 = NewMetaDataBuilder().language(STRING_2).version(STRING_1)
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
            val v1 = NewMethodRefBuilder().methodInstFullName(Option.apply(STRING_1)).methodFullName(STRING_2)
                .code(STRING_1).order(INT_1).argumentIndex(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1))
            val v2 = NewMethodRefBuilder().methodInstFullName(Option.apply(STRING_2)).methodFullName(STRING_1)
                .code(STRING_1).order(INT_1).argumentIndex(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1))
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
            val v1 = NewTypeBuilder().name(STRING_1).fullName(STRING_2).typeDeclFullName(STRING_2)
            val v2 = NewTypeBuilder().name(STRING_2).fullName(STRING_1).typeDeclFullName(STRING_2)
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
            val v1 = NewTypeRefBuilder().typeFullName(STRING_1).dynamicTypeHintFullName(
                ListMapper.stringToScalaList(STRING_2)
            ).code(STRING_1).argumentIndex(INT_1).order(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1))
            val v2 = NewTypeRefBuilder().typeFullName(STRING_2).dynamicTypeHintFullName(
                ListMapper.stringToScalaList(STRING_1)
            ).code(STRING_1).argumentIndex(INT_1).order(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1))
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
            val v1 = NewUnknownBuilder().typeFullName(STRING_1).code(STRING_2).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
            val v2 = NewUnknownBuilder().typeFullName(STRING_2).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
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
        fun updateVertexTest() {
            driver.addVertex(fileVertex)
            assertTrue(driver.exists(fileVertex))
            driver.getWholeGraph()
                .use { g -> assertTrue(g.nodes(fileVertex.id()).asSequence().any { it.property(NAME) == STRING_1 }) }
            driver.updateVertexProperty(fileVertex.id(), FILE, NAME, STRING_2)
            driver.getWholeGraph()
                .use { g -> assertTrue(g.nodes(fileVertex.id()).asSequence().any { it.property(NAME) == STRING_2 }) }
        }

        @Test
        fun testGetMetaData() {
            driver.addVertex(metaDataVertex)
            val metaData = driver.getMetaData()
            assertNotNull(metaData)
            assertEquals(metaDataVertex.id(), metaData!!.id())
            driver.deleteVertex(metaData.id(), metaData.build().label())
            assertNull(driver.getMetaData())
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
            assertFalse(driver.exists(typeDeclVertex, typeParameterVertex, AST))
            driver.addEdge(typeDeclVertex, typeParameterVertex, AST)
            assertTrue(driver.exists(typeDeclVertex, typeParameterVertex, AST))
        }

        @Test
        fun testEdgeWhenItWillViolateSchema() {
            driver.addVertex(literalVertex)
            driver.addVertex(identifierVertex)
            assertTrue(driver.exists(literalVertex))
            assertTrue(driver.exists(identifierVertex))
            assertFalse(driver.exists(literalVertex, identifierVertex, AST))
            assertThrows(PlumeSchemaViolationException::class.java) {
                driver.addEdge(
                    literalVertex,
                    identifierVertex,
                    AST
                )
            }
        }

        @Test
        fun testEdgeWhenVerticesAreNotAlreadyPresent() {
            assertFalse(driver.exists(callVertex, identifierVertex, AST))
            driver.addEdge(callVertex, identifierVertex, AST)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(callVertex, identifierVertex, AST))
        }

        @Test
        fun testEdgeDirection() {
            assertFalse(driver.exists(callVertex, identifierVertex, AST))
            assertFalse(driver.exists(identifierVertex, callVertex, AST))
            driver.addEdge(callVertex, identifierVertex, AST)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(callVertex, identifierVertex, AST))
            assertFalse(driver.exists(identifierVertex, callVertex, AST))
        }

        @Test
        fun testCfgEdgeCreation() {
            assertFalse(driver.exists(literalVertex, identifierVertex, CFG))
            driver.addEdge(literalVertex, identifierVertex, CFG)
            assertTrue(driver.exists(literalVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(literalVertex, identifierVertex, CFG))
        }

        @Test
        fun testBindsToEdgeCreation() {
            assertFalse(driver.exists(typeArgumentVertex, typeParameterVertex, BINDS_TO))
            driver.addEdge(typeArgumentVertex, typeParameterVertex, BINDS_TO)
            assertTrue(driver.exists(typeArgumentVertex))
            assertTrue(driver.exists(typeParameterVertex))
            assertTrue(driver.exists(typeArgumentVertex, typeParameterVertex, BINDS_TO))
        }

        @Test
        fun testRefEdgeCreation() {
            assertFalse(driver.exists(bindingVertex, methodVertex, REF))
            driver.addEdge(bindingVertex, methodVertex, REF)
            assertTrue(driver.exists(bindingVertex))
            assertTrue(driver.exists(methodVertex))
            assertTrue(driver.exists(bindingVertex, methodVertex, REF))
        }

        @Test
        fun testReceiverEdgeCreation() {
            assertFalse(driver.exists(callVertex, identifierVertex, RECEIVER))
            driver.addEdge(callVertex, identifierVertex, RECEIVER)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(identifierVertex))
            assertTrue(driver.exists(callVertex, identifierVertex, RECEIVER))
        }

        @Test
        fun testConditionEdgeCreation() {
            assertFalse(driver.exists(controlStructureVertex, jumpTargetVertex, CONDITION))
            driver.addEdge(controlStructureVertex, jumpTargetVertex, CONDITION)
            assertTrue(driver.exists(controlStructureVertex))
            assertTrue(driver.exists(jumpTargetVertex))
            assertTrue(driver.exists(controlStructureVertex, jumpTargetVertex, CONDITION))
        }

        @Test
        fun testBindsEdgeCreation() {
            assertFalse(driver.exists(typeDeclVertex, bindingVertex, BINDS))
            driver.addEdge(typeDeclVertex, bindingVertex, BINDS)
            assertTrue(driver.exists(typeDeclVertex))
            assertTrue(driver.exists(bindingVertex))
            assertTrue(driver.exists(typeDeclVertex, bindingVertex, BINDS))
        }

        @Test
        fun testArgumentEdgeCreation() {
            assertFalse(driver.exists(callVertex, jumpTargetVertex, ARGUMENT))
            driver.addEdge(callVertex, jumpTargetVertex, ARGUMENT)
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(callVertex))
            assertTrue(driver.exists(callVertex, jumpTargetVertex, ARGUMENT))
        }

        @Test
        fun testSourceFileEdgeCreation() {
            assertFalse(driver.exists(methodVertex, fileVertex, SOURCE_FILE))
            driver.addEdge(methodVertex, fileVertex, SOURCE_FILE)
            assertTrue(driver.exists(methodVertex))
            assertTrue(driver.exists(fileVertex))
            assertTrue(driver.exists(methodVertex, fileVertex, SOURCE_FILE))
        }
    }

    @Nested
    @DisplayName("Any OverflowDb result related tests based off of a test CPG")
    inner class PlumeGraphTests {
        private lateinit var g: Graph

        @BeforeEach
        fun setUp() {
            generateSimpleCPG(driver)
        }

        @AfterEach
        fun tearDown() {
            g.close()
        }

        @Test
        fun testGetWholeGraph() {
            g = driver.getWholeGraph()
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(21, ns.size)
            assertEquals(30, es.size)

            val file = g.V(fileVertex.id()).next()
            val ns1 = g.V(namespaceBlockVertex1.id()).next()
            val mtd = g.V(methodVertex.id()).next()
            val block = g.V(blockVertex.id()).next()
            val call = g.V(callVertex.id()).next()
            val rtn = g.V(returnVertex.id()).next()
            val fldIdent = g.V(fldIdentVertex.id()).next()
            val methodRef = g.V(methodRefVertex.id()).next()
            val typeRef = g.V(typeRefVertex.id()).next()
            val controlStructure = g.V(controlStructureVertex.id()).next()
            val jumpTarget = g.V(jumpTargetVertex.id()).next()
            val mtdRtn = g.V(mtdRtnVertex.id()).next()
            val identifier = g.V(identifierVertex.id()).next()
            // Check program structure
            assertTrue(file.out(AST).asSequence().any { it.id() == namespaceBlockVertex1.id() })
            assertTrue(ns1.out(AST).asSequence().any { it.id() == namespaceBlockVertex2.id() })
            // Check method head
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdParamInVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == localVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == blockVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdRtnVertex.id() })
            assertTrue(mtd.out(CFG).asSequence().any { it.id() == blockVertex.id() })
            // Check method body AST
            assertTrue(block.out(AST).asSequence().any { it.id() == callVertex.id() })
            assertTrue(call.out(AST).asSequence().any { it.id() == identifierVertex.id() })
            assertTrue(call.out(AST).asSequence().any { it.id() == literalVertex.id() })
            assertTrue(block.out(AST).asSequence().any { it.id() == returnVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdRtnVertex.id() })
            // Check method body CFG
            assertTrue(block.out(CFG).asSequence().any { it.id() == callVertex.id() })
            assertTrue(call.out(CFG).asSequence().any { it.id() == fldIdentVertex.id() })
            assertTrue(rtn.out(CFG).asSequence().any { it.id() == mtdRtnVertex.id() })
            assertTrue(fldIdent.out(CFG).asSequence().any { it.id() == methodRefVertex.id() })
            assertTrue(methodRef.out(CFG).asSequence().any { it.id() == typeRefVertex.id() })
            assertTrue(typeRef.out(CFG).asSequence().any { it.id() == controlStructureVertex.id() })
            assertTrue(controlStructure.out(CFG).asSequence().any { it.id() == jumpTargetVertex.id() })
            assertTrue(jumpTarget.out(CFG).asSequence().any { it.id() == returnVertex.id() })
            assertTrue(rtn.out(CFG).asSequence().any { it.id() == mtdRtnVertex.id() })

            assertTrue(call.`in`(CFG).asSequence().any { it.id() == blockVertex.id() })
            assertTrue(rtn.`in`(CFG).asSequence().any { it.id() == jumpTargetVertex.id() })
            assertTrue(mtdRtn.`in`(CFG).asSequence().any { it.id() == returnVertex.id() })
            // Check method body misc. edges
            assertTrue(call.out(ARGUMENT).asSequence().any { it.id() == identifierVertex.id() })
            assertTrue(call.out(ARGUMENT).asSequence().any { it.id() == literalVertex.id() })
            assertTrue(identifier.out(REF).asSequence().any { it.id() == localVertex.id() })

            assertTrue(g.nodes().asSequence().any { it.id() == unknownVertex.id() })
        }

        @Test
        fun testGetEmptyMethodBody() {
            driver.clearGraph()
            g = driver.getMethod(methodVertex.build().fullName())
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(0, ns.size)
            assertEquals(0, es.size)
        }

        @Test
        fun testGetMethodHeadOnly() {
            g = driver.getMethod(methodVertex.build().fullName(), false)
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(6, ns.size)
            assertEquals(5, es.size)

            val mtd = g.V(methodVertex.id()).next()
            // Assert no program structure vertices part of the method body
            assertFalse(ns.any { it.id() == metaDataVertex.id() })
            assertFalse(ns.any { it.id() == namespaceBlockVertex2.id() })
            assertFalse(ns.any { it.id() == namespaceBlockVertex1.id() })
            assertFalse(ns.any { it.id() == fileVertex.id() })
            // Check method head
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdParamInVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == localVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == blockVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdRtnVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == modifierVertex.id() })
            // Check that none of the other vertices exist
            assertFalse(ns.any { it.id() == callVertex.id() })
            assertFalse(ns.any { it.id() == identifierVertex.id() })
            assertFalse(ns.any { it.id() == typeDeclVertex.id() })
            assertFalse(ns.any { it.id() == literalVertex.id() })
            assertFalse(ns.any { it.id() == returnVertex.id() })
        }

        @Test
        fun testGetMethodBody() {
            g = driver.getMethod(methodVertex.build().fullName(), true)
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(15, ns.size)
            assertEquals(26, es.size)

            val mtd = g.V(methodVertex.id()).next()
            val block = g.V(blockVertex.id()).next()
            val call = g.V(callVertex.id()).next()
            val rtn = g.V(returnVertex.id()).next()
            val fldIdent = g.V(fldIdentVertex.id()).next()
            val methodRef = g.V(methodRefVertex.id()).next()
            val typeRef = g.V(typeRefVertex.id()).next()
            val controlStructure = g.V(controlStructureVertex.id()).next()
            val jumpTarget = g.V(jumpTargetVertex.id()).next()
            val mtdRtn = g.V(mtdRtnVertex.id()).next()
            val identifier = g.V(identifierVertex.id()).next()
            // Assert no program structure vertices part of the method body
            assertFalse(ns.any { it.id() == metaDataVertex.id() })
            assertFalse(ns.any { it.id() == namespaceBlockVertex2.id() })
            assertFalse(ns.any { it.id() == namespaceBlockVertex1.id() })
            assertFalse(ns.any { it.id() == fileVertex.id() })
            // Check method head
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdParamInVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == localVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == blockVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdRtnVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == blockVertex.id() })

            assertTrue(block.out(AST).asSequence().any { it.id() == callVertex.id() })
            assertTrue(call.out(AST).asSequence().any { it.id() == identifierVertex.id() })
            assertTrue(call.out(AST).asSequence().any { it.id() == literalVertex.id() })
            assertTrue(block.out(AST).asSequence().any { it.id() == returnVertex.id() })
            assertTrue(mtd.out(AST).asSequence().any { it.id() == mtdRtnVertex.id() })
            // Check method body CFG
            assertTrue(block.out(CFG).asSequence().any { it.id() == callVertex.id() })
            assertTrue(call.out(CFG).asSequence().any { it.id() == fldIdentVertex.id() })
            assertTrue(rtn.out(CFG).asSequence().any { it.id() == mtdRtnVertex.id() })
            assertTrue(fldIdent.out(CFG).asSequence().any { it.id() == methodRefVertex.id() })
            assertTrue(methodRef.out(CFG).asSequence().any { it.id() == typeRefVertex.id() })
            assertTrue(typeRef.out(CFG).asSequence().any { it.id() == controlStructureVertex.id() })
            assertTrue(controlStructure.out(CFG).asSequence().any { it.id() == jumpTargetVertex.id() })
            assertTrue(jumpTarget.out(CFG).asSequence().any { it.id() == returnVertex.id() })
            assertTrue(rtn.out(CFG).asSequence().any { it.id() == mtdRtnVertex.id() })

            assertTrue(call.`in`(CFG).asSequence().any { it.id() == blockVertex.id() })
            assertTrue(rtn.`in`(CFG).asSequence().any { it.id() == jumpTargetVertex.id() })
            assertTrue(mtdRtn.`in`(CFG).asSequence().any { it.id() == returnVertex.id() })
            // Check method body misc. edges
            assertTrue(call.out(ARGUMENT).asSequence().any { it.id() == identifierVertex.id() })
            assertTrue(call.out(ARGUMENT).asSequence().any { it.id() == literalVertex.id() })
            assertTrue(identifier.out(REF).asSequence().any { it.id() == localVertex.id() })
        }

        @Test
        fun testGetProgramStructure() {
            val unknown = io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN()
            driver.addVertex(NewFileBuilder().name(unknown).order(0).hash(Option.apply(unknown)))
            g = driver.getProgramStructure()
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(5, ns.size)
            assertEquals(2, es.size)

            val file = g.V(fileVertex.id()).next()
            val ns1 = g.V(namespaceBlockVertex1.id()).next()
            // Assert program structure vertices are present
            assertTrue(ns.any { it.id() == namespaceBlockVertex2.id() })
            assertTrue(ns.any { it.id() == namespaceBlockVertex1.id() })
            assertTrue(ns.any { it.id() == fileVertex.id() })
            assertTrue(ns.any { it.id() == typeDeclVertex.id() })
            // Check that vertices are connected by AST edges
            assertTrue(file.out(AST).asSequence().any { it.id() == namespaceBlockVertex1.id() })
            assertTrue(ns1.out(AST).asSequence().any { it.id() == namespaceBlockVertex2.id() })
        }

        @Test
        fun testGetProgramTypeData() {
            g = driver.getProgramTypeData()
            val nodeCounts = g.nodes().asSequence().groupBy { it.label() }.mapValues { it.value.count() }.toMutableMap()
            assertEquals(2, nodeCounts.remove(NAMESPACE_BLOCK))
            nodeCounts.forEach { (_, u) -> assertEquals(1, u) }
            val edgeCounts = g.edges().asSequence().groupBy { it.label() }.mapValues { it.value.count() }.toMutableMap()
            assertEquals(17, edgeCounts[AST])
            assertEquals(1, edgeCounts[REF])
            assertEquals(20, g.nodeCount())
            assertEquals(18, g.edgeCount())
        }

        @Test
        fun testGetNeighbours() {
            g = driver.getNeighbours(fileVertex)
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(3, ns.size)
            assertEquals(2, es.size)

            val file = g.V(fileVertex.id()).next()
            val mtd = g.V(methodVertex.id()).next()
            // Check that vertices are connected by AST edges
            assertTrue(file.out(AST).asSequence().any { it.id() == namespaceBlockVertex1.id() })
            assertTrue(mtd.out(SOURCE_FILE).asSequence().any { it.id() == fileVertex.id() })
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
            driver.deleteVertex(methodVertex.id(), methodVertex.build().label())
            assertFalse(driver.exists(methodVertex))
            // Try delete vertex which doesn't exist, should not throw error
            driver.deleteVertex(methodVertex.id(), methodVertex.build().label())
            assertFalse(driver.exists(methodVertex))
            // Delete metadata
            assertTrue(driver.exists(metaDataVertex))
            driver.deleteVertex(metaDataVertex.id(), metaDataVertex.build().label())
            assertFalse(driver.exists(metaDataVertex))
        }

        @Test
        fun testEdgeDelete() {
            assertTrue(driver.exists(methodVertex, fileVertex, SOURCE_FILE))
            driver.deleteEdge(methodVertex, fileVertex, SOURCE_FILE)
            assertFalse(driver.exists(methodVertex, fileVertex, SOURCE_FILE))
            assertTrue(driver.exists(methodVertex))
            assertTrue(driver.exists(fileVertex))
            // Try delete edge which doesn't exist, should not throw error
            driver.deleteEdge(methodVertex, fileVertex, SOURCE_FILE)
        }

        @Test
        fun testMethodDelete() {
            assertTrue(driver.exists(methodVertex))
            driver.deleteMethod(methodVertex.build().fullName())
            assertFalse(driver.exists(methodVertex))
            assertFalse(driver.exists(literalVertex))
            assertFalse(driver.exists(returnVertex))
            assertFalse(driver.exists(mtdRtnVertex))
            assertFalse(driver.exists(localVertex))
            assertFalse(driver.exists(blockVertex))
            assertFalse(driver.exists(callVertex))
            // Check that deleting a method doesn't throw any error
            driver.deleteMethod(methodVertex.build().fullName())
        }
    }

    @Nested
    @DisplayName("ID retrieval tests")
    inner class VertexIdTests {

        @Test
        fun testGetIdInsideRange() {
            val ids1 = driver.getVertexIds(0, 10)
            assertTrue(ids1.isEmpty())
            driver.addVertex(NewArrayInitializerBuilder().order(INT_1).id(1L))
            val ids2 = driver.getVertexIds(0, 10)
            assertEquals(setOf(1L), ids2)
        }

        @Test
        fun testGetIdOutsideRange() {
            driver.addVertex(NewArrayInitializerBuilder().order(INT_1).id(11L))
            val ids1 = driver.getVertexIds(0, 10)
            assertEquals(emptySet<Long>(), ids1)
        }

        @Test
        fun testGetIdOnExistingGraph() {
            generateSimpleCPG(driver)
            val ids = driver.getVertexIds(0, 100)
            assertEquals(21, ids.size)
        }

    }

    @Nested
    @DisplayName("Test methods that select and return lists of properties and vertices")
    inner class PropertyAndVertexReturns {

        @BeforeEach
        fun setUp() {
            generateSimpleCPG(driver)
        }

        @Test
        fun testGetMethodNames() {
            assertEquals(listOf(STRING_1), driver.getPropertyFromVertices<String>(FULL_NAME, METHOD))
            driver.addVertex(methodVertex.fullName(STRING_2).id(1200))
            val newNames = driver.getPropertyFromVertices<String>(FULL_NAME, METHOD)
            assertTrue(newNames.contains(STRING_1))
            assertTrue(newNames.contains(STRING_2))
        }

        @Test
        fun testGetNonExistentProperty() {
            assertEquals(emptyList<String>(), driver.getPropertyFromVertices<String>("<dne>"))
        }

        @Test
        fun getVertexByFullNameAndType() {
            val r = driver.getVerticesByProperty(FULL_NAME, STRING_1)
            assertEquals(3, r.size)
            assertTrue(r.any { it is NewNamespaceBlockBuilder })
            assertTrue(r.any { it is NewTypeDeclBuilder })
            assertTrue(r.any { it is NewMethodBuilder })
            assertTrue(driver.getVerticesByProperty(FULL_NAME, STRING_1, TYPE_DECL).size == 1)
        }

        @Test
        fun getNonExistentVertexByProperty() {
            val r1 = driver.getVerticesByProperty(FULL_NAME, "<dne>")
            assertTrue(r1.isEmpty())
            val r2 = driver.getVerticesByProperty(FULL_NAME, "<dne>", NAMESPACE_BLOCK)
            assertTrue(r2.isEmpty())
        }

        @Test
        fun getVertexByIsExternalAndType() {
            val r = driver.getVerticesByProperty(IS_EXTERNAL, BOOL_1)
            assertEquals(2, r.size)
            assertTrue(r.any { it is NewTypeDeclBuilder })
            assertTrue(r.any { it is NewMethodBuilder })
            assertTrue(driver.getVerticesByProperty(IS_EXTERNAL, BOOL_1, TYPE_DECL).size == 1)
        }

        @Test
        fun getVertexOfType() {
            val r1 = driver.getVerticesOfType(METHOD)
            assertEquals(1, r1.size)
            assertTrue(r1.any { it is NewMethodBuilder })
            val r2 = driver.getVerticesOfType(NAMESPACE_BLOCK)
            assertEquals(2, r2.size)
            assertTrue(r2.all { it is NewNamespaceBlockBuilder })
        }

    }
}