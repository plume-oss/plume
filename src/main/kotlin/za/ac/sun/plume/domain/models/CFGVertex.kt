package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Any vertex that can occur as part of a control flow graph.
 */
abstract class CFGVertex(
        val lineNumber: Int,
        val code: String,
        order: Int
) : TrackingPoint(order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.CFG_NODE,
                VertexBaseTraits.TRACKING_POINT,
                VertexBaseTraits.AST_NODE
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CFGVertex) return false
        if (!super.equals(other)) return false

        if (lineNumber != other.lineNumber) return false
        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + code.hashCode()
        return result
    }
}