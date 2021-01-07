package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ExpressionVertex
import java.util.*

/**
 * Reference to a type/class.
 */
class TypeRefVertex(
    val typeFullName: String,
    val dynamicTypeFullName: String,
    code: String,
    argumentIndex: Int,
    lineNumber: Int,
    columnNumber: Int,
    order: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.TYPE_REF

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
                VertexLabel.RETURN,
                VertexLabel.METHOD_REF,
                VertexLabel.TYPE_REF,
                VertexLabel.BLOCK,
                VertexLabel.JUMP_TARGET,
                VertexLabel.CONTROL_STRUCTURE,
                VertexLabel.UNKNOWN
            )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeRefVertex) return false

        if (order != other.order) return false
        if (typeFullName != other.typeFullName) return false
        if (dynamicTypeFullName != other.dynamicTypeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = TypeRefVertex::class.java.name.hashCode()
        result = 31 * result + order.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + dynamicTypeFullName.hashCode()
        return result
    }


}