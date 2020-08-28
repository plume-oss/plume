package za.ac.sun.plume.intraprocedural

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.*
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.vertices.BlockVertex
import za.ac.sun.plume.domain.models.vertices.ControlStructureVertex
import za.ac.sun.plume.domain.models.vertices.LiteralVertex
import za.ac.sun.plume.domain.models.vertices.LocalVertex
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import za.ac.sun.plume.util.TestQueryBuilderUtil
import java.io.File
import java.io.IOException

@Disabled
class LoopIntraproceduralTest {
    private lateinit var g: GraphTraversalSource
    private lateinit var methodRoot: Vertex

    companion object {
        private val logger = LogManager.getLogger(LoopIntraproceduralTest::javaClass)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}loop"
        private val TEST_GRAPH = TestConstants.testGraph

        @AfterAll
        @JvmStatic
        @Throws(IOException::class)
        fun tearDownAll() {
            deleteClassFiles(PATH)
            val f = File(TEST_GRAPH)
            if (f.exists() && !f.delete()) {
                logger.warn("Could not clear " + ConditionalIntraproceduralTest::class.java.name + "'s test resources.")
            }
        }

        init {
            val testFileUrl = LoopIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val fileCannon = Extractor(driver, CLS_PATH)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        val resourceDir = "${PATH.absolutePath}${File.separator}Loop$currentTestNumber.java"
        // Load test resource and project + export graph
        val f = File(resourceDir)
        fileCannon.load(f)
        fileCannon.project()
        driver.exportGraph(TEST_GRAPH)
        g = TinkerGraph.open().traversal()
        g.io<Any>(TEST_GRAPH).read().iterate()
        val methodTraversal = g.V()
                .has("METHOD", "fullName", "intraprocedural.loop.Loop"
                        + currentTestNumber + ".main")
        Assertions.assertTrue(methodTraversal.hasNext())
        methodRoot = methodTraversal.next()
    }

    @Test
    fun loop1Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "STORE", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1", 2).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "ADD", 2).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, LiteralVertex.LABEL, "name", "1", 3).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1", 3).hasNext())
    }

    @Test
    fun loop2Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check that there is no IF
        val ifRootCheckTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "IF")
        Assertions.assertFalse(ifRootCheckTraversal.hasNext())
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 20)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
    }

    @Test
    fun loop3Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "STORE", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1", 2).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "ADD", 2).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, LiteralVertex.LABEL, "name", "1", 3).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1", 3).hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 2)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop4Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 20)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "order", 29, 2)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop5Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 20)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        // Check nested-while branch
        val whileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 22)
        Assertions.assertTrue(whileWhileBodyTraversal.hasNext())
        val whileWhileBody = whileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop6Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        // Check nested-while branch
        val whileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 26)
        Assertions.assertTrue(whileWhileBodyTraversal.hasNext())
        val whileWhileBody = whileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop7Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 20)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        // Check nested-while branch
        val whileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 23)
        Assertions.assertTrue(whileWhileBodyTraversal.hasNext())
        val whileWhileBody = whileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop8Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        // Check nested-while branch
        val whileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 25)
        Assertions.assertTrue(whileWhileBodyTraversal.hasNext())
        val whileWhileBody = whileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 34)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop9Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        // Check nested-while branch
        val whileWhileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, ControlStructureVertex.LABEL, "name", "DO_WHILE").has("order", 24)
        Assertions.assertTrue(whileWhileRootTraversal.hasNext())
        val whileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 25)
        Assertions.assertTrue(whileWhileBodyTraversal.hasNext())
        val whileWhileBody = whileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check nested-while-while branch
        val whileWhileWhileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, ControlStructureVertex.LABEL, "name", "WHILE").has("order", 31)
        Assertions.assertTrue(whileWhileWhileRootTraversal.hasNext())
        val whileWhileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 33)
        Assertions.assertTrue(whileWhileWhileBodyTraversal.hasNext())
        val whileWhileWhileBody = whileWhileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 44)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }

    @Test
    fun loop10Test() {
        // Get conditional root
        val whileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, methodRoot, ControlStructureVertex.LABEL, "name", "WHILE")
        Assertions.assertTrue(whileRootTraversal.hasNext())
        val whileRoot = whileRootTraversal.next()
        // Check while branch
        val whileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileRoot, BlockVertex.LABEL, "name", "IF_BODY").has("order", 21)
        Assertions.assertTrue(whileBodyTraversal.hasNext())
        val whileBody = whileBodyTraversal.next()
        // a = a - b;
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "SUB").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "2").hasNext())
        // Check nested-while branch
        val whileWhileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, ControlStructureVertex.LABEL, "name", "DO_WHILE").has("order", 29)
        Assertions.assertTrue(whileWhileRootTraversal.hasNext())
        val whileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 30)
        Assertions.assertTrue(whileWhileBodyTraversal.hasNext())
        val whileWhileBody = whileWhileBodyTraversal.next()
        // a ++;
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // Check nested-while-while branch
        val whileWhileWhileRootTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, ControlStructureVertex.LABEL, "name", "WHILE").has("order", 36)
        Assertions.assertTrue(whileWhileWhileRootTraversal.hasNext())
        val whileWhileWhileBodyTraversal = TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "IF_BODY").has("order", 38)
        Assertions.assertTrue(whileWhileWhileBodyTraversal.hasNext())
        val whileWhileWhileBody = whileWhileWhileBodyTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, LiteralVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileWhileBody, LocalVertex.LABEL, "name", "2").hasNext())
        // a = a + b
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, BlockVertex.LABEL, "name", "ADD").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "2").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileWhileBody, LocalVertex.LABEL, "name", "1").hasNext())
        // b = a / b;
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "STORE").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "2").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, BlockVertex.LABEL, "name", "DIV").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "1").hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdge(g, EdgeLabel.AST, whileBody, LocalVertex.LABEL, "name", "2").hasNext())
        // Check method level store
        val postWhileStoreTraversal = TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, methodRoot, BlockVertex.LABEL, "name", "STORE", 1).has("order", 59)
        Assertions.assertTrue(postWhileStoreTraversal.hasNext())
        val postWhileStoreVertex = postWhileStoreTraversal.next()
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LiteralVertex.LABEL, "name", "3", 1).hasNext())
        Assertions.assertTrue(TestQueryBuilderUtil.getVertexAlongEdgeFixed(g, EdgeLabel.AST, postWhileStoreVertex, LocalVertex.LABEL, "name", "2", 1).hasNext())
    }
}