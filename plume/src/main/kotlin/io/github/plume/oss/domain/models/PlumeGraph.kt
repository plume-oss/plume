package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.EdgeLabel
import io.shiftleft.codepropertygraph.generated.nodes.NewNode

/**
 * Represents a CPG where vertices are [NewNode] instances and edges are categorized by their [EdgeLabel] under
 * a [HashMap] which then returns the source [NewNode] and a [HashSet] of all [NewNode]s connected by the
 * [EdgeLabel].
 */
class PlumeGraph {

    private val vertices = HashSet<NewNode>()
    private val edges = HashMap<EdgeLabel, HashMap<NewNode, HashSet<NewNode>>>()

    init {
        EdgeLabel.values().forEach { edges[it] = HashMap() }
    }

    /**
     * Returns an immutable set of all the [NewNode]s in this graph.
     *
     * @return An immutable set of [NewNode] instances.
     */
    fun vertices() = vertices.toSet()

    /**
     * Adds a [NewNode] to the graph.
     *
     * @param v A [NewNode] instance to add.
     */
    fun addVertex(v: NewNode) = vertices.add(v)

    /**
     * Creates an edge between two [NewNode]s in the graph.
     *
     * @param fromV The from [NewNode].
     * @param toV   The CPG edge label.
     * @param edge  The to [NewNode].
     */
    fun addEdge(fromV: NewNode, toV: NewNode, edge: EdgeLabel) {
        vertices.add(fromV)
        vertices.add(toV)
        val edgeMap = edges[edge]!!
        if (edgeMap[fromV].isNullOrEmpty()) edgeMap[fromV] = HashSet<NewNode>().apply { this.add(toV) }
        else edgeMap[fromV]!!.add(toV)
    }

    /**
     * Returns all the edges going out of the given [NewNode].
     *
     * @param v The source [NewNode].
     * @return a [HashMap] of all edges categorized by [EdgeLabel]s where target [NewNode]s are the values.
     */
    fun edgesOut(v: NewNode): HashMap<EdgeLabel, HashSet<NewNode>> {
        val outMap = HashMap<EdgeLabel, HashSet<NewNode>>()
        edges.keys.filter { !edges[it].isNullOrEmpty() && edges[it]?.containsKey(v) ?: false }
                .map { Pair(it, edges[it]) }
                .forEach { outMap[it.first] = it.second?.get(v)!!.toHashSet() }
        return outMap
    }

    /**
     * Returns all the edges going into the given [NewNode].
     *
     * @param v The target [NewNode].
     * @return a [HashMap] of all edges categorized by [EdgeLabel]s where target [NewNode]s are the values.
     */
    fun edgesIn(v: NewNode): HashMap<EdgeLabel, HashSet<NewNode>> {
        val inMap = HashMap<EdgeLabel, HashSet<NewNode>>()
        edges.keys.forEach { eLabel ->
            val vertexMap = edges[eLabel]
            if (!vertexMap.isNullOrEmpty()) {
                val inVerts = HashSet<NewNode>()
                vertexMap.keys.forEach { srcV -> if (vertexMap[srcV]!!.contains(v)) inVerts.add(srcV) }
                if (inVerts.isNotEmpty()) inMap[eLabel] = inVerts
            }
        }
        return inMap
    }

    override fun toString(): String {
        return "PlumeGraph(vertices:${vertices.size}, edges:${edges.values.map { it.values }.flatten().flatten().count()})"
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