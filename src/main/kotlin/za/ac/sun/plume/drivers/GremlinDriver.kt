package za.ac.sun.plume.drivers

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
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeSchemaViolationException
import za.ac.sun.plume.domain.mappers.VertexMapper
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.checkSchemaConstraints
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.vertexToMap
import za.ac.sun.plume.domain.models.PlumeGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.FileVertex
import za.ac.sun.plume.domain.models.vertices.MethodVertex
import java.util.*
import kotlin.collections.HashMap
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as underscore

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
            connected = false
        } catch (e: Exception) {
            logger.warn("Exception thrown while attempting to close graph.", e)
        }
    }

    override fun addVertex(v: PlumeVertex) {
        if (!exists(v)) createVertex(v)
    }

    override fun exists(v: PlumeVertex): Boolean =
            try {
                if (!transactionOpen) openTx()
                findVertexTraversal(v).hasNext()
            } finally {
                if (transactionOpen) closeTx()
            }

    protected open fun findVertexTraversal(v: PlumeVertex): GraphTraversal<Vertex, Vertex> = g.V(v.hashCode().toString())

    override fun exists(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel): Boolean {
        try {
            if (!transactionOpen) openTx()
            if (!findVertexTraversal(fromV).hasNext() || !findVertexTraversal(toV).hasNext()) return false
            val a = findVertexTraversal(fromV).next()
            val b = findVertexTraversal(toV).next()
            return g.V(a).outE(edge.name).filter(underscore.inV().`is`(b)).hasLabel(edge.name).hasNext()
        } finally {
            if (transactionOpen) closeTx()
        }
    }

    override fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
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
     * Given a [PlumeVertex], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v The [PlumeVertex] to translate into a [Vertex].
     * @return The newly created [Vertex].
     */
    protected open fun createVertex(v: PlumeVertex): Vertex =
            try {
                if (!transactionOpen) openTx()
                val propertyMap = vertexToMap(v)
                // Get the implementing class label parameter
                val label = propertyMap.remove("label") as String?
                // Get the implementing classes fields and values
                g.graph.addVertex(T.label, label, T.id, v.hashCode().toString()).apply {
                    propertyMap.forEach { (key: String?, value: Any?) -> this.property(key, value) }
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
    private fun createEdge(v1: Vertex, edgeLabel: EdgeLabel, v2: Vertex): Edge {
        return if (this is TinkerGraphDriver) {
            v1.addEdge(edgeLabel.name, v2, T.id, UUID.randomUUID())
        } else {
            g.V(v1.id()).addE(edgeLabel.name).to(g.V(v2.id())).next()
        }
    }

    override fun getMethod(fullName: String, signature: String): PlumeGraph {
        if (!transactionOpen) openTx()
        val methodSubgraph = g.V().hasLabel(MethodVertex.LABEL.toString())
                .has("fullName", fullName).has("signature", signature)
                .repeat(underscore.outE(EdgeLabel.AST.toString()).inV()).emit()
                .inE()
                .subgraph("sg")
                .cap<Graph>("sg")
                .next()
        val result = gremlinToPlume(methodSubgraph)
        if (transactionOpen) closeTx()
        return result
    }

    override fun getProgramStructure(): PlumeGraph {
        if (!transactionOpen) openTx()
        val methodSubgraph = g.V().hasLabel(FileVertex.LABEL.toString())
                .repeat(underscore.outE(EdgeLabel.AST.toString()).inV()).emit()
                .inE()
                .subgraph("sg")
                .cap<Graph>("sg")
                .next()
        val result = gremlinToPlume(methodSubgraph)
        if (transactionOpen) closeTx()
        return result
    }

    override fun getNeighbours(v: PlumeVertex): PlumeGraph {
        if (!transactionOpen) openTx()
        val methodSubgraph = findVertexTraversal(v)
                .repeat(underscore.outE(EdgeLabel.AST.toString()).bothV())
                .times(1)
                .inE()
                .subgraph("sg")
                .cap<Graph>("sg")
                .next()
        val result = gremlinToPlume(methodSubgraph)
        if (transactionOpen) closeTx()
        return result
    }

    /**
     * Converts a [Graph] instance to a [PlumeGraph] instance.
     *
     * @param graph A Gremlin graph.
     * @return The resulting [PlumeGraph].
     */
    private fun gremlinToPlume(graph: Graph): PlumeGraph {
        val g = graph.traversal()
        val plumeGraph = PlumeGraph()
        g.V().toSet().map(this::gremlinToPlume).forEach { plumeGraph.addVertex(it) }
        g.E().valueMap<String>().with(WithOptions.tokens).forEachRemaining {
            val edgeLabel = EdgeLabel.valueOf(it[T.label].toString())
            val plumeSrc = gremlinToPlume(g.E(it[T.id]).outV().next())
            val plumeTgt = gremlinToPlume(g.E(it[T.id]).inV().next())
            plumeGraph.addEdge(plumeSrc, plumeTgt, edgeLabel)
        }
        return plumeGraph
    }

    /**
     * Converts a [Vertex] instance to a [PlumeVertex] instance.
     *
     * @param v A Gremlin vertex.
     * @return The resulting [PlumeVertex].
     */
    private fun gremlinToPlume(v: Vertex): PlumeVertex {
        val map = HashMap<String, Any>()
        map["label"] = v.label()
        v.properties<Any>().forEach { map[it.key()] = it.value() }
        return VertexMapper.mapToVertex(map)
    }
}