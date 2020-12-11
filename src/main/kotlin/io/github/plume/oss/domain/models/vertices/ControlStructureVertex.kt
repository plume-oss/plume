package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ExpressionVertex
import java.util.*

/**
 * A control structure such as if, while, or for
 */
class ControlStructureVertex(
        code: String,
        lineNumber: Int,
        columnNumber: Int,
        order: Int,
        argumentIndex: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.CONTROL_STRUCTURE

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.EXPRESSION)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.AST to listOf(
                        VertexLabel.LITERAL,
                        VertexLabel.MODIFIER,
                        VertexLabel.ARRAY_INITIALIZER,
                        VertexLabel.CALL,
                        VertexLabel.LOCAL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.UNKNOWN,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF
                ),
                EdgeLabel.CONDITION to listOf(
                        VertexLabel.LITERAL,
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.UNKNOWN,
                        VertexLabel.ARRAY_INITIALIZER
                ),
                EdgeLabel.CFG to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.FIELD_IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.METHOD_RETURN,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.UNKNOWN
                )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ControlStructureVertex) return false
        if (!super.equals(other)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + ControlStructureVertex::class.java.name.hashCode()
        return result
    }

    override fun toString(): String {
        return "ControlStructureVertex(code='$code', order=$order)"
    }

}