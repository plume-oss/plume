package io.github.plume.oss.drivers

import io.github.plume.oss.Traversals
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.util.ExtractorConst.TYPE_REFERENCED_EDGES
import io.github.plume.oss.util.ExtractorConst.TYPE_REFERENCED_NODES
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import overflowdb.*
import java.util.*
import kotlin.streams.asStream
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories


/**
 * Driver to create an OverflowDB database file from Plume's domain classes.
 */
class OverflowDbDriver internal constructor() : IDriver {

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
        private set

    /**
     * Specifies if OverflowDb should write to disk when memory is constrained.
     */
    var overflow: Boolean = true
        private set

    /**
     * Percentage of the heap from when overflowing should begin to occur. Default is 80%.
     */
    var heapPercentageThreshold: Int = 80
        private set

    /**
     * If specified, OverflowDB will measure and report serialization/deserialization timing averages.
     */
    var serializationStatsEnabled: Boolean = false
        private set

    /**
     * Set the storage location.
     *
     * @param value the storage location to overflow to e.g. /tmp/cpg.bin
     */
    fun storageLocation(value: String): OverflowDbDriver = apply { storageLocation = value }

    /**
     * Set whether the database overflows or not.
     *
     * @param value true to overflow, false to remain in memory.
     */
    fun overflow(value: Boolean): OverflowDbDriver = apply { overflow = value }

    /**
     * Set the percentage threshold before overflowing.
     *
     * @param value the percentage of the heap space.
     */
    fun heapPercentageThreshold(value: Int): OverflowDbDriver = apply { heapPercentageThreshold = value }

    /**
     * To set if serialization/deserialization timing averages should be reported.
     *
     * @param value true to report averages, false to not.
     */
    fun serializationStatsEnabled(value: Boolean): OverflowDbDriver = apply { serializationStatsEnabled = value }

    fun connect(): OverflowDbDriver = apply {
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

    override fun exists(v: NewNodeBuilder) = (graph.node(v.id()) != null)

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        val srcNode = graph.node(src.id()) ?: return false
        val dstNode = graph.node(tgt.id()) ?: return false
        return srcNode.out(edge).asSequence().toList().any { node -> node.id() == dstNode.id() }
    }

    override fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (!exists(src)) addVertex(src)
        if (!exists(tgt)) addVertex(tgt)
        val srcNode = graph.node(src.id())
        val dstNode = graph.node(tgt.id())

        try {
            srcNode.addEdge(edge, dstNode)
        } catch (exc: RuntimeException) {
            logger.error(exc.message, exc)
            throw PlumeSchemaViolationException(src, tgt, edge)
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

    override fun getMethod(fullName: String, includeBody: Boolean): Graph {
        if (includeBody) return deepCopyGraph(Traversals.getMethod(graph, fullName))
        return deepCopyGraph(Traversals.getMethodStub(graph, fullName))
    }

    override fun getMethodNames(): List<String> = Traversals.getMethodNames(graph)

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
        val g = deepCopyGraph(Traversals.getProgramStructure(graph))
        val ns = Traversals.getFiles(graph).toMutableList<StoredNode>()
            .toCollection(Traversals.getTypeDecls(graph).toMutableList<StoredNode>())
            .toCollection(Traversals.getNamespaceBlocks(graph).toMutableList<StoredNode>())
        ns.filter { g.node(it.id()) == null }
            .forEach { t ->
                val node = g.addNode(t.id(), t.label())
                t.propertyMap().forEach { (key, value) -> node.setProperty(key, value) }
            }
        return g
    }

    override fun getProgramTypeData(): Graph {
        val pg = newOverflowGraph()
        graph.nodes(*TYPE_REFERENCED_NODES).asSequence().forEach { n ->
            // Add vertices
            if (pg.node(n.id()) == null) {
                val node = pg.addNode(n.id(), n.label())
                n.propertyMap().forEach { (key, value) -> node.setProperty(key, value) }
            }
        }
        TYPE_REFERENCED_EDGES.forEach { tre ->
            graph.edges(tre).asSequence()
                .map { e ->
                    val srcNode = if (pg.node(e.outNode().id()) == null) {
                        val s = pg.addNode(e.outNode().id(), e.outNode().label())
                        e.outNode().propertyMap().forEach { (key, value) -> s.setProperty(key as String, value) }; s
                    } else {
                        pg.node(e.outNode().id())
                    }
                    val dstNode = if (pg.node(e.inNode().id()) == null) {
                        val d = pg.addNode(e.inNode().id(), e.inNode().label())
                        e.inNode().propertyMap().forEach { (key, value) -> d.setProperty(key as String, value) }; d
                    } else {
                        pg.node(e.inNode().id())
                    }
                    Triple(srcNode, dstNode, e.label())
                }.forEach { (src, dst, e) -> pg.node(src.id()).addEdge(e, pg.node(dst.id())) }
        }
        return pg
    }

    override fun getNeighbours(v: NewNodeBuilder): Graph = deepCopyGraph(Traversals.getNeighbours(graph, v.id()))

    override fun deleteVertex(id: Long, label: String?) {
        graph.node(id)?.let { graph.remove(it) }
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        val e = graph.node(src.id())?.outE(edge)?.next()
        if (e?.inNode()?.id() == tgt.id()) e.remove()
    }

    override fun deleteMethod(fullName: String) = Traversals.deleteMethod(graph, fullName)

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        val node = graph.node(id) ?: return
        node.setProperty(key, value)
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        val maybeMetaData = Traversals.getMetaData(graph)
        return if (maybeMetaData.isDefined) VertexMapper.mapToVertex(maybeMetaData.get()) as NewMetaDataBuilder else null
    }

    override fun getVerticesByProperty(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): List<NewNodeBuilder> =
        (if (label != null) graph.nodes(label) else graph.nodes()).asSequence()
            .filter { it.property(propertyKey) == propertyValue }
            .map(VertexMapper::mapToVertex)
            .toList()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getPropertyFromVertices(propertyKey: String, label: String?): List<T> =
        (if (label != null) graph.nodes(label) else graph.nodes()).asSequence()
            .filter { it.propertyKeys().contains(propertyKey) }
            .map { it.property(propertyKey) as T }
            .toList()

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