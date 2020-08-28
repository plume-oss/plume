package za.ac.sun.plume.util

import org.apache.tinkerpop.gremlin.structure.Direction
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph


object GraphDiff {

    private const val SORTKEY = "name"

    /**
     * add a sort key to the given edge
     * @param e
     * @return - the sort key
     */
    private fun addSortKey(e: Edge): String? {
        val sortKey: String = e.inVertex().label().toString() + "-" + e.outVertex().label()
        e.property(SORTKEY, sortKey)
        return sortKey
    }

    /**
     * get the difference of two Graphs
     * @param a
     * @param b
     * @return
     */
    fun diff(a: Graph, b: Graph): Graph {
        // step one: intersect both graphs
        val c: Graph = TinkerGraph.open()
        // copy all vertices of a
        a.traversal().V().forEachRemaining { v: Vertex -> c.addVertex(v.label()) }
        // copy all edges of a
        a.traversal().E().forEachRemaining { e: Edge ->
            val iva: Vertex = e.inVertex()
            val ova: Vertex = e.outVertex()
            val ivc: Vertex = c.traversal().V().hasLabel(iva.label()).next()
            val ovc: Vertex = c.traversal().V().hasLabel(ova.label()).next()
            addSortKey(e)
            val edge: Edge = ovc.addEdge(e.label(), ivc)
            addSortKey(edge)
        }
        // copy all vertices of b that are not yet in c
        b.traversal().V().forEachRemaining { v: Vertex ->
            if (c.traversal().V().hasLabel(v.label()).count().next() == 0L) {
                c.addVertex(v.label())
            }
            // copy all edges of b that are not yet in c
            b.traversal().E().forEachRemaining { e: Edge ->
                val ivb: Vertex = e.inVertex()
                val ovb: Vertex = e.outVertex()
                val ivc: Vertex = c.traversal().V().hasLabel(ivb.label()).next()
                val ovc: Vertex = c.traversal().V().hasLabel(ovb.label()).next()
                val sortKey = addSortKey(e)
                if (!c.traversal().E().has(SORTKEY, sortKey).hasNext()) {
                    val edge: Edge = ovc.addEdge(e.label(), ivc)
                    addSortKey(edge)
                }
            }
        }
        // step two: remove all edges that are "the same" in both graphs
        // inefficient version - would be much quicker with sorted lists
        // of edges ...
        c.traversal().E().forEachRemaining { e: Edge ->
            val sortKey: String = e.property<Any>(SORTKEY).value().toString()
            if (a.traversal().E().has(SORTKEY, sortKey).hasNext() &&
                    c.traversal().E().has(SORTKEY, sortKey).hasNext()) {
                e.remove()
            }
        }
        // step three remove all "unconnected" vertices
        c.traversal().V().forEachRemaining { v: Vertex -> if (!v.edges(Direction.BOTH).hasNext()) v.remove() }
        return c
    }
}