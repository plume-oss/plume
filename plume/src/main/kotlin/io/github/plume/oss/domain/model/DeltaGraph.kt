/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.domain.model

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.drivers.TigerGraphDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import overflowdb.Graph
import overflowdb.Node

/**
 * This is based off of [io.shiftleft.passes.DiffGraph]. Where DiffGraphs do not propagate ID changes, this one does by
 * making use of [NewNodeBuilder] objects. These get assigned an ID when passed to [IDriver.addVertex] so no duplication
 * occurs.
 */
class DeltaGraph private constructor(val changes: List<Delta>) {

    private val logger = LogManager.getLogger(DeltaGraph::class.java)
    private constructor(builder: Builder) : this(builder.getChanges())

    /**
     * Applies the delta graph to an OverflowDB instance. To have valid IDs this must be passed into the driver with
     * the [DeltaGraph.apply] method first.
     *
     * @param existingG optionally one can write the deltas to the given OverflowDB graph.
     * @return An OverflowDB graph with the changes from this [DeltaGraph] applied to it.
     */
    fun toOverflowDb(existingG: Graph): Graph {
        fun exists(g: Graph, id: Long): Boolean {
            val maybeNode = g.node(id)
            return if (maybeNode == null) false
            else (maybeNode.id() == id)
        }

        fun d2g(g: Graph) {
            fun addNode(n: NewNodeBuilder<out NewNode>): Node {
                val b = n.build()
                val v = g.addNode(n.id(), b.label())
                b.properties().foreachEntry { key, value -> v.setProperty(key, value) }
                return v
            }
            changes.filterIsInstance<VertexAdd>().forEach { d -> if (!exists(g, d.n.id())) addNode(d.n) }
            changes.filterIsInstance<EdgeAdd>().forEach { d ->
                val src = if (exists(g, d.src.id())) g.node(d.src.id()) else addNode(d.src)
                val dst = if (exists(g, d.dst.id())) g.node(d.dst.id()) else g.node(d.dst.id()) ?: addNode(d.dst)
                try {
                    src.addEdge(d.e, dst)
                } catch (e: Exception) {
                    logger.error("Exception while adding edge between ${d.src} ($src) and ${d.dst} ($dst)", e)
                }
            }
            changes.filterIsInstance<VertexDelete>().forEach { d -> g.node(d.id)?.remove() }
            changes.filterIsInstance<EdgeDelete>().forEach { d ->
                val src = g.node(d.src.id())
                g.node(d.dst.id())?.let { dst ->
                    src.outE(d.e).asSequence().firstOrNull { it.inNode() == dst }?.remove()
                }
            }
        }
        return existingG.apply { d2g(this) }
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

        fun addVertex(n: NewNodeBuilder<out NewNode>) = apply { changes.add(VertexAdd(n)) }

        fun deleteVertex(n: NewNodeBuilder<out NewNode>) = apply { changes.add(VertexDelete(n.id(), n.build().label())) }

        fun deleteVertex(id: Long, label: String) = apply { changes.add(VertexDelete(id, label)) }

        fun addEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, e: String) = apply {
            changes.add(VertexAdd(src))
            changes.add(VertexAdd(tgt))
            changes.add(EdgeAdd(src, tgt, e))
        }

        fun deleteEdge(src: NewNodeBuilder<out NewNode>, tgt: NewNodeBuilder<out NewNode>, e: String) =
            apply { changes.add(EdgeDelete(src, tgt, e)) }

        fun addAll(otherChanges: List<Delta>) = apply { changes.addAll(otherChanges) }

        fun build() = DeltaGraph(this)
    }

    abstract class Delta
    data class VertexAdd(val n: NewNodeBuilder<out NewNode>) : Delta()
    data class VertexDelete(val id: Long, val label: String) : Delta()
    data class EdgeAdd(val src: NewNodeBuilder<out NewNode>, val dst: NewNodeBuilder<out NewNode>, val e: String) : Delta()
    data class EdgeDelete(val src: NewNodeBuilder<out NewNode>, val dst: NewNodeBuilder<out NewNode>, val e: String) : Delta()

}