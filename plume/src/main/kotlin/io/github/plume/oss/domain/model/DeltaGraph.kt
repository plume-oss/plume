package io.github.plume.oss.domain.model

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import overflowdb.Config
import overflowdb.Graph
import overflowdb.Node
import io.shiftleft.codepropertygraph.generated.edges.Factories as EdgeFactories
import io.shiftleft.codepropertygraph.generated.nodes.Factories as NodeFactories

/**
 * This is based off of [io.shiftleft.passes.DiffGraph]. Where DiffGraphs do not propagate ID changes, this one does by
 * making use of [NewNodeBuilder] objects. These get assigned an ID when passed to [IDriver.addVertex] so no duplication
 * occurs.
 */
class DeltaGraph private constructor(private val changes: List<Delta>) {

    private constructor(builder: Builder) : this(builder.getChanges())

    /**
     * Applies stored changes to the given driver.
     *
     * @param driver The driver to write changes to.
     */
    fun apply(driver: IDriver) {
        changes.forEach { d ->
            when (d) {
                is VertexAdd -> driver.addVertex(d.n)
                is VertexDelete -> driver.deleteVertex(d.id, d.label)
                is EdgeAdd -> driver.addEdge(d.src, d.dst, d.e)
                is EdgeDelete -> driver.deleteEdge(d.src, d.dst, d.e)
            }
        }
    }

    /**
     * Applies the delta graph to an OverflowDB instance. To have valid IDs this must be passed into the driver with
     * the [DeltaGraph.apply] method first.
     */
    fun toOverflowDb(): Graph =
        Graph.open(
            Config.withDefaults(),
            NodeFactories.allAsJava(),
            EdgeFactories.allAsJava()
        ).let { g ->
            fun addNode(n: NewNodeBuilder): Node {
                val b = n.build()
                val v = if (n.id() > 0) g.addNode(n.id(), b.label()) else g.addNode(b.label())
                b.properties().foreachEntry { key, value -> v.setProperty(key, value) }
                n.id(v.id())
                return v
            }
            changes.forEach { d ->
                when (d) {
                    is VertexAdd -> addNode(d.n)
                    is VertexDelete -> g.node(d.id)?.remove()
                    is EdgeAdd -> {
                        val src = if (g.node(d.src.id()) != null) g.node(d.src.id()) else addNode(d.src)
                        val dst = if (g.node(d.dst.id()) != null) g.node(d.dst.id()) else addNode(d.dst)
                        src.addEdge(d.e, dst)
                    }
                    is EdgeDelete -> {
                        val src = g.node(d.src.id())
                        g.node(d.dst.id())?.let { dst ->
                            src.outE(d.e).asSequence().firstOrNull { it.inNode() == dst }?.remove()
                        }
                    }
                }
            }
            return g
        }

    /**
     * Builds an [DeltaGraph] instance by accumulating changes.
     */
    class Builder {

        private val changes = mutableListOf<Delta>()

        /**
         * Returns a list of the accumulated changes.
         */
        fun getChanges() = optimizeChanges()

        /**
         * Will remove [VertexAdd] deltas if there is already an [EdgeAdd] associated with one of the vertices. This
         * will cut down on unnecessary database operations.
         */
        private fun optimizeChanges(): List<Delta> {
            val optimizedChanges = mutableListOf<Delta>()
            val vertexAdds = changes.filterIsInstance<VertexAdd>().toMutableList()
            changes.filterIsInstance<EdgeAdd>().forEach { ea ->
                vertexAdds.firstOrNull { it.n == ea.src }?.let { va -> vertexAdds.remove(va) }
                vertexAdds.firstOrNull { it.n == ea.dst }?.let { va -> vertexAdds.remove(va) }
            }
            changes.filterNot { it is VertexAdd }.toCollection(optimizedChanges)
            vertexAdds.toCollection(optimizedChanges)
            return optimizedChanges.toList()
        }

        fun addVertex(n: NewNodeBuilder) = apply { changes.add(VertexAdd(n)) }

        fun deleteVertex(n: NewNodeBuilder) = apply { changes.add(VertexDelete(n.id(), n.build().label())) }

        fun deleteVertex(id: Long, label: String) = apply { changes.add(VertexDelete(id, label)) }

        fun addEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, e: String) = apply { changes.add(EdgeAdd(src, tgt, e)) }

        fun deleteEdge(src: NewNodeBuilder, tgt: NewNodeBuilder, e: String) =
            apply { changes.add(EdgeDelete(src, tgt, e)) }

        fun build() = DeltaGraph(this)
    }

    abstract class Delta
    data class VertexAdd(val n: NewNodeBuilder) : Delta()
    data class VertexDelete(val id: Long, val label: String) : Delta()
    data class EdgeAdd(val src: NewNodeBuilder, val dst: NewNodeBuilder, val e: String) : Delta()
    data class EdgeDelete(val src: NewNodeBuilder, val dst: NewNodeBuilder, val e: String) : Delta()

}