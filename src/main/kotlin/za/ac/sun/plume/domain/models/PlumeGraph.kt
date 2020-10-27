package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.EdgeLabel

/**
 * Represents a CPG where vertices are [PlumeVertex] instances and edges are categorized by their [EdgeLabel] under
 * a [HashMap] which then returns the source [PlumeVertex] and a [HashSet] of all [PlumeVertex]s connected by the
 * [EdgeLabel].
 */
class PlumeGraph {

    private val vertices = HashSet<PlumeVertex>()
    private val edges = HashMap<EdgeLabel, HashMap<PlumeVertex, HashSet<PlumeVertex>>>()

    init {
        EdgeLabel.values().forEach { edges[it] = HashMap() }
    }

    /**
     * Returns an immutable set of all the [PlumeVertex]s in this graph.
     *
     * @return An immutable set of [PlumeVertex] instances.
     */
    fun vertices() = vertices.toSet()

    /**
     * Adds a [PlumeVertex] to the graph.
     *
     * @param v A [PlumeVertex] instance to add.
     */
    fun addVertex(v: PlumeVertex) = vertices.add(v)

    /**
     * Creates an edge between two [PlumeVertex]s in the graph.
     *
     * @param fromV The from [PlumeVertex].
     * @param toV   The CPG edge label.
     * @param edge  The to [PlumeVertex].
     */
    fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        vertices.add(fromV)
        vertices.add(toV)
        val edgeMap = edges[edge]!!
        if (edgeMap[fromV].isNullOrEmpty()) edgeMap[fromV] = HashSet<PlumeVertex>().apply { this.add(toV) }
        else edgeMap[fromV]!!.add(toV)
    }

    /**
     * Returns all the edges going out of the given [PlumeVertex].
     *
     * @param v The source [PlumeVertex].
     * @return a [HashMap] of all edges categorized by [EdgeLabel]s where target [PlumeVertex]s are the values.
     */
    fun edgesOut(v: PlumeVertex): HashMap<EdgeLabel, HashSet<PlumeVertex>> {
        val outMap = HashMap<EdgeLabel, HashSet<PlumeVertex>>()
        edges.keys.forEach { eLabel ->
            val vertexMap = edges[eLabel]
            if (!vertexMap.isNullOrEmpty()) {
                outMap[eLabel] = vertexMap[v]!!.toHashSet()
            }
        }
        return outMap
    }

    /**
     * Returns all the edges going into the given [PlumeVertex].
     *
     * @param v The target [PlumeVertex].
     * @return a [HashMap] of all edges categorized by [EdgeLabel]s where target [PlumeVertex]s are the values.
     */
    fun edgesIn(v: PlumeVertex): HashMap<EdgeLabel, HashSet<PlumeVertex>> {
        val inMap = HashMap<EdgeLabel, HashSet<PlumeVertex>>()
        edges.keys.forEach { eLabel ->
            val vertexMap = edges[eLabel]
            if (!vertexMap.isNullOrEmpty()) {
                val inVerts = HashSet<PlumeVertex>()
                vertexMap.keys.forEach { srcV -> if (vertexMap[srcV]!!.contains(v)) inVerts.add(srcV) }
                if (inVerts.isNotEmpty()) inMap[eLabel] = inVerts
            }
        }
        return inMap
    }

    override fun toString(): String {
        return "PlumeGraph(vertices:${vertices.size}, edges:${edges.values.map { it.entries }.flatten().count()})"
    }
}