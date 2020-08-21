package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Expression as a specialisation of tracking point.
 */
abstract class ExpressionVertex(
        code: String,
        val argumentIndex: Int,
        lineNumber: Int,
        order: Int
) : CFGVertex(lineNumber, code, order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.TRACKING_POINT,
                VertexBaseTraits.AST_NODE,
                VertexBaseTraits.CFG_NODE,
                VertexBaseTraits.EXPRESSION,
                VertexBaseTraits.WITHIN_METHOD
        )
    }
}