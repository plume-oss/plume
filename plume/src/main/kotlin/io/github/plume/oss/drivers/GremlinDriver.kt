package io.github.plume.oss.drivers

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.exceptions.PlumeSchemaViolationException
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.mappers.VertexMapper.checkSchemaConstraints
import io.github.plume.oss.domain.mappers.VertexMapper.vertexToMap
import io.github.plume.oss.domain.models.PlumeGraph
import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.commons.configuration.BaseConfiguration
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as un

/**
 * The driver used by remote Gremlin connections.
 */
abstract class GremlinDriver : IDriver {
    private val logger = LogManager.getLogger(GremlinDriver::class.java)

    protected lateinit var graph: Graph
    protected lateinit var g: GraphTraversalSource
    protected var supportsTransactions: Boolean = false

    /**
     * The key-value configuration object used in creating the connection to the Gremlin server.
     */
    val config: BaseConfiguration = BaseConfiguration()

    /**
     * Indicates whether the driver is connected to the graph database or not.
     */
    var connected = false
        protected set

    /**
     * Indicates if there is currently a transaction open or not.
     */
    var transactionOpen = false
        protected set

    init {
        config.setProperty("gremlin.graph", "org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph")
    }

    /**
     * Connects to the graph database with the given configuration.
     *
     * @throws IllegalArgumentException if the graph database is already connected to.
     */
    open fun connect() {
        require(!connected) { "Please close the graph before trying to make another connection." }
        graph = TinkerGraph.open(config)
        supportsTransactions = graph.features().graph().supportsTransactions()
        if (!supportsTransactions) g = graph.traversal()
        connected = true
    }

    /**
     * Starts a new traversal and opens a transaction if the database supports transactions.
     *
     * @throws IllegalArgumentException if there is an already open transaction.
     */
    protected open fun openTx() {
        require(!transactionOpen) { "Please close the current transaction before creating a new one." }
        transactionOpen = true
    }

    /**
     * Closes the current traversal and ends the current transaction if the database supports transactions.
     *
     * @throws IllegalArgumentException if the transaction is already closed.
     */
    protected open fun closeTx() {
        require(transactionOpen) { "There is no transaction currently open!" }
        try {
            if (supportsTransactions) g.close()
        } catch (e: Exception) {
            logger.warn("Unable to close existing transaction! Object will be orphaned and a new traversal will continue.")
        } finally {
            transactionOpen = false
        }
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

    override fun exists(v: NewNodeBuilder): Boolean =
        try {
            if (!transactionOpen) openTx()
            findVertexTraversal(v).hasNext()
        } finally {
            if (transactionOpen) closeTx()
        }

    protected open fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> = g.V(v.id())

    override fun exists(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel): Boolean {
        try {
            if (!transactionOpen) openTx()
            if (!findVertexTraversal(fromV).hasNext() || !findVertexTraversal(toV).hasNext()) return false
            val a = findVertexTraversal(fromV).next()
            val b = findVertexTraversal(toV).next()
            return g.V(a).outE(edge.name).filter(un.inV().`is`(b)).hasLabel(edge.name).hasNext()
        } finally {
            if (transactionOpen) closeTx()
        }
    }

    override fun addEdge(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel) {
        if (!checkSchemaConstraints(fromV, toV, edge)) throw PlumeSchemaViolationException(fromV, toV, edge)
        if (exists(fromV, toV, edge)) return
        if (!transactionOpen) openTx()
        val source = if (findVertexTraversal(fromV).hasNext()) findVertexTraversal(fromV).next()
        else createVertex(fromV)
        if (!transactionOpen) openTx()
        val target = if (findVertexTraversal(toV).hasNext()) findVertexTraversal(toV).next()
        else createVertex(toV)
        if (!transactionOpen) openTx()
        try {
            createEdge(source, edge, target)
        } finally {
            if (transactionOpen) closeTx()
        }
    }

    override fun maxOrder(): Int =
        try {
            if (!transactionOpen) openTx()
            if (g.V().has("order").hasNext())
                g.V().has("order").order().by("order", Order.desc).limit(1).values<Any>("order").next() as Int
            else 0
        } finally {
            if (transactionOpen) closeTx()
        }

    protected fun setTraversalSource(g: GraphTraversalSource) {
        this.g = g
    }

    override fun clearGraph() = apply {
        if (!transactionOpen) openTx()
        g.V().drop().iterate()
        if (transactionOpen) closeTx()
    }

    /**
     * Given a [NewNode], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v The [NewNode] to translate into a [Vertex].
     * @return The newly created [Vertex].
     */
    protected open fun createVertex(v: NewNodeBuilder): Vertex =
        try {
            // TODO could use NewNode.properties() here
            if (!transactionOpen) openTx()
            val propertyMap: MutableMap<String, Any> = vertexToMap(v).apply { remove("label") }
            // Get the implementing classes fields and values
            g.graph.addVertex(T.label, v.build().label(), T.id, PlumeKeyProvider.getNewId(this)).apply {
                propertyMap.forEach { (key: String, value: Any) -> this.property(key, value) }
            }
        } finally {
            if (transactionOpen) closeTx()
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

    override fun getWholeGraph(): PlumeGraph {
        if (!transactionOpen) openTx()
        val plumeGraph = gremlinToPlume(g)
        if (transactionOpen) closeTx()
        return plumeGraph
    }

    override fun getMethod(fullName: String, signature: String, includeBody: Boolean): PlumeGraph {
        if (includeBody) return getMethod(fullName, signature)
        if (!transactionOpen) openTx()
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .has("fullName", fullName).has("signature", signature)
            .outE(EdgeLabel.AST.name)
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        val result = gremlinToPlume(methodSubgraph.traversal())
        if (transactionOpen) closeTx()
        return result
    }

    override fun getMethod(fullName: String, signature: String): PlumeGraph {
        if (!transactionOpen) openTx()
        val methodSubgraph = g.V().hasLabel(Method.Label())
            .has("fullName", fullName).has("signature", signature)
            .repeat(un.outE(EdgeLabel.AST.name).inV()).emit()
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        val result = gremlinToPlume(methodSubgraph.traversal())
        if (transactionOpen) closeTx()
        return result
    }

    override fun getProgramStructure(): PlumeGraph {
        if (!transactionOpen) openTx()
        val programStructureSubGraph = g.V().hasLabel(File.Label())
            .repeat(un.outE(EdgeLabel.AST.name).inV()).emit()
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        val result = gremlinToPlume(programStructureSubGraph.traversal())
        if (transactionOpen) closeTx()
        return result
    }

    override fun getNeighbours(v: NewNodeBuilder): PlumeGraph {
        if (v is NewMetaData) return PlumeGraph().apply { addVertex(v) }
        if (!transactionOpen) openTx()
        val neighbourSubgraph = findVertexTraversal(v)
            .repeat(un.outE(EdgeLabel.AST.name).bothV())
            .times(1)
            .inE()
            .subgraph("sg")
            .cap<Graph>("sg")
            .next()
        val result = gremlinToPlume(neighbourSubgraph.traversal())
        if (transactionOpen) closeTx()
        return result
    }

    override fun deleteVertex(v: NewNodeBuilder) {
        if (!exists(v)) return
        if (!transactionOpen) openTx()
        findVertexTraversal(v).drop().iterate()
        if (transactionOpen) closeTx()
    }

    override fun deleteMethod(fullName: String, signature: String) {
        if (!transactionOpen) openTx()
        val methodV = g.V().hasLabel(Method.Label())
            .has("fullName", fullName).has("signature", signature)
            .tryNext()
        if (methodV.isPresent) {
            g.V(methodV.get()).aggregate("x")
                .repeat(un.out(EdgeLabel.AST.name)).emit().barrier()
                .aggregate("x")
                .select<Vertex>("x")
                .unfold<Vertex>()
                .drop().iterate()
        }
        if (transactionOpen) closeTx()
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

    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        TODO("Not yet implemented")
    }
}