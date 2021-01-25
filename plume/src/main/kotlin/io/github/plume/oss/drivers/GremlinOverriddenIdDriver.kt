package io.github.plume.oss.drivers

import io.github.plume.oss.util.PlumeKeyProvider
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.tinkerpop.gremlin.process.traversal.P

abstract class GremlinOverriddenIdDriver : GremlinDriver(), IOverridenIdDriver {

    override fun getVertexIds(lowerBound: Long, upperBound: Long): Set<Long> {
        if (!transactionOpen) openTx()
        val idSet = g.V().id().`is`(P.inside(lowerBound - 1, upperBound + 1)).toSet().map { it as Long }.toSet()
        if (transactionOpen) closeTx()
        return idSet
    }

    override fun prepareVertexProperties(v: NewNodeBuilder): Map<String, Any> {
        val propertyMap = super.prepareVertexProperties(v)
        // Get the implementing classes fields and values
        if (v.id() < 0L) v.id(PlumeKeyProvider.getNewId(this))
        return propertyMap
    }

}