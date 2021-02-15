package io.github.plume.oss.util

import io.github.plume.oss.TestDomainResources
import io.github.plume.oss.TestDomainResources.Companion.STRING_1
import io.github.plume.oss.TestDomainResources.Companion.STRING_2
import io.github.plume.oss.TestDomainResources.Companion.fileVertex
import io.github.plume.oss.TestDomainResources.Companion.methodVertex
import io.github.plume.oss.drivers.DriverFactory
import io.github.plume.oss.drivers.GraphDatabase
import io.github.plume.oss.drivers.TinkerGraphDriver
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.nodes.File
import io.shiftleft.codepropertygraph.generated.nodes.StoredNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import overflowdb.Edge
import overflowdb.Node
import scala.Tuple2
import scala.collection.immutable.Seq
import scala.jdk.CollectionConverters

class DiffGraphUtilTest {

    val driver = (DriverFactory(GraphDatabase.TINKER_GRAPH) as TinkerGraphDriver).apply { connect() }

    @AfterEach
    fun tearDown() {
        TestDomainResources.simpleCpgVertices.forEach { it.id(-1) }
        driver.clearGraph()
    }

    @Test
    fun addNodeTest() {
        val builder = io.shiftleft.passes.DiffGraph.newBuilder()
        builder.addNode(fileVertex.build())

        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph().use { g ->
            assertTrue(g.nodes().asSequence().any { it.label() == File.Label() && it.property(NAME) == STRING_1 })
        }
    }

    @Test
    fun removeNodeTest() {
        driver.addVertex(fileVertex)
        driver.getWholeGraph().use { g ->
            assertTrue(g.nodes().asSequence().any { it.label() == File.Label() && it.property(NAME) == STRING_1 })
        }
        val builder = io.shiftleft.passes.DiffGraph.newBuilder()
        builder.removeNode(fileVertex.id())

        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph().use { g ->
            assertTrue(g.nodes().asSequence().none { it.label() == File.Label() && it.property(NAME) == STRING_1 })
        }
    }

    @Test
    fun createEdgeTest() {
        driver.getWholeGraph().use { g -> assertFalse(g.edges().hasNext()) }
        val builder = io.shiftleft.passes.DiffGraph.newBuilder()

        builder.addEdge(
            methodVertex.build(),
            fileVertex.build(),
            SOURCE_FILE,
            Seq.from(CollectionConverters.IterableHasAsScala(emptyList<Tuple2<String, Any>>()).asScala())
        )
        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph().use { g ->
            assertTrue(g.edges().hasNext())
            val edge = g.edges().next()
            assertTrue(edge.outNode().label() == methodVertex.build().label())
            assertTrue(edge.inNode().label() == fileVertex.build().label())
            assertTrue(edge.label() == SOURCE_FILE)
        }
    }

    @Test
    fun removeEdgeTest() {
        driver.addEdge(methodVertex, fileVertex, SOURCE_FILE)
        val edgeToRemove: Edge
        driver.getWholeGraph().use { g -> edgeToRemove = g.edges().next() }
        val builder = io.shiftleft.passes.DiffGraph.newBuilder()
        builder.removeEdge(edgeToRemove)

        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph().use { g -> assertFalse(g.edges().hasNext()) }
    }

    @Test
    fun updateNodePropertyTest() {
        driver.addVertex(fileVertex)
        driver.getWholeGraph()
            .use { g -> assertTrue(g.nodes(fileVertex.id()).asSequence().any { it.property(NAME) == STRING_1 }) }

        val builder = io.shiftleft.passes.DiffGraph.newBuilder()
        val storedNode: StoredNode
        driver.getWholeGraph().use { g -> storedNode = g.node(fileVertex.id()) as StoredNode }
        builder.addNodeProperty(storedNode, NAME, STRING_2)

        DiffGraphUtil.processDiffGraph(driver, builder.build())
        driver.getWholeGraph()
            .use { g -> assertTrue(g.nodes(fileVertex.id()).asSequence().any { it.property(NAME) == STRING_2 }) }
    }

}