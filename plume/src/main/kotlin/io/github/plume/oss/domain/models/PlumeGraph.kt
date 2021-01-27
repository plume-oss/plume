package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.EdgeLabel
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder

/**
 * Represents a CPG where vertices are [NewNodeBuilder] instances and edges are categorized by their [EdgeLabel] under
 * a [HashMap] which then returns the source [NewNodeBuilder] and a [HashSet] of all [NewNodeBuilder]s connected by the
 * [EdgeLabel].
 */
class PlumeGraph {

    private val vertices = HashSet<NewNodeBuilder>()
    private val edges = HashMap<EdgeLabel, HashMap<NewNodeBuilder, HashSet<NewNodeBuilder>>>()

    init {
        EdgeLabel.values().forEach { edges[it] = HashMap() }
    }

    /**
     * Returns an immutable set of all the [NewNodeBuilder]s in this graph.
     *
     * @return An immutable set of [NewNodeBuilder] instances.
     */
    fun vertices() = vertices.toSet()

    /**
     * Adds a [NewNodeBuilder] to the graph.
     *
     * @param v A [NewNodeBuilder] instance to add.
     */
    fun addVertex(v: NewNodeBuilder) { if (vertices.none { it.id() == v.id() }) vertices.add(v) }

    /**
     * Creates an edge between two [NewNodeBuilder]s in the graph.
     *
     * @param fromV The from [NewNodeBuilder].
     * @param toV   The to [NewNodeBuilder].
     * @param edge  The CPG edge label.
     */
    fun addEdge(fromV: NewNodeBuilder, toV: NewNodeBuilder, edge: EdgeLabel) {
        if (vertices.none { it.id() == fromV.id() }) vertices.add(fromV)
        if (vertices.none { it.id() == toV.id() }) vertices.add(toV)
        val edgeMap = edges[edge]!!
        if (edgeMap[fromV].isNullOrEmpty()) edgeMap[fromV] = HashSet<NewNodeBuilder>().apply { this.add(toV) }
        else edgeMap[fromV]!!.add(toV)
    }

    /**
     * Returns all the edges going out of the given [NewNodeBuilder].
     *
     * @param v The source [NewNodeBuilder].
     * @return a [HashMap] of all edges categorized by [EdgeLabel]s where target [NewNodeBuilder]s are the values.
     */
    fun edgesOut(v: NewNodeBuilder): HashMap<EdgeLabel, HashSet<NewNodeBuilder>> {
        val outMap = HashMap<EdgeLabel, HashSet<NewNodeBuilder>>()
        edges.keys.filter { !edges[it].isNullOrEmpty() && edges[it]?.containsKey(v) ?: false }
            .map { Pair(it, edges[it]) }
            .forEach { outMap[it.first] = it.second?.get(v)!!.toHashSet() }
        return outMap
    }

    /**
     * Returns all the edges going into the given [NewNodeBuilder].
     *
     * @param v The target [NewNodeBuilder].
     * @return a [HashMap] of all edges categorized by [EdgeLabel]s where target [NewNodeBuilder]s are the values.
     */
    fun edgesIn(v: NewNodeBuilder): HashMap<EdgeLabel, HashSet<NewNodeBuilder>> {
        val inMap = HashMap<EdgeLabel, HashSet<NewNodeBuilder>>()
        edges.keys.forEach { eLabel ->
            val vertexMap = edges[eLabel]
            if (!vertexMap.isNullOrEmpty()) {
                val inVerts = HashSet<NewNodeBuilder>()
                vertexMap.keys.forEach { srcV -> if (vertexMap[srcV]!!.contains(v)) inVerts.add(srcV) }
                if (inVerts.isNotEmpty()) inMap[eLabel] = inVerts
            }
        }
        return inMap
    }

    override fun toString(): String {
        return "PlumeGraph(vertices:${vertices.size}, edges:${
            edges.values.map { it.values }.flatten().flatten().count()
        })"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlumeGraph) return false

        if (vertices != other.vertices) return false
        if (edges != other.edges) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vertices.hashCode()
        result = 31 * result + edges.hashCode()
        return result
    }

}