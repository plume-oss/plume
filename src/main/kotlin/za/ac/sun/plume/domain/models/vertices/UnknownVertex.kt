package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * A language-specific node.
 */
class UnknownVertex(
        val typeFullName: String,
        code: String,
        order: Int,
        argumentIndex: Int,
        lineNumber: Int,
        columnNumber: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.UNKNOWN

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.EXPRESSION)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
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
                ),
                EdgeLabel.AST to listOf(
                        VertexLabel.LITERAL,
                        VertexLabel.MEMBER,
                        VertexLabel.MODIFIER,
                        VertexLabel.ARRAY_INITIALIZER,
                        VertexLabel.CALL,
                        VertexLabel.LOCAL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.FIELD_IDENTIFIER,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.UNKNOWN,
                        VertexLabel.CONTROL_STRUCTURE
                )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnknownVertex) return false
        if (!super.equals(other)) return false

        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + UnknownVertex::class.java.name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "UnknownVertex(order=$order, typeFullName='$typeFullName')"
    }
}