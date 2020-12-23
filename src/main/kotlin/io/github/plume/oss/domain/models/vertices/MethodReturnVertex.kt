package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.EvaluationStrategy
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.CFGVertex
import io.github.plume.oss.domain.models.PlumeVertex
import java.util.*

/**
 * A formal method return.
 */
class MethodReturnVertex(
        val typeFullName: String,
        val evaluationStrategy: EvaluationStrategy,
        code: String,
        lineNumber: Int,
        columnNumber: Int,
        order: Int
) : CFGVertex(lineNumber, columnNumber, code, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.METHOD_RETURN

        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.CFG_NODE,
                VertexBaseTrait.TRACKING_POINT
        )

        @JvmField
        val VALID_OUT_EDGES =  emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodReturnVertex) return false
        if (!super.equals(other)) return false

        if (typeFullName != other.typeFullName) return false
        if (evaluationStrategy != other.evaluationStrategy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + evaluationStrategy.hashCode()
        return result
    }

    override fun toString(): String {
        return "MethodReturnVertex(typeFullName='$typeFullName', evaluationStrategy=$evaluationStrategy)"
    }
}