package io.github.plume.oss.domain

import io.github.plume.oss.TestDomainResources
import io.github.plume.oss.TestDomainResources.Companion.fileVertex
import io.github.plume.oss.TestDomainResources.Companion.localVertex
import io.github.plume.oss.TestDomainResources.Companion.methodVertex
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeltaGraphTest {

    companion object {
        private val logger = LogManager.getLogger(DeltaGraphTest::class.java)
        private val driver = (DriverFactory.invoke(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).connect()

        @AfterAll
        fun tearDownAll() {
            driver.close()
        }
    }

    @AfterEach
    fun tearDown() {
        TestDomainResources.simpleCpgVertices.forEach { it.id(-1) }
        runCatching {
            driver.clearGraph()
        }.onFailure { e -> logger.debug(logger.warn("Could not clear test resources.", e)) }
    }

    @Test
    fun testVertexAdd() {
        assertEquals(-1L, fileVertex.id())
        DeltaGraph.Builder().addVertex(fileVertex).build().apply(driver)
        assertTrue(fileVertex.id() > -1L)
        driver.getWholeGraph().use { g -> assertNotNull(g.node(fileVertex.id())) }
    }

    @Test
    fun testVertexRemove() {
        assertEquals(-1L, fileVertex.id())
        driver.addVertex(fileVertex)
        assertTrue(fileVertex.id() > -1L)
        driver.getWholeGraph().use { g -> assertNotNull(g.node(fileVertex.id())) }
        DeltaGraph.Builder().deleteVertex(fileVertex).build().apply(driver)
        driver.getWholeGraph().use { g -> assertNull(g.node(fileVertex.id())) }
    }

    @Test
    fun testEdgeAdd() {
        assertEquals(-1L, fileVertex.id())
        assertEquals(-1L, methodVertex.id())
        DeltaGraph.Builder().addEdge(methodVertex, fileVertex, SOURCE_FILE).build().apply(driver)
        assertTrue(fileVertex.id() > -1L)
        assertTrue(methodVertex.id() > -1L)
        driver.getWholeGraph().use { g ->
            assertTrue(g.edges(SOURCE_FILE).hasNext())
            val e = g.edges(SOURCE_FILE).next()
            assertEquals(fileVertex.id(), e.inNode().id())
            assertEquals(methodVertex.id(), e.outNode().id())
        }
    }

    @Test
    fun testEdgeRemove() {
        assertEquals(-1L, fileVertex.id())
        assertEquals(-1L, methodVertex.id())
        driver.addEdge(methodVertex, fileVertex, SOURCE_FILE)
        assertTrue(fileVertex.id() > -1L)
        assertTrue(methodVertex.id() > -1L)
        DeltaGraph.Builder().deleteEdge(methodVertex, fileVertex, SOURCE_FILE).build().apply(driver)
        driver.getWholeGraph().use { g -> assertFalse(g.edges(SOURCE_FILE).hasNext()) }
    }

    @Test
    fun testCompoundOperations() {
        DeltaGraph.Builder().addVertex(fileVertex)
            .addEdge(methodVertex, fileVertex, SOURCE_FILE)
            .addEdge(methodVertex, localVertex, AST)
            .build()
            .apply(driver)
        assertTrue(fileVertex.id() > -1L)
        assertTrue(methodVertex.id() > -1L)
        assertTrue(localVertex.id() > -1L)
        driver.getWholeGraph().use { g ->
            assertEquals(3, g.nodeCount())
            assertEquals(2, g.edgeCount())
            assertTrue(g.edges(SOURCE_FILE).hasNext())
            val e1 = g.edges(SOURCE_FILE).next()
            assertEquals(fileVertex.id(), e1.inNode().id())
            assertEquals(methodVertex.id(), e1.outNode().id())
            assertTrue(g.edges(AST).hasNext())
            val e2 = g.edges(AST).next()
            assertEquals(localVertex.id(), e2.inNode().id())
            assertEquals(methodVertex.id(), e2.outNode().id())
        }
    }

}