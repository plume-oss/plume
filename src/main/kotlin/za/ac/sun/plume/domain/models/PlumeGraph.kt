package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.EdgeLabel

class PlumeGraph {

    private val vertices = HashSet<PlumeVertex>()
    private val edges = HashMap<EdgeLabel, HashMap<PlumeVertex, HashSet<PlumeVertex>>>()

    init {
        EdgeLabel.values().forEach { edges[it] = HashMap() }
    }

    fun vertices() = vertices.toSet()

    fun addVertex(v: PlumeVertex) = vertices.add(v)

    fun addEdge(fromV: PlumeVertex, toV: PlumeVertex, edge: EdgeLabel) {
        vertices.add(fromV)
        vertices.add(toV)
        val edgeMap = edges[edge]!!
        if (edgeMap[fromV].isNullOrEmpty()) edgeMap[fromV] = HashSet<PlumeVertex>().apply { this.add(toV) }
        else edgeMap[fromV]!!.add(toV)
    }

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
        return "PlumeGraph(vertices:${vertices.size}, edges:${edges.values.map{ it.entries }.flatten().count()})"
    }
}