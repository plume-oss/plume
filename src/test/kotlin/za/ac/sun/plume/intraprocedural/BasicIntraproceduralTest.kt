package za.ac.sun.plume.intraprocedural

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants.testGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.vertices.BlockVertex
import za.ac.sun.plume.domain.models.vertices.IdentifierVertex
import za.ac.sun.plume.domain.models.vertices.LiteralVertex
import za.ac.sun.plume.domain.models.vertices.LocalVertex
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ExtractorConst.BOOLEAN
import za.ac.sun.plume.util.ExtractorConst.BYTE
import za.ac.sun.plume.util.ExtractorConst.DOUBLE
import za.ac.sun.plume.util.ExtractorConst.INT
import za.ac.sun.plume.util.ExtractorConst.LONG
import za.ac.sun.plume.util.ExtractorConst.RETURN
import za.ac.sun.plume.util.ExtractorConst.SHORT
import za.ac.sun.plume.util.ExtractorConst.VOID
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import za.ac.sun.plume.util.TestQueryBuilderUtil
import za.ac.sun.plume.util.TestQueryBuilderUtil.*
import java.io.File
import java.io.IOException
import java.lang.NullPointerException

class BasicIntraproceduralTest {
    private lateinit var g: GraphTraversalSource
    private lateinit var currentTestNumber: String

    companion object {
        private val logger = LogManager.getLogger(BasicIntraproceduralTest::javaClass)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}basic"
        private val TEST_GRAPH = testGraph

        @AfterAll
        @JvmStatic
        @Throws(IOException::class)
        fun tearDownAll() {
            deleteClassFiles(PATH)
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear ${BasicIntraproceduralTest::javaClass.name}'s test resources.");
            }
        }

        @JvmStatic
        fun testBasic1Structure(g: GraphTraversalSource, methodRoot: Vertex) {
            assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "c").has("typeFullName", INT).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "3").has("typeFullName", INT).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "2").has("typeFullName", INT).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", INT).hasNext())
            val addTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", INT)
            assertTrue(addTraversal.hasNext())
            val addVertex = addTraversal.next()
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", BYTE).hasNext())
            assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
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
        val driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val extractor = Extractor(driver, CLS_PATH)
        // Select test resource based on integer in method name
        currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Basic$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        extractor.load(f)
        extractor.project()
        driver.exportGraph(TEST_GRAPH)
        g = TinkerGraph.open().traversal()
        g.io<Any>(TEST_GRAPH).read().iterate()
    }

    @Test
    fun basic1Test() {
        val methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.main")
        assertTrue(methodTraversal.hasNext())
        val methodRoot = methodTraversal.next()
        assertEquals(3, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())
        testBasic1Structure(g, methodRoot)
    }

    @Test
    fun basic2Test() {
        val methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.main")
        assertTrue(methodTraversal.hasNext())
        val methodRoot = methodTraversal.next()
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(4, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "b").has("typeFullName", DOUBLE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "c").has("typeFullName", DOUBLE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "\$stack6").has("typeFullName", DOUBLE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a").has("typeFullName", BYTE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "6").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b").has("typeFullName", DOUBLE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "2.0").has("typeFullName", DOUBLE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", DOUBLE).hasNext())
        val addTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", DOUBLE)
        assertTrue(addTraversal.hasNext())
        val addVertex = addTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "\$stack6").has("typeFullName", DOUBLE).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "b").has("typeFullName", DOUBLE).hasNext())
    }

    @Test
    fun basic3Test() {
        val methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.main")
        assertTrue(methodTraversal.hasNext())
        val methodRoot = methodTraversal.next()
        assertTrue(buildStoreTraversal(g, EdgeLabel.AST, methodRoot).hasNext())
        assertEquals(4, buildStoreTraversal(g, EdgeLabel.AST, methodRoot).count().next())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "a").has("typeFullName", LONG).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "c").has("typeFullName", LONG).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LocalVertex.LABEL, "name", "\$stack6").has("typeFullName", LONG).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "a").has("typeFullName", LONG).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "4300343223423L").has("typeFullName", LONG).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "b").has("typeFullName", SHORT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, LiteralVertex.LABEL, "name", "-2342").has("typeFullName", INT).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, IdentifierVertex.LABEL, "name", "c").has("typeFullName", LONG).hasNext())
        val addTraversal = getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "ADD").has("typeFullName", LONG)
        assertTrue(addTraversal.hasNext())
        val addVertex = addTraversal.next()
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "\$stack6").has("typeFullName", LONG).hasNext())
        assertTrue(getVertexAlongEdge(g, EdgeLabel.AST, addVertex, IdentifierVertex.LABEL, "name", "a").has("typeFullName", LONG).hasNext())
    }

    @Test
    fun basic4Test() {
        val constructorTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.<init>")
        assertTrue(constructorTraversal.hasNext())
        val constructor = constructorTraversal.next()
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, constructor).hasNext())
        assertEquals(3, buildMethodModifierTraversal(g, EdgeLabel.AST, constructor).count().next())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, constructor).has("name", "VIRTUAL").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, constructor).has("name", "CONSTRUCTOR").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, constructor).has("name", "PUBLIC").hasNext())
        assertTrue(buildMethodReturnTraversal(g, EdgeLabel.AST, constructor).has("name", RETURN).has("typeFullName", VOID).hasNext())
        val johnTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.John")
        assertTrue(johnTraversal.hasNext())
        val johnVertex = johnTraversal.next()
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, johnVertex).hasNext())
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabel.AST, johnVertex).count().next())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, johnVertex).has("name", "STATIC").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, johnVertex).has("name", "PUBLIC").hasNext())
        assertTrue(buildMethodReturnTraversal(g, EdgeLabel.AST, johnVertex).has("name", RETURN).has("typeFullName", "$INT[]").hasNext())
        val mainTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.main")
        assertTrue(mainTraversal.hasNext())
        val mainVertex = mainTraversal.next()
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, mainVertex).hasNext())
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabel.AST, mainVertex).count().next())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, mainVertex).has("name", "STATIC").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, mainVertex).has("name", "PUBLIC").hasNext())
        assertTrue(buildMethodReturnTraversal(g, EdgeLabel.AST, mainVertex).has("name", RETURN).has("typeFullName", VOID).hasNext())
        assertTrue(buildMethodParameterInTraversal(g, EdgeLabel.AST, mainVertex).has("name", "args").hasNext())
        val dickTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.Dick")
        assertTrue(dickTraversal.hasNext())
        val dickVertex = dickTraversal.next()
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, dickVertex).hasNext())
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabel.AST, dickVertex).count().next())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, dickVertex).has("name", "STATIC").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, dickVertex).has("name", "PUBLIC").hasNext())
        assertTrue(buildMethodReturnTraversal(g, EdgeLabel.AST, dickVertex).has("name", RETURN).has("typeFullName", BOOLEAN).hasNext())
        val nigelTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.Nigel")
        assertTrue(nigelTraversal.hasNext())
        val nigelVertex = nigelTraversal.next()
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, nigelVertex).hasNext())
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabel.AST, nigelVertex).count().next())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, nigelVertex).has("name", "STATIC").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, nigelVertex).has("name", "PUBLIC").hasNext())
        assertTrue(buildMethodReturnTraversal(g, EdgeLabel.AST, nigelVertex).has("name", RETURN).has("typeFullName",DOUBLE).hasNext())
        val sallyTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.basic.Basic$currentTestNumber.Sally")
        assertTrue(sallyTraversal.hasNext())
        val sallyVertex = sallyTraversal.next()
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, sallyVertex).hasNext())
        assertEquals(2, buildMethodModifierTraversal(g, EdgeLabel.AST, sallyVertex).count().next())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, sallyVertex).has("name", "STATIC").hasNext())
        assertTrue(buildMethodModifierTraversal(g, EdgeLabel.AST, sallyVertex).has("name", "PUBLIC").hasNext())
        assertTrue(buildMethodReturnTraversal(g, EdgeLabel.AST, sallyVertex).has("name", RETURN).has("typeFullName", INT).hasNext())
    }

    @Test
    @Throws(IOException::class)
    fun basic5Test() {
        val driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect(); importGraph(TEST_GRAPH) }
        val fileCannon = Extractor(driver, CLS_PATH)
        val resourceDir = "${PATH.absolutePath}${File.separator}basic5${File.separator}Basic5.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        fileCannon.load(f)
        fileCannon.project()
        driver.exportGraph(TEST_GRAPH)
        g = TinkerGraph.open().traversal()
        g.io<Any>(TEST_GRAPH).read().iterate()

        // This is Basic1 in two separate packages
        val intraNamespaceTraversal = g.V().has(VertexLabel.NAMESPACE_BLOCK.toString(), "fullName", "intraprocedural")
        assertTrue(intraNamespaceTraversal.hasNext())
        val intraNamespaceVertex = intraNamespaceTraversal.next()
        val basicNamespaceTraversal = getVertexAlongEdge(g, EdgeLabel.AST, intraNamespaceVertex, VertexLabel.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic")
        assertTrue(basicNamespaceTraversal.hasNext())
        val basicNamespaceVertex = basicNamespaceTraversal.next()
        val basic5NamespaceTraversal = getVertexAlongEdge(g, EdgeLabel.AST, basicNamespaceVertex, VertexLabel.NAMESPACE_BLOCK, "fullName", "intraprocedural.basic.basic5")
        assertTrue(basic5NamespaceTraversal.hasNext())
        val basic5NamespaceVertex = basic5NamespaceTraversal.next()
        val basicMethodTraversal = getVertexAlongEdge(g, EdgeLabel.AST, basicNamespaceVertex, VertexLabel.METHOD, "name", "main")
        assertTrue(basicMethodTraversal.hasNext())
        val basic6MethodTraversal = getVertexAlongEdge(g, EdgeLabel.AST, basic5NamespaceVertex, VertexLabel.METHOD, "name", "main")
        assertTrue(basic6MethodTraversal.hasNext())
        assertEquals(6, buildStoreTraversal(g, EdgeLabel.AST, intraNamespaceVertex).count().next())
        testBasic1Structure(g, basicNamespaceVertex)
        testBasic1Structure(g, basic5NamespaceVertex)
    }
}