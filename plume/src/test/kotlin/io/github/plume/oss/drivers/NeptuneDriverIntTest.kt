package io.github.plume.oss.drivers

import io.github.plume.oss.TestDomainResources
import io.github.plume.oss.TestDomainResources.Companion.INT_1
import io.github.plume.oss.TestDomainResources.Companion.INT_2
import io.github.plume.oss.TestDomainResources.Companion.STRING_1
import io.github.plume.oss.TestDomainResources.Companion.STRING_2
import io.github.plume.oss.TestDomainResources.Companion.bindingVertex
import io.github.plume.oss.TestDomainResources.Companion.blockVertex
import io.github.plume.oss.TestDomainResources.Companion.callVertex
import io.github.plume.oss.TestDomainResources.Companion.controlStructureVertex
import io.github.plume.oss.TestDomainResources.Companion.fldIdentVertex
import io.github.plume.oss.TestDomainResources.Companion.fileVertex
import io.github.plume.oss.TestDomainResources.Companion.generateSimpleCPG
import io.github.plume.oss.TestDomainResources.Companion.identifierVertex
import io.github.plume.oss.TestDomainResources.Companion.jumpTargetVertex
import io.github.plume.oss.TestDomainResources.Companion.literalVertex
import io.github.plume.oss.TestDomainResources.Companion.localVertex
import io.github.plume.oss.TestDomainResources.Companion.metaDataVertex
import io.github.plume.oss.TestDomainResources.Companion.mtdParamInVertex
import io.github.plume.oss.TestDomainResources.Companion.methodRefVertex
import io.github.plume.oss.TestDomainResources.Companion.mtdRtnVertex
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
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import scala.Option
import kotlin.properties.Delegates

class NeptuneDriverIntTest {

    companion object {
        lateinit var driver: NeptuneDriver
        private var testStartTime by Delegates.notNull<Long>()

        @JvmStatic
        @BeforeAll
        fun setUpAll() {
            testStartTime = System.nanoTime()
            driver = (DriverFactory(GraphDatabase.NEPTUNE) as NeptuneDriver).apply {
                this.addHostnames(System.getenv("NEPTUNE_HOSTNAME") ?: "localhost")
                    .port(8182)
                    .keyCertChainFile("src/test/resources/conf/SFSRootCAG2.pem")
                    .connect()
            }
        }

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            println("${NeptuneDriverIntTest::class.java.simpleName} completed in ${(System.nanoTime() - testStartTime) / 1e6} ms")
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
            val v1 = NewFieldIdentifierBuilder().canonicalname(STRING_1).code(STRING_2).argumentindex(INT_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
            val v2 = NewFieldIdentifierBuilder().canonicalname(STRING_2).code(STRING_1).argumentindex(INT_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
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
            val v1 = NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_1)).methodfullname(STRING_2)
                .code(STRING_1).order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1))
            val v2 = NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_2)).methodfullname(STRING_1)
                .code(STRING_1).order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1))
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
            val v1 = NewTypeBuilder().name(STRING_1).fullname(STRING_2).typedeclfullname(STRING_2)
            val v2 = NewTypeBuilder().name(STRING_2).fullname(STRING_1).typedeclfullname(STRING_2)
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
                SootToPlumeUtil.createScalaList(STRING_2)
            ).code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1))
            val v2 = NewTypeRefBuilder().typefullname(STRING_2).dynamictypehintfullname(
                SootToPlumeUtil.createScalaList(STRING_1)
            ).code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1))
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
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
            val v2 = NewUnknownBuilder().typefullname(STRING_2).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
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
    @DisplayName("Any OverflowDb result related tests based off of a test CPG")
    inner class PlumeGraphTests {

        @BeforeEach
        fun setUp() {
            generateSimpleCPG(TinkerGraphDriverIntTest.driver)
        }

        @Test
        fun testGetWholeGraph() {
            val g = TinkerGraphDriverIntTest.driver.getWholeGraph()
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
            val g = TinkerGraphDriverIntTest.driver.getMethod(methodVertex.build().fullName(), methodVertex.build().signature())
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(0, ns.size)
            assertEquals(0, es.size)
        }

        @Test
        fun testGetMethodHeadOnly() {
            val g = TinkerGraphDriverIntTest.driver.getMethod(methodVertex.build().fullName(), methodVertex.build().signature(), false)
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
            val g = TinkerGraphDriverIntTest.driver.getMethod(methodVertex.build().fullName(), methodVertex.build().signature(), true)
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
            val g = TinkerGraphDriverIntTest.driver.getProgramStructure()
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(3, ns.size)
            assertEquals(2, es.size)

            val file = g.V(fileVertex.id()).next()
            val ns1 = g.V(namespaceBlockVertex1.id()).next()
            // Assert no program structure vertices part of the method body
            assertTrue(ns.any { it.id() == namespaceBlockVertex2.id() })
            assertTrue(ns.any { it.id() == namespaceBlockVertex1.id() })
            assertTrue(ns.any { it.id() == fileVertex.id() })
            // Check that vertices are connected by AST edges
            assertTrue(file.out(AST).asSequence().any { it.id() == namespaceBlockVertex1.id() })
            assertTrue(ns1.out(AST).asSequence().any { it.id() == namespaceBlockVertex2.id() })
        }

        @Test
        fun testGetNeighbours() {
            val g = TinkerGraphDriverIntTest.driver.getNeighbours(fileVertex)
            val ns = g.nodes().asSequence().toList()
            val es = g.edges().asSequence().toList()
            assertEquals(3, ns.size)
            assertEquals(2, es.size)

            val file = g.V(fileVertex.id()).next()
            val mtd = g.V(methodVertex.id()).next()
            // Check that vertices are connected by AST edges
            assertTrue(file.out(AST).asSequence().any { it.id() == namespaceBlockVertex1.id()})
            assertTrue(mtd.out(SOURCE_FILE).asSequence().any { it.id() == fileVertex.id()})
        }
    }

    @Nested
    @DisplayName("Delete operation tests")
    inner class DriverDeleteTests {

        @BeforeEach
        fun setUp() {
            generateSimpleCPG(driver)
        }

        @AfterEach
        fun tearDown() {
            driver.clearGraph()
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
            driver.deleteMethod(methodVertex.build().fullName(), methodVertex.build().signature())
            assertFalse(driver.exists(methodVertex))
            assertFalse(driver.exists(literalVertex))
            assertFalse(driver.exists(returnVertex))
            assertFalse(driver.exists(mtdRtnVertex))
            assertFalse(driver.exists(localVertex))
            assertFalse(driver.exists(blockVertex))
            assertFalse(driver.exists(callVertex))
            // Check that deleting a method doesn't throw any error
            driver.deleteMethod(methodVertex.build().fullName(), methodVertex.build().signature())
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
}
