package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * Reference to a method instance.
 */
class MethodRefVertex(
        val methodInstFullName: String,
        val methodFullName: String,
        code: String,
        order: Int,
        argumentIndex: Int,
        lineNumber: Int,
        columnNumber: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.METHOD_REF

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.EXPRESSION)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
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
        if (other !is MethodRefVertex) return false
        if (!super.equals(other)) return false

        if (methodInstFullName != other.methodInstFullName) return false
        if (methodFullName != other.methodFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + methodInstFullName.hashCode()
        result = 31 * result + methodFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "MethodRefVertex(methodInstFullName='$methodInstFullName', methodFullName='$methodFullName')"
    }
}