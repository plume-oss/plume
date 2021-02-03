package io.github.plume.oss.graphio

import io.github.plume.oss.TestDomainResources.Companion.blockVertex
import io.github.plume.oss.TestDomainResources.Companion.callVertex
import io.github.plume.oss.TestDomainResources.Companion.fileVertex
import io.github.plume.oss.TestDomainResources.Companion.identifierVertex
import io.github.plume.oss.TestDomainResources.Companion.literalVertex
import io.github.plume.oss.TestDomainResources.Companion.localVertex
import io.github.plume.oss.TestDomainResources.Companion.metaDataVertex
import io.github.plume.oss.TestDomainResources.Companion.mtdParamInVertex
import io.github.plume.oss.TestDomainResources.Companion.mtdRtnVertex
import io.github.plume.oss.TestDomainResources.Companion.methodVertex
import io.github.plume.oss.TestDomainResources.Companion.namespaceBlockVertex1
import io.github.plume.oss.TestDomainResources.Companion.namespaceBlockVertex2
import io.github.plume.oss.TestDomainResources.Companion.returnVertex
import io.github.plume.oss.TestDomainResources.Companion.typeDeclVertex
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import overflowdb.Graph
import java.io.File
import java.io.FileWriter

class GraphSONTest {

    companion object {
        val driver = (DriverFactory.invoke(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }

        private lateinit var graph: Graph
        private val tempDir = System.getProperty("java.io.tmpdir")
        private val testGraphSON = "${tempDir}/plume/plume_driver_test.json"

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            File(testGraphSON).delete()
        }
    }

    @BeforeEach
    fun setUp() {
        // Create program data
        driver.addVertex(metaDataVertex)
        driver.addEdge(fileVertex, namespaceBlockVertex1, AST)
        driver.addEdge(namespaceBlockVertex1, namespaceBlockVertex2, AST)
        // Create method head
        driver.addEdge(typeDeclVertex, methodVertex, AST)
        driver.addEdge(methodVertex, fileVertex, SOURCE_FILE)
        driver.addEdge(methodVertex, mtdParamInVertex, AST)
        driver.addEdge(methodVertex, localVertex, AST)
        driver.addEdge(methodVertex, blockVertex, AST)
        driver.addEdge(methodVertex, blockVertex, CFG)
        // Create method body
        driver.addEdge(blockVertex, callVertex, AST)
        driver.addEdge(blockVertex, callVertex, CFG)
        driver.addEdge(callVertex, identifierVertex, AST)
        driver.addEdge(callVertex, literalVertex, AST)
        driver.addEdge(callVertex, identifierVertex, ARGUMENT)
        driver.addEdge(callVertex, literalVertex, ARGUMENT)
        driver.addEdge(blockVertex, returnVertex, AST)
        driver.addEdge(callVertex, returnVertex, CFG)
        driver.addEdge(methodVertex, mtdRtnVertex, AST)
        driver.addEdge(returnVertex, mtdRtnVertex, CFG)
        // Link dependencies
        driver.addEdge(identifierVertex, localVertex, REF)

        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.close()
        graph.close()
    }

    @Test
    fun graphWriteTest() {
        GraphSONWriter.write(graph, FileWriter(testGraphSON))
        driver.clearGraph()
        driver.importGraph(testGraphSON)
        val otherGraph = driver.getWholeGraph()
        assertEquals(graph.nodeCount(), otherGraph.nodeCount())
        assertEquals(graph.edgeCount(), otherGraph.edgeCount())
    }
}