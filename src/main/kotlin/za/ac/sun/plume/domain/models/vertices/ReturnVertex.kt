package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * A return instruction.
 */
class ReturnVertex(
        lineNumber: Int,
        columnNumber: Int,
        order: Int,
        argumentIndex: Int,
        code: String
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.RETURN

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.EXPRESSION)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.AST to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.UNKNOWN,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.CONTROL_STRUCTURE
                ),
                EdgeLabel.CFG to listOf(VertexLabel.METHOD_RETURN),
                EdgeLabel.ARGUMENT to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
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
        if (other !is ReturnVertex) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun toString(): String {
        return "ReturnVertex(lineNumber=$lineNumber, order=$order, argumentIndex=$argumentIndex, code='$code')"
    }
}