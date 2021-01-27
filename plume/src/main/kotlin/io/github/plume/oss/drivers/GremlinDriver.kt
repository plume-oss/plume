package io.github.plume.oss.drivers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.models.PlumeGraph
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
import scala.jdk.CollectionConverters
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

    override fun exists(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel): Boolean {
        if (!findVertexTraversal(fromV).hasNext() || !findVertexTraversal(toV).hasNext()) return false
        val a = findVertexTraversal(fromV).next()
        val b = findVertexTraversal(toV).next()
        return g.V(a).outE(edge.name).filter(un.inV().`is`(b)).hasLabel(edge.name).hasNext()
    }

    override fun addEdge(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel) {
        if (!checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(fromV, toV, edge)
        if (exists(fromV, toV, edge)) return
        val source = if (findVertexTraversal(fromV).hasNext()) findVertexTraversal(fromV).next()
        else createVertex(fromV)
        val target = if (findVertexTraversal(toV).hasNext()) findVertexTraversal(toV).next()
        else createVertex(toV)
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
        val propertyMap = prepareVertexProperties(v)
        return g.graph
            .addVertex(T.label, v.build().label())
            .apply {
                propertyMap.forEach { (k, v) -> this.property(k, v) }
                v.id(this.id() as Long)
            }
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
     * @param v1        The from [Vertex].
     * @param edgeLabel The CPG edge label.
     * @param v2        The to [Vertex].
     * @return The newly created [Edge].
     */
    private fun createEdge(v1: Vertex, edgeLabel: EdgeLabel, v2: Vertex): Edge =
        g.V(v1.id()).addE(edgeLabel.name).to(g.V(v2.id())).next()

    override fun getWholeGraph(): PlumeGraph = gremlinToPlume(g)

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
        if (includeBody) return getMethodWithBody(fullName, signature)
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .has("FULL_NAME", fullName).has("SIGNATURE", signature)
            .outE(EdgeLabel.AST.name)
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(methodSubgraph.traversal())
    }

    private fun getMethodWithBody(fullName: String, signature: String): PlumeGraph {
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .has("FULL_NAME", fullName).has("SIGNATURE", signature)
            .repeat(un.outE(EdgeLabel.AST.name).inV()).emit()
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(methodSubgraph.traversal())
    }

    override fun getProgramStructure(): PlumeGraph {
        val programStructureSubGraph = g.V().hasLabel(File.Label())
            .repeat(un.outE(EdgeLabel.AST.name).inV()).emit()
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(programStructureSubGraph.traversal())
    }

    override fun getNeighbours(v: NewNodeBuilder): PlumeGraph {
        if (v is NewMetaData) return PlumeGraph().apply { addVertex(v) }
        val neighbourSubgraph = findVertexTraversal(v)
            .repeat(un.outE(EdgeLabel.AST.name).bothV())
            .times(1)
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        return gremlinToPlume(neighbourSubgraph.traversal())
    }

    override fun deleteVertex(v: NewNodeBuilder) {
        if (!exists(v)) return
        findVertexTraversal(v).drop().iterate()
    }

    override fun deleteMethod(fullName: String, signature: String) {
        val methodV = g.V().hasLabel(Method.Label())
            .has("FULL_NAME", fullName).has("SIGNATURE", signature)
            .tryNext()
        if (methodV.isPresent) {
            g.V(methodV.get()).aggregate("x")
                .repeat(un.out(EdgeLabel.AST.name)).emit().barrier()
                .aggregate("x")
                .select<Vertex>("x")
                .unfold<Vertex>()
                .drop().iterate()
        }
    }

    /**
     * Converts a [GraphTraversalSource] instance to a [PlumeGraph] instance.
     *
     * @param g A [GraphTraversalSource] from the subgraph to convert.
     * @return The resulting [PlumeGraph].
     */
    private fun gremlinToPlume(g: GraphTraversalSource): PlumeGraph {
        val plumeGraph = PlumeGraph()
        val f = { gt: GraphTraversal<Edge, Vertex> ->
            gt.valueMap<String>()
                .by(un.unfold<Any>())
                .with(WithOptions.tokens)
                .next()
                .mapKeys { k -> k.key.toString() }
        }
        g.V().valueMap<Any>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>()).toStream()
            .map { props -> VertexMapper.mapToVertex(props.mapKeys { it.key.toString() }) }
            .forEach { plumeGraph.addVertex(it) }
        g.E().barrier().valueMap<String>()
            .with(WithOptions.tokens)
            .by(un.unfold<Any>())
            .forEach {
                val edgeLabel = EdgeLabel.valueOf(it[T.label].toString())
                val plumeSrc = VertexMapper.mapToVertex(f(g.E(it[T.id]).outV()))
                val plumeTgt = VertexMapper.mapToVertex(f(g.E(it[T.id]).inV()))
                plumeGraph.addEdge(plumeSrc, plumeTgt, edgeLabel)
            }
        return plumeGraph
    }

}