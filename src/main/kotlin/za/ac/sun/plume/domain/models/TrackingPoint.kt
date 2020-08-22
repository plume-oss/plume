package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import java.util.*

/**
 * Any node that can occur in a data flow.
 */
abstract class TrackingPoint(order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.TRACKING_POINT,
                VertexBaseTrait.AST_NODE,
                VertexBaseTrait.WITHIN_METHOD
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackingPoint) return false
        if (!super.equals(other)) return false
        return true
    }
}