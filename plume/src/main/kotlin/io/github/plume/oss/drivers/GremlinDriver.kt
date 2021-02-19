package io.github.plume.oss.drivers

import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.commons.configuration.BaseConfiguration
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import overflowdb.Config
import scala.jdk.CollectionConverters
import kotlin.streams.toList
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
    open fun connect() {
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

    protected open fun prepareVertexProperties(v: NewNodeBuilder): Map<String, Any> {
        val propertyMap = CollectionConverters.MapHasAsJava(v.build().properties()).asJava().toMutableMap()
        propertyMap.computeIfPresent("DYNAMIC_TYPE_HINT_FULL_NAME") { _, value ->
            when (value) {
                is scala.collection.immutable.`$colon$colon`<*> -> value.head()
                else -> value
            }
        }
        return propertyMap
    }

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

    override fun getWholeGraph(): overflowdb.Graph = gremlinToPlume(g)

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): overflowdb.Graph {
        if (includeBody) return getMethodWithBody(fullName, signature)
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .has("FULL_NAME", fullName).has("SIGNATURE", signature)
            .outE(AST)
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(methodSubgraph.traversal())
    }

    private fun getMethodWithBody(fullName: String, signature: String): overflowdb.Graph {
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .has("FULL_NAME", fullName).has("SIGNATURE", signature)
            .repeat(un.outE(AST).inV()).emit()
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(methodSubgraph.traversal())
    }

    override fun getProgramStructure(): overflowdb.Graph {
        val sg = g.V().hasLabel(File.Label())
            .repeat(un.outE(AST).inV()).emit()
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        // Transfer type decl vertices to the result, this needs to be done with the tokens step to get all properties
        // from the remote server
        g.V().hasLabel(TYPE_DECL, FILE, NAMESPACE_BLOCK)
            .unfold<Vertex>()
            .valueMap<String>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>())
            .toStream()
            .filter { !sg.traversal().V(it[T.id]).hasNext() }
            .map { Pair(sg.addVertex(T.label, it[T.label], T.id, it[T.id]), it as Map<*, *>) }
            .forEach { (v, map) ->
                map.filter { it.key != T.id && it.key != T.label }
                    .forEach { (key, value) -> v.property(key.toString(), value) }
            }
        return gremlinToPlume(sg.traversal())
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
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(neighbourSubgraph.traversal())
    }

    override fun deleteVertex(id: Long, label: String?) {
        if (!g.V(id).hasNext()) return
        g.V(id).drop().iterate()
    }

    override fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, edge: String) {
        if (!exists(src, tgt, edge)) return
        g.V(src.id()).outE(edge).where(un.otherV().hasId(tgt.id())).drop().iterate()
    }

    override fun deleteMethod(fullName: String, signature: String) {
        val methodV = g.V().hasLabel(Method.Label())
            .has("FULL_NAME", fullName).has("SIGNATURE", signature)
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
            val props: Map<String, Any> = g.V().hasLabel(META_DATA).valueMap<String>()
                .by(un.unfold<Any>())
                .with(WithOptions.tokens)
                .next().mapKeys { it.key.toString() }
            VertexMapper.mapToVertex(props) as NewMetaDataBuilder
        } else {
            null
        }
    }

    /**
     * Converts a [GraphTraversalSource] instance to a [overflowdb.Graph] instance.
     *
     * @param g A [GraphTraversalSource] from the subgraph to convert.
     * @return The resulting [overflowdb.Graph].
     */
    private fun gremlinToPlume(g: GraphTraversalSource): overflowdb.Graph {
        val overflowGraph = newOverflowGraph()
        val f = { gt: GraphTraversal<Edge, Vertex> ->
            gt.valueMap<String>()
                .by(un.unfold<Any>())
                .with(WithOptions.tokens)
                .next()
                .mapKeys { k -> k.key.toString() }
        }
        val vertices = g.V().valueMap<Any>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>()).toStream()
            .map { props ->
                val nodeBuilder = VertexMapper.mapToVertex(props.mapKeys { it.key.toString() })
                val builtNode = nodeBuilder.build()
                val n = overflowGraph.addNode(nodeBuilder.id(), builtNode.label())
                builtNode.properties().foreachEntry { key, value -> n.setProperty(key, value) }
                Pair(n.id(), n)
            }.toList().toMap()
        g.E().barrier().valueMap<String>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>())
            .forEach {
                val edgeLabel = it[T.label].toString()
                val plumeSrc = vertices[VertexMapper.mapToVertex(f(g.E(it[T.id]).outV())).id()]
                val plumeTgt = vertices[VertexMapper.mapToVertex(f(g.E(it[T.id]).inV())).id()]
                plumeSrc?.addEdge(edgeLabel, plumeTgt)
            }
        return overflowGraph
    }

    private fun newOverflowGraph(): overflowdb.Graph = overflowdb.Graph.open(
        Config.withDefaults(),
        NodeFactories.allAsJava(),
        EdgeFactories.allAsJava()
    )

}