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
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import overflowdb.Graph
import java.io.File
import java.io.FileWriter

class GraphMLTest {

    companion object {
        val driver = (DriverFactory.invoke(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }
        private lateinit var graph: Graph
        private val tempDir = System.getProperty("java.io.tmpdir")
        private val testGraphML = "${tempDir}/plume/plume_driver_test.xml"

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            File(testGraphML).delete()
        }
    }

    @BeforeEach
    fun setUp() {
        // Create program data
        driver.addVertex(metaDataVertex)
        driver.addEdge(fileVertex, namespaceBlockVertex1, EdgeLabel.AST)
        driver.addEdge(namespaceBlockVertex1, namespaceBlockVertex2, EdgeLabel.AST)
        // Create method head
        driver.addEdge(typeDeclVertex, methodVertex, EdgeLabel.AST)
        driver.addEdge(methodVertex, fileVertex, EdgeLabel.SOURCE_FILE)
        driver.addEdge(methodVertex, mtdParamInVertex, EdgeLabel.AST)
        driver.addEdge(methodVertex, localVertex, EdgeLabel.AST)
        driver.addEdge(methodVertex, blockVertex, EdgeLabel.AST)
        driver.addEdge(methodVertex, blockVertex, EdgeLabel.CFG)
        // Create method body
        driver.addEdge(blockVertex, callVertex, EdgeLabel.AST)
        driver.addEdge(blockVertex, callVertex, EdgeLabel.CFG)
        driver.addEdge(callVertex, identifierVertex, EdgeLabel.AST)
        driver.addEdge(callVertex, literalVertex, EdgeLabel.AST)
        driver.addEdge(callVertex, identifierVertex, EdgeLabel.ARGUMENT)
        driver.addEdge(callVertex, literalVertex, EdgeLabel.ARGUMENT)
        driver.addEdge(blockVertex, returnVertex, EdgeLabel.AST)
        driver.addEdge(callVertex, returnVertex, EdgeLabel.CFG)
        driver.addEdge(methodVertex, mtdRtnVertex, EdgeLabel.AST)
        driver.addEdge(returnVertex, mtdRtnVertex, EdgeLabel.CFG)
        // Link dependencies
        driver.addEdge(identifierVertex, localVertex, EdgeLabel.REF)

        graph = driver.getWholeGraph()
    }

    @AfterEach
    fun tearDown() {
        driver.clearGraph()
    }

    @Test
    fun graphWriteTest() {
        GraphMLWriter.write(graph, FileWriter(testGraphML))
        driver.clearGraph()
        driver.importGraph(testGraphML)
        val otherGraph = driver.getWholeGraph()
        assertEquals(graph, otherGraph)
    }
}