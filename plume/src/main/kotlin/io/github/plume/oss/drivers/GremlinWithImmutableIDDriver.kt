package io.github.plume.oss.drivers

import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.tinkerpop.gremlin.process.traversal.P
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex

abstract class GremlinWithImmutableIDDriver : GremlinDriver() {

    override fun findVertexTraversal(v: NewNodeBuilder): GraphTraversal<Vertex, Vertex> =
        g.V().has(v.build().label(), "id", v.id())

    override fun createVertex(v: NewNodeBuilder): Vertex =
        try {
            if (!transactionOpen) openTx()
            val propertyMap = prepareVertexProperties(v)
            g.graph.addVertex(T.label, v.build().label(), "id", v.id()).apply {
                propertyMap.forEach { (key: String?, value: Any?) -> this.property(key, value) }
            }
        } finally {
            if (transactionOpen) closeTx()
        }

    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        if (!transactionOpen) openTx()
        val idSet =
            g.V().values<Long>("id").`is`(P.inside(lowerBound - 1, upperBound + 1)).toSet()
                .map { it as Long }.toSet()
        if (transactionOpen) closeTx()
        return idSet
    }

}