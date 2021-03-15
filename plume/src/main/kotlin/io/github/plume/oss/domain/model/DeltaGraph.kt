package io.github.plume.oss.domain.model

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder

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
     * Builds an [DeltaGraph] instance by accumulating changes.
     */
    class Builder {

        private val changes = mutableListOf<Delta>()

        /**
         * Returns a list of the accumulated changes.
         */
        fun getChanges() = changes.toList()

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