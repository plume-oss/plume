package io.github.plume.oss.drivers

import io.github.plume.oss.Traversals
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import overflowdb.*
import scala.Tuple2
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

        graph = Graph.open(
            odbConfig,
            NodeFactories.allAsJava(),
            EdgeFactories.allAsJava()
        )
        connected = true
    }

    override fun addVertex(v: NewNodeBuilder) {
        if (exists(v)) return
        val newNode = v.build()
        val node = graph.addNode(newNode.label())
        newNode.properties().foreachEntry { key, value ->
            node.setProperty(key, value)
        }
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
        var srcNode = graph.node(fromV.hashCode().toLong())
        if (srcNode == null) {
            addVertex(fromV)
            srcNode = graph.node(fromV.hashCode().toLong())
        }
        var dstNode = graph.node(toV.hashCode().toLong())
        if (dstNode == null) {
            addVertex(toV)
            dstNode = graph.node(toV.hashCode().toLong())
        }

        try {
            srcNode.addEdge(edge.name, dstNode)
        } catch (exc: RuntimeException) {
            logger.error(exc.message, exc)
            throw PlumeSchemaViolationException(fromV, toV, edge)
        }
    }

    override fun maxOrder(): Int {
        return Traversals.maxOrder(graph)
    }

    override fun clearGraph(): IDriver = apply {
        Traversals.clearGraph(graph)
    }

    override fun getWholeGraph(): PlumeGraph {
        return nodesWithEdgesToPlumeGraph(Traversals.getWholeGraph(graph))
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
        if (includeBody) return edgeListToPlumeGraph(Traversals.getMethod(graph, fullName, signature))
        return edgeListToPlumeGraph(Traversals.getMethodStub(graph, fullName, signature))
    }

    private fun nodesWithEdgesToPlumeGraph(nodesWithEdges: List<Tuple2<StoredNode, List<Edge>>>): PlumeGraph {
        val plumeGraph = PlumeGraph()
        val plumeVertices = nodesWithEdges
            .map { x -> x._1 }
            .distinct()
            .map { node -> Pair(node.id(), node) }
            .toMap()

        val vertices = plumeVertices.values.map { v ->
            VertexMapper.mapToVertex(
                preProcessPropertyMap(v.propertyMap()) + mapOf<String, Any>(
                    "label" to v.label(),
                    "id" to v.id()
                )
            )
        }.toList().onEach { v -> v.let { x -> plumeGraph.addVertex(x) } }
        val edges = nodesWithEdges.flatMap { x -> x._2 }
        serializePlumeEdges(edges, vertices.map { Pair(it.id(), it) }.toMap(), plumeGraph)
        return plumeGraph
    }

    private fun edgeListToPlumeGraph(edges: List<Edge>): PlumeGraph {
        val plumeGraph = PlumeGraph()
        val plumeVertices = edges.flatMap { edge -> listOf(edge.inNode(), edge.outNode()) }
            .distinct()
            .map { node -> Pair(node.id(), node) }
            .toMap()

        val vertices = plumeVertices.values.map { v: NodeRef<NodeDb> ->
            val propMap = preProcessPropertyMap(v.propertyMap()) + mapOf<String, Any>(
                "label" to v.label(),
                "id" to v.id()
            )
            VertexMapper.mapToVertex(propMap)
        }.toList().onEach { v -> v.let { x -> plumeGraph.addVertex(x) } }
        serializePlumeEdges(edges, vertices.map { Pair(it.id(), it) }.toMap(), plumeGraph)
        return plumeGraph
    }

    private fun preProcessPropertyMap(props: Map<String, Any>): Map<String, Any> {
        val propertyMap = props.toMutableMap()
        propertyMap.computeIfPresent("DYNAMIC_TYPE_HINT_FULL_NAME") { _, value ->
            when (value) {
                is scala.collection.immutable.`$colon$colon`<*> -> value.head()
                else -> value
            }
        }
        return propertyMap
    }

    private fun serializePlumeEdges(
        edges: List<Edge>,
        plumeVertices: Map<Long, NewNodeBuilder>,
        plumeGraph: PlumeGraph
    ) {
        edges.forEach { edge ->
            val srcNode = plumeVertices[edge.outNode().id()]
            val dstNode = plumeVertices[edge.inNode().id()]
            if (srcNode != null && dstNode != null) {
                plumeGraph.addEdge(srcNode, dstNode, EdgeLabel.valueOf(edge.label()))
            }
        }
    }

    override fun getProgramStructure(): PlumeGraph {
        return edgeListToPlumeGraph(Traversals.getProgramStructure(graph))
    }

    override fun getNeighbours(v: NewNodeBuilder): PlumeGraph {
        return edgeListToPlumeGraph(Traversals.getNeighbours(graph, v.id()))
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

}