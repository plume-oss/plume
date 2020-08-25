package za.ac.sun.plume.drivers

import org.apache.commons.configuration.BaseConfiguration
import org.apache.logging.log4j.LogManager
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as underscore
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.mappers.VertexMapper.Companion.propertiesToMap
import za.ac.sun.plume.domain.models.ASTVertex
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import java.lang.IllegalArgumentException
import java.util.*

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

    /**
     * Indicates if there is currently a transaction open or not.
     */
    var transactionOpen = false
        private set

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
        g = graph.traversal()
    }

    /**
     * Closes the current traversal and ends the current transaction if the database supports transactions.
     *
     * @throws IllegalArgumentException if the transaction is already closed.
     */
    protected open fun closeTx() {
        require(transactionOpen) { "There is no transaction currently open!" }
        try {
            g.close()
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

    private fun findVertexTraversal(v: PlumeVertex): GraphTraversal<Vertex, Vertex> =
            when (v) {
                is ASTVertex -> findVertexTraversal(v)
                is BindingVertex -> findVertexTraversal(v)
                is MetaDataVertex -> findVertexTraversal(v)
                is TypeVertex -> findVertexTraversal(v)
                else -> throw IllegalArgumentException("Unsupported PlumeVertex type")
            }

    private fun findVertexTraversal(v: ASTVertex): GraphTraversal<Vertex, Vertex> =
            run {
                val label = v.javaClass.getDeclaredField("LABEL").get(v).toString()
                g.V().has(label, "order", v.order)
            }

    private fun findVertexTraversal(v: BindingVertex): GraphTraversal<Vertex, Vertex> = g.V()
            .has(BindingVertex.LABEL.toString(), "name", v.name)
            .has("signature", v.signature)

    private fun findVertexTraversal(v: MetaDataVertex): GraphTraversal<Vertex, Vertex> = g.V()
            .has(MetaDataVertex.LABEL.toString(), "language", v.language)
            .has("version", v.version)

    private fun findVertexTraversal(v: TypeVertex): GraphTraversal<Vertex, Vertex> = g.V()
            .has(TypeVertex.LABEL.toString(), "name", v.name)
            .has("fullName", v.fullName)
            .has("typeDeclFullName", v.typeDeclFullName)

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

    override fun clearGraph() {
        if (!transactionOpen) openTx()
        g.V().drop().iterate()
        if (transactionOpen) closeTx()
    }

    /**
     * Given a [PlumeVertex], creates a [Vertex] and translates the object's field properties to key-value
     * pairs on the [Vertex] object. This is then added to this driver's [Graph].
     *
     * @param v the [PlumeVertex] to translate into a [Vertex].
     * @return the newly created [Vertex].
     */
    protected open fun createVertex(v: PlumeVertex): Vertex =
            try {
                if (!transactionOpen) openTx()
                val propertyMap = propertiesToMap(v)
                // Get the implementing class label parameter
                val label = propertyMap.remove("label") as String?
                // Get the implementing classes fields and values
                val newV = g.graph.addVertex(T.label, label, T.id, UUID.randomUUID())
                propertyMap.forEach { (key: String?, value: Any?) -> newV.property(key, value) }
                newV
            } finally {
                if (transactionOpen) closeTx()
            }

    /**
     * Wrapper method for creating an edge between two vertices. This wrapper method assigns a random UUID as the ID
     * for the edge.
     *
     * @param v1        the from [Vertex].
     * @param edgeLabel the CPG edge label.
     * @param v2        the to [Vertex].
     * @return the newly created [Edge].
     */
    private fun createEdge(v1: Vertex, edgeLabel: EdgeLabel, v2: Vertex): Edge {
        return if (this is TinkerGraphDriver) {
            v1.addEdge(edgeLabel.name, v2, T.id, UUID.randomUUID())
        } else {
            g.V(v1.id()).addE(edgeLabel.name).to(g.V(v2.id())).next()
        }
    }
}