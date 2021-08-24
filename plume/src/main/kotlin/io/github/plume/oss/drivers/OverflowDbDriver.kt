/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.drivers

import io.github.plume.oss.Traversals
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.DriverTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import overflowdb.*
import scala.jdk.CollectionConverters
import java.util.*
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
        PlumeTimer.measure(DriverTimeKey.CONNECT_DESERIALIZE) {
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
    }

    override fun addVertex(v: NewNodeBuilder<out NewNode>) {
        if (exists(v)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { createVertex(v) }
    }

    private fun createVertex(v: NewNodeBuilder<out NewNode>) {
        val newNode = v.build()
        val node = graph.addNode(newNode.label())
        VertexMapper.stripUnusedProperties(
            v.build().label(),
            CollectionConverters.MapHasAsJava(newNode.properties()).asJava().toMutableMap()
        ).forEach { (key, value) -> node.setProperty(key, value) }
        v.id(node.id())
    }

    override fun exists(v: NewNodeBuilder<out NewNode>): Boolean {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            val maybeN = graph.node(v.id())
            res = (maybeN != null && maybeN.id() == v.id())
        }
        return res
    }

    override fun exists(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String): Boolean {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            if (exists(src) && exists(tgt)) {
                val srcNode = graph.node(src.id())
                val dstNode = graph.node(tgt.id())
                if (srcNode != null && dstNode != null)
                    res = srcNode.out(edge).asSequence().toList().any { node -> node.id() == dstNode.id() }
            }
        }
        return res
    }

    override fun addEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (!exists(src)) addVertex(src)
        if (!exists(tgt)) addVertex(tgt)
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { createEdge(src, tgt, edge) }
    }

    private fun createEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String) {
        val srcNode = graph.node(src.id())
        val dstNode = graph.node(tgt.id())
        try {
            srcNode.addEdge(edge, dstNode)
        } catch (exc: RuntimeException) {
            logger.error(exc.message, exc)
            throw PlumeSchemaViolationException(src, tgt, edge)
        }
    }

    override fun bulkTransaction(dg: DeltaGraph) {
        dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }.forEach { addVertex(it) }
        dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().distinct().forEach { addEdge(it.src, it.dst, it.e) }
        dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().forEach { deleteVertex(it.id, it.label) }
        dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().forEach { deleteEdge(it.src, it.dst, it.e) }
    }

    override fun clearGraph(): IDriver = apply {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { Traversals.clearGraph(graph) }
    }

    override fun getWholeGraph(): Graph {
        val result = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { graph.copyTo(result) }
        return result
    }

    override fun getMethod(fullName: String, includeBody: Boolean): Graph {
        if (includeBody) return deepCopyGraph(Traversals.getMethod(graph, fullName))
        return deepCopyGraph(Traversals.getMethodStub(graph, fullName))
    }

    private fun deepCopyGraph(edges: List<Edge>): Graph {
        val newGraph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            deepCopyVertices(newGraph, edges)
            deepCopyEdges(newGraph, edges)
        }
        return newGraph
    }

    private fun deepCopyVertices(graph: Graph, edges: List<Edge>): List<Node> {
        return edges.flatMap { edge -> listOf(edge.inNode(), edge.outNode()) }
            .distinct()
            .onEach { n ->
                val node = graph.addNode(n.id(), n.label())
                n.propertyMap().forEach { (key, value) -> node.setProperty(key as String?, value) }
            }
    }

    private fun deepCopyEdges(graph: Graph, edges: List<Edge>) {
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
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            val ns = Traversals.getFiles(graph).toMutableList<StoredNode>()
                .toCollection(Traversals.getTypeDecls(graph).toMutableList<StoredNode>())
                .toCollection(Traversals.getNamespaceBlocks(graph).toMutableList<StoredNode>())
            ns.filter { g.node(it.id()) == null }
                .forEach { t ->
                    val node = g.addNode(t.id(), t.label())
                    t.propertyMap().forEach { (key, value) -> node.setProperty(key, value) }
                }
        }
        return g
    }

    override fun getNeighbours(v: NewNodeBuilder<out NewNode>): Graph = deepCopyGraph(Traversals.getNeighbours(graph, v.id()))

    override fun deleteVertex(id: Long, label: String?) {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            graph.node(id)?.let { graph.remove(it) }
        }
    }

    override fun deleteEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            removeEdge(src, tgt, edge)
        }
    }

    private fun removeEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String) {
        val e = graph.node(src.id())?.outE(edge)?.next()
        if (e?.inNode()?.id() == tgt.id()) e.remove()
    }

    override fun deleteMethod(fullName: String) {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            Traversals.deleteMethod(graph, fullName)
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        var node: Node? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            node = graph.node(id)
        }
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            node?.setProperty(key, value)
        }
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        var maybeMetaData: NewMetaDataBuilder? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            val m = Traversals.getMetaData(graph)
            maybeMetaData = if (m.isDefined) VertexMapper.mapToVertex(m.get()) as NewMetaDataBuilder else null
        }
        return maybeMetaData
    }

    override fun getVerticesByProperty(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): List<NewNodeBuilder<out NewNode>> {
        val l = mutableListOf<NewNodeBuilder<out NewNode>>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            (if (label != null) graph.nodes(label) else graph.nodes()).asSequence()
                .filter { it.property(propertyKey) == propertyValue }
                .map(VertexMapper::mapToVertex)
                .toList()
                .toCollection(l)
        }
        return l
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> {
        val l = mutableListOf<T>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            (if (label != null) graph.nodes(label) else graph.nodes()).asSequence()
                .filter { it.propertyKeys().contains(propertyKey) }
                .map { it.property(propertyKey) as T }
                .toList()
                .toCollection(l)
        }
        return l
    }

    override fun getVerticesOfType(label: String): List<NewNodeBuilder <out NewNode>> {
        val l = mutableListOf<NewNodeBuilder<out NewNode>>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            graph.nodes(label).asSequence()
                .map(VertexMapper::mapToVertex)
                .toList()
                .toCollection(l)
        }
        return l
    }

    override fun close() {
        PlumeTimer.measure(DriverTimeKey.DISCONNECT_SERIALIZE) {
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

    private fun newOverflowGraph(odbConfig: Config = Config.withDefaults()): Graph = Graph.open(
        odbConfig,
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

}