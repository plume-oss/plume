package za.ac.sun.plume.drivers

import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import za.ac.sun.plume.domain.models.vertices.FileVertex
import java.io.File
import java.util.*

class TinkerGraphDriverTest : AbstractGremlinDriverTest() {

    companion object {
        private val tempDir = System.getProperty("java.io.tmpdir")
        private val logger = LogManager.getLogger(TinkerGraphDriverTest::class.java)
        val testGraphML = "$tempDir/plume/plume_driver_test.xml"
        val testGraphSON = "$tempDir/plume/plume_driver_test.json"
        val testGryo = "$tempDir/plume/plume_driver_test.kryo"

        @AfterAll
        @JvmStatic
        fun tearDownAll() {
            val testFiles = arrayOf(File(testGraphML), File(testGraphSON), File(testGryo))
            Arrays.stream(testFiles).forEach { file: File ->
                try {
                    if (!file.delete()) logger.warn("Could not clear test resources.")
                } catch (e: Exception) {
                    logger.warn("Could not clear test resources.", e)
                }
            }
        }
    }

    override fun provideHook() = provideBuilder().build()

    override fun provideBuilder() = super.provideBuilder() as TinkerGraphDriver.Builder

    /**
     * Provides a hook with the contents of a serialized graph to populate the graph with.
     * Default is a [TinkerGraphDriver].
     *
     * @param existingGraph the path to a GraphML, GraphSON, or Gryo graph.
     * @return a hook connected to a graph database populated with the contents of the file at the given path.
     */
    fun provideHook(existingGraph: String): TinkerGraphDriver {
        return TinkerGraphDriver.Builder().useExistingGraph(existingGraph).build()
    }

    @Nested
    @DisplayName("Graph import file types")
    inner class ValidateGraphImportFileTypes {
        private lateinit var testGraph: Graph

        @BeforeEach
        fun setUp() {
            testGraph = TinkerGraph.open()
        }

        @Test
        fun testImportingGraphML() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            hook.exportGraph(testGraphML)
            assertDoesNotThrow<GremlinDriver> { provideHook(testGraphML) }
            val g = testGraph.traversal()
            g.io<Any>(testGraphML).read().iterate()
            Assertions.assertTrue(g.V().has(FileVertex.LABEL.toString(), "name", "Test1").hasNext())
            Assertions.assertTrue(g.V().has(FileVertex.LABEL.toString(), "name", "Test2").hasNext())
        }

        @Test
        fun testImportingGraphJSON() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            hook.exportGraph(testGraphSON)
            assertDoesNotThrow<GremlinDriver> { provideHook(testGraphSON) }
            val g = testGraph.traversal()
            g.io<Any>(testGraphSON).read().iterate()
            Assertions.assertTrue(g.V().has(FileVertex.LABEL.toString(), "name", "Test1").hasNext())
            Assertions.assertTrue(g.V().has(FileVertex.LABEL.toString(), "name", "Test2").hasNext())
        }

        @Test
        fun testImportingGryo() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            hook.exportGraph(testGryo)
            assertDoesNotThrow<GremlinDriver> { provideHook(testGryo) }
            val g = testGraph.traversal()
            g.io<Any>(testGryo).read().iterate()
            Assertions.assertTrue(g.V().has(FileVertex.LABEL.toString(), "name", "Test1").hasNext())
            Assertions.assertTrue(g.V().has(FileVertex.LABEL.toString(), "name", "Test2").hasNext())
        }

        @Test
        fun testImportingGraphThatDNE() {
            Assertions.assertThrows(IllegalArgumentException::class.java) { provideBuilder().useExistingGraph("/tmp/plume/DNE.kryo").build() }
        }

        @Test
        fun testImportingInvalidExtension() {
            Assertions.assertThrows(IllegalArgumentException::class.java) { provideBuilder().useExistingGraph("/tmp/plume/invalid.txt").build() }
        }
    }

    @Nested
    @DisplayName("Graph export file types")
    inner class ValidateGraphExportFileTypes {
        @Test
        fun testExportingGraphML() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            hook.exportGraph(testGraphML)
            Assertions.assertTrue(File(testGraphML).exists())
        }

        @Test
        fun testExportingGraphJSON() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            hook.exportGraph(testGraphSON)
            Assertions.assertTrue(File(testGraphSON).exists())
        }

        @Test
        fun testExportingGryo() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            hook.exportGraph(testGryo)
            Assertions.assertTrue(File(testGryo).exists())
        }

        @Test
        fun testExportingInvalidFileType() {
            val hook = provideHook()
            hook.createVertex(FileVertex("Test1", 0))
            hook.createVertex(FileVertex("Test2", 1))
            Assertions.assertThrows(IllegalArgumentException::class.java) { hook.exportGraph("/tmp/plume/invalid.txt") }
        }
    }
}