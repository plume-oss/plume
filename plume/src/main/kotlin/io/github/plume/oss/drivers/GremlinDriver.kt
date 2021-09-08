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

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.metrics.DriverTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.PropertyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.commons.configuration.BaseConfiguration
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import overflowdb.Config
import overflowdb.Node
import scala.jdk.CollectionConverters
import java.util.*
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as un

/**
 * The driver used by remote Gremlin connections.
 */
abstract class GremlinDriver : IDriver {
    private val logger = LogManager.getLogger(GremlinDriver::class.java)

    protected lateinit var graph: Graph
    protected lateinit var g: GraphTraversalSource

    /**
     * The key-value configuration object used in creating the connection to the Gremlin server.
     */
    val config: BaseConfiguration = BaseConfiguration()

    /**
     * Indicates whether the driver is connected to the graph database or not.
     */
    var connected = false
        protected set

    init {
        config.setProperty("gremlin.graph", "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph")
        config.setProperty("gremlin.tinkergraph.vertexIdManager", "LONG")
    }

    /**
     * Connects to the graph database with the given configuration.
     *
     * @throws IllegalArgumentException if the graph database is already connected to.
     */
    open fun connect(): GremlinDriver = apply {
        PlumeTimer.measure(DriverTimeKey.CONNECT_DESERIALIZE) {
            require(!connected) { "Please close the graph before trying to make another connection." }
            graph = TinkerGraph.open(config)
            g = graph.traversal()
            connected = true
        }
    }


    /**
     * Attempts to close the graph database connection and resources.
     *
     * @throws IllegalArgumentException if one attempts to close an already closed graph.
     */
    override fun close() {
        PlumeTimer.measure(DriverTimeKey.DISCONNECT_SERIALIZE) {
            require(connected) { "Cannot close a graph that is not already connected!" }
            try {
                g.close()
                graph.close()
            } catch (e: Exception) {
                logger.warn("Exception thrown while attempting to close graph.", e)
            } finally {
                connected = false
            }
        }
    }

    override fun addVertex(v: NewNodeBuilder<out NewNode>) {
        if (!exists(v)) createVertex(v)
    }

    override fun exists(v: NewNodeBuilder<out NewNode>): Boolean = findVertexTraversal(v).hasNext()

    protected open fun findVertexTraversal(v: NewNodeBuilder<out NewNode>): GraphTraversal<Vertex, Vertex> {
        var result: GraphTraversal<Vertex, Vertex>? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { result = g.V(v.id()) }
        return result!!
    }

    override fun exists(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String): Boolean {
        if (!findVertexTraversal(src).hasNext() || !findVertexTraversal(tgt).hasNext()) return false
        val a = findVertexTraversal(src).next()
        val b = findVertexTraversal(tgt).next()
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            res = g.V(a).outE(edge).filter(un.inV().`is`(b)).hasLabel(edge).hasNext()
        }
        return res
    }

    override fun addEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (exists(src, tgt, edge)) return
        val source = if (findVertexTraversal(src).hasNext()) findVertexTraversal(src).next()
        else createVertex(src)
        val target = if (findVertexTraversal(tgt).hasNext()) findVertexTraversal(tgt).next()
        else createVertex(tgt)
        createEdge(source, edge, target)
    }

    override fun bulkTransaction(dg: DeltaGraph) {
        val vAdds = mutableListOf<NewNodeBuilder<out NewNode>>()
        val eAdds = mutableListOf<DeltaGraph.EdgeAdd>()
        val vDels = mutableListOf<DeltaGraph.VertexDelete>()
        val eDels = mutableListOf<DeltaGraph.EdgeDelete>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { bulkTxReads(dg, vAdds, eAdds, vDels, eDels) }
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { bulkTxWrites(vAdds, eAdds, vDels, eDels) }
    }

    protected open fun bulkTxReads(
        dg: DeltaGraph,
        vAdds: MutableList<NewNodeBuilder<out NewNode>>,
        eAdds: MutableList<DeltaGraph.EdgeAdd>,
        vDels: MutableList<DeltaGraph.VertexDelete>,
        eDels: MutableList<DeltaGraph.EdgeDelete>,
    ) {
        dg.changes.filterIsInstance<DeltaGraph.VertexAdd>().map { it.n }
            .filterNot(::exists)
            .forEachIndexed { i, va -> if (vAdds.none { va === it }) vAdds.add(va.id(-(i + 1).toLong())) }
        dg.changes.filterIsInstance<DeltaGraph.EdgeAdd>().distinct().filterNot { exists(it.src, it.dst, it.e) }
            .toCollection(eAdds)
        dg.changes.filterIsInstance<DeltaGraph.VertexDelete>().filter { g.V(it.id).hasNext() }
            .toCollection(vDels)
        dg.changes.filterIsInstance<DeltaGraph.EdgeDelete>().filter { exists(it.src, it.dst, it.e) }
            .toCollection(eDels)
    }

    protected open fun bulkTxWrites(
        vAdds: MutableList<NewNodeBuilder<out NewNode>>,
        eAdds: MutableList<DeltaGraph.EdgeAdd>,
        vDels: MutableList<DeltaGraph.VertexDelete>,
        eDels: MutableList<DeltaGraph.EdgeDelete>,
    ) {
        vAdds.chunked(50).forEach { vs -> bulkAddNodes(vs) }
        eAdds.chunked(50).forEach { es -> bulkAddEdges(es) }
        vDels.forEach { deleteVertex(it.id, it.label) }
        eDels.forEach {
            findVertexTraversal(it.src).outE(it.e).where(un.otherV().V(findVertexTraversal(it.dst))).drop().iterate()
        }
    }

    protected open fun bulkAddNodes(vs: List<NewNodeBuilder<out NewNode>>) {
        if (vs.isEmpty()) return
        var gPtr: GraphTraversal<*, *>? = null
        val addedVs = mutableMapOf<String, NewNodeBuilder<out NewNode>>()
        vs.forEachIndexed { i, v ->
            if (!exists(v)) {
                PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
                    if (gPtr == null) gPtr = g.addV(v.build().label())
                    else gPtr?.addV(v.build().label())
                    prepareVertexProperties(v).forEach { (k, v) -> gPtr?.property(k, v) }
                    gPtr?.`as`("v$i")
                    addedVs["v$i"] = v
                }
            }
        }
        val keys = addedVs.keys.toList()
        when (keys.size) {
            1 -> gPtr?.select<Any>(keys.first())
            2 -> gPtr?.select<Any>(keys.first(), keys[1])
            else -> gPtr?.select<Any>(keys.first(), keys[1], *keys.minus(keys.first()).minus(keys[1]).toTypedArray())
        }
        val newVs = gPtr?.next()
        addedVs.forEach { (t, u) ->
            when (newVs) {
                is LinkedHashMap<*, *> -> {
                    when (val maybeV = newVs[t]) {
                        is Vertex -> assignId(u, maybeV)
                        else -> logger.warn("Unhandled result from bulk node add from map: ${maybeV?.javaClass}")
                    }
                }
                is Vertex -> assignId(u, newVs)
                else -> logger.warn("Unhandled result from bulk node add: ${newVs?.javaClass}")
            }

        }
    }

    protected open fun assignId(n: NewNodeBuilder<out NewNode>, v: Vertex): NewNodeBuilder<out NewNode> = n.id(v.id() as Long)

    protected open fun bulkAddEdges(es: List<DeltaGraph.EdgeAdd>) {
        if (es.isEmpty()) return
        var gPtr: GraphTraversal<*, *>? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            es.map { Triple(g.V(it.src.id()).next(), it.e, g.V(it.dst.id()).next()) }.forEach { (src, e, dst) ->
                if (gPtr == null) gPtr = g.V(src).addE(e).to(dst)
                else gPtr?.V(src)?.addE(e)?.to(dst)
            }
        }
        gPtr?.next()
    }

    override fun clearGraph() = apply {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { g.V().drop().iterate() }
    }

    /**
     * Given a [NewNode], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v The [NewNode] to translate into a [Vertex].
     * @return The newly created [Vertex].
     */
    protected open fun createVertex(v: NewNodeBuilder<out NewNode>): Vertex {
        var newVertex: Vertex? = null
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            val newVertexTraversal = g.addV(v.build().label())
            prepareVertexProperties(v).forEach { (k, v) -> newVertexTraversal.property(k, v) }
            newVertex = newVertexTraversal.next()
        }
        assignId(v, newVertex!!)
        return newVertex!!
    }

    protected open fun prepareVertexProperties(v: NewNodeBuilder<out NewNode>): Map<String, Any> =
        VertexMapper.prepareListsInMap(
            VertexMapper.handleProperties(
                v.build().label(), CollectionConverters.MapHasAsJava(v.build().properties()).asJava().toMutableMap()
            )
        ).toMap()

    /**
     * Wrapper method for creating an edge between two vertices.
     *
     * @param v1   The from [Vertex].
     * @param edge The CPG edge label.
     * @param v2   The to [Vertex].
     * @return The newly created [Edge].
     */
    private fun createEdge(v1: Vertex, edge: String, v2: Vertex) {
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { g.V(v1.id()).addE(edge).to(g.V(v2.id())).next() }
    }

    override fun getWholeGraph(): overflowdb.Graph {
        val graph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            g.V().valueMap<Any>()
                .by(un.unfold<Any>())
                .with(WithOptions.tokens).toList().map { VertexMapper.mapToVertex(mapVertexKeys(it)) }
                .forEach { addNodeToODB(graph, it) }
            g.E().toList()
                .map {
                    Triple(
                        graph.node(it.outVertex().id() as Long),
                        graph.node(it.inVertex().id() as Long),
                        it.label()
                    )
                }
                .forEach { (src, dst, e) ->
                    src?.addEdge(e, dst)
                }
        }
        return graph
    }

    override fun getMethod(fullName: String, includeBody: Boolean): overflowdb.Graph {
        if (includeBody) return getMethodWithBody(fullName)
        val methodSubgraph: MutableList<Edge> = mutableListOf()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            g.V().hasLabel(METHOD)
                .let {
                    if (this !is JanusGraphDriver) it.has(FULL_NAME, fullName)
                    else it.has("_$FULL_NAME", fullName)
                }
                .outE(AST)
                .toList()
                .toCollection(methodSubgraph)
        }
        return gremlinToPlume(methodSubgraph)
    }

    private fun getMethodWithBody(fullName: String): overflowdb.Graph {
        val methodSubgraph: MutableList<Edge> = mutableListOf()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            g.V().hasLabel(METHOD)
                .let {
                    if (this !is JanusGraphDriver) it.has(FULL_NAME, fullName)
                    else it.has("_$FULL_NAME", fullName)
                }
                .repeat(un.outE(AST).inV()).emit()
                .inE()
                .toList()
                .toCollection(methodSubgraph)
        }
        return gremlinToPlume(methodSubgraph)
    }

    override fun getNeighbours(v: NewNodeBuilder<out NewNode>): overflowdb.Graph {
        val n = v.build()
        val neighbourSubgraph: MutableList<Edge> = mutableListOf()
        if (v is NewMetaDataBuilder) return newOverflowGraph().apply {
            val newNode = this.addNode(n.label())
            n.properties().foreachEntry { key, value -> newNode.setProperty(key, value) }
        }
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            findVertexTraversal(v)
                .repeat(un.outE(AST).bothV())
                .times(1)
                .inE()
                .toList()
                .toCollection(neighbourSubgraph)
        }
        return gremlinToPlume(neighbourSubgraph)
    }

    override fun deleteVertex(id: Long, label: String?) {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { res = g.V(id).hasNext() }
        if (!res) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { g.V(id).drop().iterate() }
    }

    override fun deleteEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, edge: String) {
        if (!exists(src, tgt, edge)) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
            g.V(src.id()).outE(edge).where(un.otherV().hasId(tgt.id())).drop().iterate()
        }
    }

    override fun deleteMethod(fullName: String) {
        var methodV: Optional<Vertex> = Optional.empty()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            methodV = g.V().hasLabel(METHOD)
                .let {
                    if (this !is JanusGraphDriver) it.has(FULL_NAME, fullName)
                    else it.has("_$FULL_NAME", fullName)
                }
                .tryNext()
        }
        if (methodV.isPresent) {
            PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) {
                g.V(methodV.get()).aggregate("x")
                    .repeat(un.out(AST)).emit().barrier()
                    .aggregate("x")
                    .select<Vertex>("x")
                    .unfold<Vertex>()
                    .drop().iterate()
            }
        }

    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        var res = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { res = g.V(id).hasNext() }
        if (!res) return
        PlumeTimer.measure(DriverTimeKey.DATABASE_WRITE) { g.V(id).property(key, value).iterate() }
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        var hasNext = false
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) { hasNext = g.V().hasLabel(META_DATA).hasNext() }
        return if (hasNext) {
            val props = mutableMapOf<String, Any>()
            PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
                mapVertexKeys(
                    g.V().hasLabel(META_DATA).valueMap<Any>()
                        .by(un.unfold<Any>())
                        .with(WithOptions.tokens)
                        .next()
                ).toMap(props)
            }
            VertexMapper.mapToVertex(props) as NewMetaDataBuilder
        } else {
            null
        }
    }

    override fun getVerticesByProperty(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): List<NewNodeBuilder<out NewNode>> {
        val l = mutableListOf<NewNodeBuilder<out NewNode>>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            (if (label != null) g.V().hasLabel(label) else g.V())
                .has("${if (this is JanusGraphDriver) "_" else ""}$propertyKey", propertyValue)
                .valueMap<Any>()
                .with(WithOptions.tokens)
                .by(un.unfold<Any>())
                .toList()
                .map(::mapVertexKeys)
                .map(VertexMapper::mapToVertex)
                .toCollection(l)
        }
        return l
    }

    override fun <T : Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> {
        val l = mutableListOf<T>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            (if (label != null) g.V().hasLabel(label) else g.V())
                .values<T>("${if (this is JanusGraphDriver) "_" else ""}$propertyKey")
                .toList()
                .toCollection(l)
        }
        return l
    }

    override fun getVerticesOfType(label: String): List<NewNodeBuilder<out NewNode>> {
        val l = mutableListOf<NewNodeBuilder<out NewNode>>()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            g.V().hasLabel(label)
                .valueMap<Any>()
                .with(WithOptions.tokens)
                .by(un.unfold<Any>())
                .toList()
                .map(::mapVertexKeys)
                .map(VertexMapper::mapToVertex)
                .toCollection(l)
        }
        return l
    }

    protected open fun mapVertexKeys(props: Map<Any, Any>) = props.mapKeys { it.key.toString() }

    private fun gremlinToPlume(es: List<Edge>): overflowdb.Graph {
        val overflowGraph = newOverflowGraph()
        PlumeTimer.measure(DriverTimeKey.DATABASE_READ) {
            es.map {
                Triple(
                    g.V(it.outVertex().id()).valueMap<Any>().with(WithOptions.tokens).by(un.unfold<Any>()).next(),
                    g.V(it.inVertex().id()).valueMap<Any>().with(WithOptions.tokens).by(un.unfold<Any>()).next(),
                    it.label()
                )
            }.map { (src, dst, e) ->
                Triple(
                    addNodeToODB(overflowGraph, VertexMapper.mapToVertex(mapVertexKeys(src))),
                    addNodeToODB(overflowGraph, VertexMapper.mapToVertex(mapVertexKeys(dst))),
                    e
                )
            }.forEach { (src, dst, e) -> src?.addEdge(e, dst) }
        }
        return overflowGraph
    }

    protected fun addNodeToODB(graph: overflowdb.Graph, nBuilder: NewNodeBuilder<out NewNode>): Node? {
        val n = nBuilder.build()
        val maybeNode = graph.node(nBuilder.id())
        return if (maybeNode != null) return maybeNode
        else graph.addNode(nBuilder.id(), n.label()).apply {
            n.properties().foreachEntry { key, value -> this.setProperty(key, value) }
        }
    }

    protected fun newOverflowGraph(): overflowdb.Graph = Cpg.emptyGraph()

}