package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * An arbitrary identifier/reference.
 */
class IdentifierVertex(
        val name: String,
        val typeFullName: String,
        code: String,
        order: Int,
        argumentIndex: Int,
        lineNumber: Int,
        columnNumber: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.IDENTIFIER

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.EXPRESSION,
                VertexBaseTrait.LOCAL_LIKE
        )

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.REF to listOf(
                        VertexLabel.LOCAL,
                        VertexLabel.METHOD_PARAMETER_IN
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
        if (other !is IdentifierVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "IdentifierVertex(name='$name', typeFullName='$typeFullName')"
    }
}