package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Any node that can occur in a data flow.
 */
abstract class TrackingPoint(order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.TRACKING_POINT,
                VertexBaseTraits.AST_NODE,
                VertexBaseTraits.WITHIN_METHOD
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackingPoint) return false
        if (!super.equals(other)) return false
        return true
    }
}