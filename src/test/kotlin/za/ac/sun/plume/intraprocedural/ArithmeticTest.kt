package za.ac.sun.plume.intraprocedural

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.vertices.BlockVertex
import za.ac.sun.plume.domain.models.vertices.IdentifierVertex
import za.ac.sun.plume.domain.models.vertices.LiteralVertex
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ExtractorConst.ASSIGN
import za.ac.sun.plume.util.ExtractorConst.BYTE
import za.ac.sun.plume.util.ExtractorConst.INT
import za.ac.sun.plume.util.ExtractorConst.SHORT
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import za.ac.sun.plume.util.TestQueryBuilderUtil.buildStoreTraversal
import za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdge
import java.io.File
import java.io.IOException

class ArithmeticTest {
    private lateinit var g: GraphTraversalSource
    private lateinit var methodRoot: Vertex

    companion object {
        private val logger = LogManager.getLogger(ArithmeticTest::javaClass)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}arithmetic"
        private val TEST_GRAPH = TestConstants.testGraph

        @AfterAll
        @JvmStatic
        @Throws(IOException::class)
        fun tearDownAll() {
            deleteClassFiles(PATH)
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear " + ArithmeticTest::class.java.name + "'s test resources.")
            }
        }

        init {
            val testFileUrl = ArithmeticTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val hook =  (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val fileCannon = Extractor(hook, CLS_PATH)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Arithmetic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        fileCannon.load(f)
        fileCannon.project()
        hook.exportGraph(TEST_GRAPH)
        g = TinkerGraph.open().traversal()
        g.io<Any>(TEST_GRAPH).read().iterate()
        val methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.arithmetic.Arithmetic$currentTestNumber.main")
        assertTrue(methodTraversal.hasNext())
        methodRoot = methodTraversal.next()
    }

    @Test
    fun arithmetic1Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(6, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())

        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "1").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "d").has("typeFullName", INT).hasNext())

        val subTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", INT)
        assertTrue(subTraversal.hasNext())
        val subVertex = subTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, subVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, subVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "f").has("typeFullName", INT).hasNext())

        val divTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "DIV").has("typeFullName", INT)
        assertTrue(divTraversal.hasNext())
        val divVertex = divTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, divVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, divVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", INT).hasNext())

        val addTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT)
        assertTrue(addTraversal.hasNext())
        val addVertex = addTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "e").has("typeFullName", INT).hasNext())

        val mulTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "MUL").has("typeFullName", INT)
        assertTrue(mulTraversal.hasNext())
        val mulVertex = mulTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, mulVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, mulVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
    }

    @Test
    fun arithmetic2Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(5, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "d").has("typeFullName", INT).hasNext())
        val addTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT)
        assertTrue(addTraversal.hasNext())
        val addVertex = addTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "\$stack5").has("typeFullName", INT).hasNext())
        val mulTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "MUL").has("typeFullName", INT)
        assertTrue(mulTraversal.hasNext())
        val mulVertex = mulTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, mulVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, mulVertex, IdentifierVertex.LABEL, "name", "c").has("typeFullName", BYTE).hasNext())
    }

    @Test
    fun arithmetic3Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(6, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())

        val subTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "SUB").has("typeFullName", INT)
        assertTrue(subTraversal.hasNext())
        val subVertex = subTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "d").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, subVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, subVertex, IdentifierVertex.LABEL, "name", "\$stack6").has("typeFullName", INT).hasNext())

        val addTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT)
        assertTrue(addTraversal.hasNext())
        val addVertex = addTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "\$stack6").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "\$stack5").has("typeFullName", INT).hasNext())

        val mulTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "MUL").has("typeFullName", INT)
        assertTrue(mulTraversal.hasNext())
        val mulVertex = mulTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "\$stack5").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, mulVertex, IdentifierVertex.LABEL, "name", "c").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, mulVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
    }

    @Test
    fun arithmetic4Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(6, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())

        val aStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 21)
        assertTrue(aStore.hasNext())
        val aStoreVertex = aStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, aStoreVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, aStoreVertex, LiteralVertex.LABEL, "name", "-1").has("typeFullName", INT).hasNext())

        val bStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 24)
        assertTrue(bStore.hasNext())
        val bStoreVertex = bStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, bStoreVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, bStoreVertex, LiteralVertex.LABEL, "name", "2").has("typeFullName", INT).hasNext())

        val add1Traversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT).has("order", 29)
        assertTrue(add1Traversal.hasNext())
        val add1Vertex = add1Traversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a#3").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add1Vertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add1Vertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", INT).hasNext())

        val add2Traversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT).has("order", 37)
        assertTrue(add2Traversal.hasNext())
        val add2Vertex = add2Traversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b#4").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add2Vertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add2Vertex, LiteralVertex.LABEL, "name", "-1").has("typeFullName", INT).hasNext())

        val cStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 32)
        assertTrue(cStore.hasNext())
        val cStoreVertex = cStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, cStoreVertex, IdentifierVertex.LABEL, "name", "c").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, cStoreVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())

        val dStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", INT).has("order", 40)
        assertTrue(dStore.hasNext())
        val dStoreVertex = dStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, dStoreVertex, IdentifierVertex.LABEL, "name", "d").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, dStoreVertex, IdentifierVertex.LABEL, "name", "b#4").has("typeFullName", INT).hasNext())
    }

    @Test
    fun arithmetic5Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(7, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())

        val andTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "AND").has("typeFullName", INT)
        assertTrue(andTraversal.hasNext())
        val andVertex = andTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, andVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, andVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())

        val orTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "OR").has("typeFullName", INT)
        assertTrue(orTraversal.hasNext())
        val orVertex = orTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, orVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, orVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())

        val remTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "REM").has("typeFullName", INT)
        assertTrue(remTraversal.hasNext())
        val remVertex = remTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "g").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, remVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, remVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())

        val shlTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "SHL").has("typeFullName", INT)
        assertTrue(shlTraversal.hasNext())
        val shlVertex = shlTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "e").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, shlVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, shlVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())

        val shrTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "SHR").has("typeFullName", INT)
        assertTrue(shrTraversal.hasNext())
        val shrVertex = shrTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "f").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, shrVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, shrVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())
    }

    @Test
    fun arithmetic6Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(4, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())

        val aStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 19)
        assertTrue(aStore.hasNext())
        val aStoreVertex = aStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, aStoreVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, aStoreVertex, LiteralVertex.LABEL, "name", "0").has("typeFullName", INT).hasNext())

        val bStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 22)
        assertTrue(bStore.hasNext())
        val bStoreVertex = bStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, bStoreVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, bStoreVertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", INT).hasNext())

        val ushrTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "USHR").has("typeFullName", INT)
        assertTrue(ushrTraversal.hasNext())
        val ushrVertex = ushrTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "d").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, ushrVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, ushrVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())

        val xorTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "XOR").has("typeFullName", INT)
        assertTrue(xorTraversal.hasNext())
        val xorVertex = xorTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, xorVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, xorVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
    }

    @Test
    fun arithmetic7Test() {
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(6, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())

        val aStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 19)
        assertTrue(aStore.hasNext())
        val aStoreVertex = aStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, aStoreVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, aStoreVertex, LiteralVertex.LABEL, "name", "0").has("typeFullName", INT).hasNext())

        val bStore = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", ASSIGN).has("typeFullName", BYTE).has("order", 22)
        assertTrue(bStore.hasNext())
        val bStoreVertex = bStore.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, bStoreVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, bStoreVertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", INT).hasNext())

        val add1Traversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT).has("order", 27)
        assertTrue(add1Traversal.hasNext())
        val add1Vertex = add1Traversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a#3").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add1Vertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add1Vertex, LiteralVertex.LABEL, "name", "1").has("typeFullName", INT).hasNext())

        val add2Traversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT).has("order", 37)
        assertTrue(add2Traversal.hasNext())
        val add2Vertex = add2Traversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a#3").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add2Vertex, IdentifierVertex.LABEL, "name", "a#3").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add2Vertex, LiteralVertex.LABEL, "name", "2").has("typeFullName", INT).hasNext())

        val add3Traversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT).has("order", 32)
        assertTrue(add3Traversal.hasNext())
        val add3Vertex = add3Traversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b#4").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add3Vertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add3Vertex, LiteralVertex.LABEL, "name", "-1").has("typeFullName", INT).hasNext())

        val add4Traversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT).has("order", 42)
        assertTrue(add4Traversal.hasNext())
        val add4Vertex = add4Traversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b#4").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add4Vertex, IdentifierVertex.LABEL, "name", "b#4").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, add4Vertex, LiteralVertex.LABEL, "name", "-3").has("typeFullName", INT).hasNext())
    }
}