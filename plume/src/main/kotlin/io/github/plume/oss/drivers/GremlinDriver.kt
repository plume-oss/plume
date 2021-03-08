package io.github.plume.oss.drivers

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.util.ExtractorConst.TYPE_REFERENCED_EDGES
import io.github.plume.oss.util.ExtractorConst.TYPE_REFERENCED_NODES
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.Method
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
import scala.collection.immutable.`$colon$colon`
import scala.jdk.CollectionConverters
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
        require(!connected) { "Please close the graph before trying to make another connection." }
        graph = TinkerGraph.open(config)
        g = graph.traversal()
        connected = true
    }


    /**
     * Attempts to close the graph database connection and resources.
     *
     * @throws IllegalArgumentException if one attempts to close an already closed graph.
     */
    override fun close() {
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

    override fun addVertex(v: NewNodeBuilder) {
        if (!exists(v)) createVertex(v)
    }

    override fun exists(v: NewNodeBuilder): Boolean = findVertexTraversal(v).hasNext()

    protected open fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> = g.V(v.id())

    override fun exists(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String): Boolean {
        if (!findVertexTraversal(src).hasNext() || !findVertexTraversal(tgt).hasNext()) return false
        val a = findVertexTraversal(src).next()
        val b = findVertexTraversal(tgt).next()
        return g.V(a).outE(edge).filter(un.inV().`is`(b)).hasLabel(edge).hasNext()
    }

    override fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!checkSchemaConstraints(src, tgt, edge)) throw PlumeSchemaViolationException(src, tgt, edge)
        if (exists(src, tgt, edge)) return
        val source = if (findVertexTraversal(src).hasNext()) findVertexTraversal(src).next()
        else createVertex(src)
        val target = if (findVertexTraversal(tgt).hasNext()) findVertexTraversal(tgt).next()
        else createVertex(tgt)
        createEdge(source, edge, target)
    }

    override fun clearGraph() = apply { g.V().drop().iterate() }

    /**
     * Given a [NewNode], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v The [NewNode] to translate into a [Vertex].
     * @return The newly created [Vertex].
     */
    protected open fun createVertex(v: NewNodeBuilder): Vertex {
        val newVertexTraversal = g.addV(v.build().label())
        prepareVertexProperties(v).forEach { (k, v) -> newVertexTraversal.property(k, v) }
        val newVertex = newVertexTraversal.next()
        v.id(newVertex.id() as Long)
        return newVertex
    }

    protected open fun prepareVertexProperties(v: NewNodeBuilder): Map<String, Any> =
        CollectionConverters.MapHasAsJava(v.build().properties()).asJava()
            .mapValues { (_, value) ->
                when (value) {
                    is `$colon$colon`<*> -> value.head()
                    else -> value
                }
            }.toMap()


    /**
     * Wrapper method for creating an edge between two vertices. This wrapper method assigns a random UUID as the ID
     * for the edge.
     *
     * @param v1   The from [Vertex].
     * @param edge The CPG edge label.
     * @param v2   The to [Vertex].
     * @return The newly created [Edge].
     */
    private fun createEdge(v1: Vertex, edge: String, v2: Vertex) = g.V(v1.id()).addE(edge).to(g.V(v2.id())).next()

    override fun getWholeGraph(): overflowdb.Graph {
        val graph = newOverflowGraph()
        g.V().valueMap<Any>()
            .by(un.unfold<Any>())
            .with(WithOptions.tokens).toList().map { VertexMapper.mapToVertex(mapVertexKeys(it)) }
            .forEach { addNodeToODB(graph, it) }
        g.E().toList()
            .map { Triple(graph.node(it.outVertex().id() as Long), graph.node(it.inVertex().id() as Long), it.label()) }
            .forEach { (src, dst, e) ->
                src?.addEdge(e, dst)
            }
        return graph
    }

    override fun getMethod(fullName: String, includeBody: Boolean): overflowdb.Graph {
        if (includeBody) return getMethodWithBody(fullName)
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .let {
                if (this !is JanusGraphDriver) it.has(FULL_NAME, fullName)
                else it.has("_$FULL_NAME", fullName)
            }
            .outE(AST)
            .toList()
        return gremlinToPlume(methodSubgraph)
    }

    override fun getMethodNames(): List<String> =
        g.V().hasLabel(METHOD)
            .values<String>("${if (this is JanusGraphDriver) "_" else ""}$FULL_NAME")
            .toList()

    private fun getMethodWithBody(fullName: String): overflowdb.Graph {
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .let {
                if (this !is JanusGraphDriver) it.has(FULL_NAME, fullName)
                else it.has("_$FULL_NAME", fullName)
            }
            .repeat(un.outE(AST).inV()).emit()
            .inE()
            .toList()
        return gremlinToPlume(methodSubgraph)
    }

    override fun getProgramStructure(): overflowdb.Graph {
        val fes = g.V().hasLabel(FILE)
            .repeat(un.outE(AST).inV()).emit()
            .inE()
            .toList()
        val graph = gremlinToPlume(fes)
        // Transfer type decl vertices to the result, this needs to be done with the tokens step to get all properties
        // from the remote server
        g.V().hasLabel(TYPE_DECL, FILE, NAMESPACE_BLOCK)
            .unfold<Vertex>()
            .valueMap<String>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>())
            .toList()
            .forEach { addNodeToODB(graph, VertexMapper.mapToVertex(mapVertexKeys(it))) }
        return graph
    }

    override fun getProgramTypeData(): overflowdb.Graph {
        val tes = g.V().hasLabel(
            TYPE_REFERENCED_NODES.first(),
            *TYPE_REFERENCED_NODES.copyOfRange(1, TYPE_REFERENCED_NODES.size)
        )
            .bothE(
                TYPE_REFERENCED_EDGES.first(),
                *TYPE_REFERENCED_EDGES.copyOfRange(1, TYPE_REFERENCED_EDGES.size)
            ).dedup().toList()
        val graph = gremlinToPlume(tes)
        g.V().hasLabel(
            TYPE_REFERENCED_NODES.first(),
            *TYPE_REFERENCED_NODES.copyOfRange(1, TYPE_REFERENCED_NODES.size)
        )
            .unfold<Vertex>()
            .valueMap<String>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>())
            .toList().forEach { addNodeToODB(graph, VertexMapper.mapToVertex(mapVertexKeys(it))) }
        return graph
    }

    override fun getNeighbours(v: NewNodeBuilder): overflowdb.Graph {
        val n = v.build()
        if (v is NewMetaDataBuilder) return newOverflowGraph().apply {
            val newNode = this.addNode(n.label())
            n.properties().foreachEntry { key, value -> newNode.setProperty(key, value) }
        }
        val neighbourSubgraph = findVertexTraversal(v)
            .repeat(un.outE(AST).bothV())
            .times(1)
            .inE()
            .toList()
        return gremlinToPlume(neighbourSubgraph)
    }

    override fun deleteVertex(id: Long, label: String?) {
        if (!g.V(id).hasNext()) return
        g.V(id).drop().iterate()
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        g.V(src.id()).outE(edge).where(un.otherV().hasId(tgt.id())).drop().iterate()
    }

    override fun deleteMethod(fullName: String) {
        val methodV = g.V().hasLabel(METHOD)
            .let {
                if (this !is JanusGraphDriver) it.has(FULL_NAME, fullName)
                else it.has("_$FULL_NAME", fullName)
            }
            .tryNext()
        if (methodV.isPresent) {
            g.V(methodV.get()).aggregate("x")
                .repeat(un.out(AST)).emit().barrier()
                .aggregate("x")
                .select<Vertex>("x")
                .unfold<Vertex>()
                .drop().iterate()
        }
    }

    override fun updateVertexProperty(id: Long, label: String?, key: String, value: Any) {
        if (!g.V(id).hasNext()) return
        g.V(id).property(key, value).iterate()
    }

    override fun getMetaData(): NewMetaDataBuilder? {
        return if (g.V().hasLabel(META_DATA).hasNext()) {
            val props: Map<String, Any> = mapVertexKeys(
                g.V().hasLabel(META_DATA).valueMap<Any>()
                    .by(un.unfold<Any>())
                    .with(WithOptions.tokens)
                    .next()
            )
            VertexMapper.mapToVertex(props) as NewMetaDataBuilder
        } else {
            null
        }
    }

    override fun getVerticesByProperty(
        propertyKey: String,
        propertyValue: Any,
        label: String?
    ): List<NewNodeBuilder> = (if (label != null) g.V().hasLabel(label) else g.V())
        .has("${if (this is JanusGraphDriver) "_" else ""}$propertyKey", propertyValue)
        .valueMap<Any>()
        .with(WithOptions.tokens)
        .by(un.unfold<Any>())
        .toList()
        .map(::mapVertexKeys)
        .map(VertexMapper::mapToVertex)

    override fun <T: Any> getPropertyFromVertices(propertyKey: String, label: String?): List<T> =
        (if (label != null) g.V().hasLabel(label) else g.V())
            .values<T>("${if (this is JanusGraphDriver) "_" else ""}$propertyKey")
            .toList()

    protected open fun mapVertexKeys(props: Map<Any, Any>) = props.mapKeys { it.key.toString() }

    private fun gremlinToPlume(es: List<Edge>): overflowdb.Graph {
        val overflowGraph = newOverflowGraph()
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
        return overflowGraph
    }

    private fun addNodeToODB(graph: overflowdb.Graph, nBuilder: NewNodeBuilder): Node? {
        val n = nBuilder.build()
        val maybeNode = graph.node(nBuilder.id())
        return if (maybeNode != null) return maybeNode
        else graph.addNode(nBuilder.id(), n.label()).apply {
            n.properties().foreachEntry { key, value -> this.setProperty(key, value) }
        }
    }

    private fun newOverflowGraph(): overflowdb.Graph = overflowdb.Graph.open(
        Config.withDefaults(),
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

}