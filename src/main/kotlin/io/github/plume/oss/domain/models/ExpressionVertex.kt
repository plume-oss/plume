package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.VertexBaseTrait
import java.util.*

/**
 * Expression as a specialisation of tracking point.
 */
abstract class ExpressionVertex(
        code: String,
        val argumentIndex: Int,
        lineNumber: Int,
        columnNumber: Int,
        order: Int
) : CFGVertex(lineNumber, columnNumber, code, order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.TRACKING_POINT,
                VertexBaseTrait.AST_NODE,
                VertexBaseTrait.CFG_NODE,
                VertexBaseTrait.EXPRESSION,
                VertexBaseTrait.WITHIN_METHOD
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExpressionVertex) return false
        if (!super.equals(other)) return false

        if (argumentIndex != other.argumentIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + argumentIndex
        return result
    }
}