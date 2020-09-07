package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
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

        if (typeFullName != other.typeFullName) return false
        if (dynamicTypeFullName != other.dynamicTypeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 31 * typeFullName.hashCode()
        result = 31 * result + dynamicTypeFullName.hashCode()
        return result
    }


}