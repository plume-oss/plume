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
}