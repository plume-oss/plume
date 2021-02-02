package io.github.plume.oss.drivers

import io.github.plume.oss.Traversals
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import overflowdb.*
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories


/**
 * Driver to create an OverflowDB database file from Plume's domain classes.
 */
class OverflowDbDriver : IDriver {

    private val logger = LogManager.getLogger(OverflowDbDriver::class.java)

    /**
     * Indicates whether the driver is connected to the graph database or not.
     */
    internal var connected = false

    private lateinit var graph: Graph

    /**
     * Where the database will be serialize/deserialize and overflow to disk.
     */
    var storageLocation: String = ""

    /**
     * Specifies if OverflowDb should write to disk when memory is constrained.
     */
    var overflow: Boolean = true

    /**
     * Percentage of the heap from when overflowing should begin to occur. Default is 80%.
     */
    var heapPercentageThreshold: Int = 80

    /**
     * If specified, OverflowDB will measure and report serialization/deserialization timing averages.
     */
    var serializationStatsEnabled: Boolean = false

    fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        val odbConfig = Config.withDefaults()
            .apply {
                if (this@OverflowDbDriver.storageLocation.isNotBlank())
                    this.withStorageLocation(this@OverflowDbDriver.storageLocation)
            }
            .apply { if (!overflow) this.disableOverflow() }
            .apply { if (serializationStatsEnabled) this.withSerializationStatsEnabled() }
            .withHeapPercentageThreshold(heapPercentageThreshold)

        graph = newOverflowGraph(odbConfig)
        connected = true
    }

    override fun addVertex(v: NewNodeBuilder) {
        if (exists(v)) return
        val newNode = v.build()
        val node = graph.addNode(newNode.label())
        newNode.properties().foreachEntry { key, value -> node.setProperty(key, value) }
        v.id(node.id())
    }

    override fun exists(v: NewNodeBuilder): Boolean {
        return (graph.node(v.id()) != null)
    }

    override fun exists(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel): Boolean {
        val srcNode = graph.node(fromV.id()) ?: return false
        val dstNode = graph.node(toV.id()) ?: return false
        return srcNode.out(edge.name).asSequence().toList().any { node -> node.id() == dstNode.id() }
    }

    override fun addEdge(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel) {
        if (!VertexMapper.checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(
            fromV,
            toV,
            edge
        )
        if (!exists(fromV)) addVertex(fromV)
        if (!exists(toV)) addVertex(toV)
        val srcNode = graph.node(fromV.id())
        val dstNode = graph.node(toV.id())

        try {
            srcNode.addEdge(edge.name, dstNode)
        } catch (exc: RuntimeException) {
            logger.error(exc.message, exc)
            throw PlumeSchemaViolationException(fromV, toV, edge)
        }
    }

    override fun clearGraph(): IDriver = apply {
        Traversals.clearGraph(graph)
    }

    override fun getWholeGraph(): Graph {
        val result = newOverflowGraph()
        graph.copyTo(result)
        return result
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): Graph {
        if (includeBody) return deepCopyGraph(Traversals.getMethod(graph, fullName, signature))
        return deepCopyGraph(Traversals.getMethodStub(graph, fullName, signature))
    }

    private fun deepCopyGraph(edges: List<Edge>): Graph {
        val graph = newOverflowGraph()
        deepCopyVertices(graph, edges)
        deepCopyEdges(edges, graph)
        return graph
    }

    private fun deepCopyVertices(graph: Graph, edges: List<Edge>): List<Node> {
        return edges.flatMap { edge -> listOf(edge.inNode(), edge.outNode()) }
            .distinct()
            .onEach { n ->
                val node = graph.addNode(n.id(), n.label())
                n.propertyMap().forEach { (key, value) -> node.setProperty(key as String?, value) }
            }
    }

    private fun deepCopyEdges(edges: List<Edge>, graph: Graph) {
        edges.forEach { edge ->
            val srcNode = graph.node(edge.outNode().id())
            val dstNode = graph.node(edge.inNode().id())
            if (srcNode != null && dstNode != null) {
                srcNode.addEdge(edge.label(), dstNode)
            }
        }
    }

    override fun getProgramStructure(): Graph {
        return deepCopyGraph(Traversals.getProgramStructure(graph))
    }

    override fun getNeighbours(v: NewNodeBuilder): Graph {
        return deepCopyGraph(Traversals.getNeighbours(graph, v.id()))
    }

    override fun deleteVertex(v: NewNodeBuilder) {
        graph.node(v.id())?.let { graph.remove(it) }
    }

    override fun deleteMethod(fullName: String, signature: String) {
        Traversals.deleteMethod(graph, fullName, signature)
    }

    override fun close() {
        require(connected) { "Cannot close a graph that is not already connected!" }
        try {
            graph.close()
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        } finally {
            connected = false
        }
    }

    private fun newOverflowGraph(odbConfig: Config = Config.withDefaults()): Graph = Graph.open(
        odbConfig,
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

}