package za.ac.sun.plume.intraprocedural

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.TestConstants
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.Equality
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.DriverFactory
import za.ac.sun.plume.drivers.GraphDatabase
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.util.ExtractorConst.ASSIGN
import za.ac.sun.plume.util.ExtractorConst.FALSE_TARGET
import za.ac.sun.plume.util.ExtractorConst.IF_ROOT
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET
import za.ac.sun.plume.util.GraphDiff
import za.ac.sun.plume.util.ResourceCompilationUtil.deleteClassFiles
import za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdge
import za.ac.sun.plume.util.TestQueryBuilderUtil.getVertexAlongEdgeFixed
import java.io.File
import java.io.IOException

@Disabled
class ConditionalIntraproceduralTest {
    private lateinit var g: GraphTraversalSource
    private lateinit var methodRoot: Vertex
    private lateinit var testResourcePath: String

    companion object {
        private val logger = LogManager.getLogger(ConditionalIntraproceduralTest::javaClass)
        private var PATH: File
        private var CLS_PATH: File
        private val TEST_PATH = "intraprocedural${File.separator}conditional"
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
            val testFileUrl = ConditionalIntraproceduralTest::class.java.classLoader.getResource(TEST_PATH)
                    ?: throw NullPointerException("Unable to obtain test resource")
            PATH = File(testFileUrl.file)
            CLS_PATH = File(PATH.absolutePath.replace(System.getProperty("user.dir") + File.separator, "").replace(TEST_PATH, ""))
        }
    }

    @BeforeEach
    @Throws(IOException::class)
    fun setUp(testInfo: TestInfo) {
        val hook = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        val fileCannon = Extractor(hook, CLS_PATH)
        // Select test resource based on integer in method name
        val currentTestNumber = testInfo
                .displayName
                .replace("[^0-9]".toRegex(), "")
        testResourcePath = "${PATH.absolutePath}${File.separator}Conditional$currentTestNumber"
        val testSourceFile = "$testResourcePath.java"
        // Load test resource and project + export graph
        val f = File(testSourceFile)
        fileCannon.load(f)
        fileCannon.project()
        hook.exportGraph(TEST_GRAPH)
        g = TinkerGraph.open().traversal()
        g.io<Any>(TEST_GRAPH).read().iterate()
        val methodTraversal = g.V()
                .has(VertexLabel.METHOD.name, "fullName", "intraprocedural.conditional.Conditional$currentTestNumber.main")
        assertTrue(methodTraversal.hasNext())
        methodRoot = methodTraversal.next()
    }

    @Test
    fun conditional1Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional2Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional3Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional4Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional5Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional6Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional7Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional8Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional9Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional10Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional11Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional12Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional13Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional14Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional15Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional16Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }

    @Test
    fun conditional17Test() {
        val a = TinkerGraph.open()
        val b = TinkerGraph.open()
        a.traversal().io<Any>(TEST_GRAPH).read().iterate()
        b.traversal().io<Any>("$testResourcePath.xml").read().iterate()
        val c = GraphDiff.diff(a, b)
        assertEquals(0L, c.traversal().V().count().next())
    }
}