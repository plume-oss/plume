package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * A control structure such as if, while, or for
 */
class ControlStructureVertex(
        val name: String,
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

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "ControlStructureVertex(name='$name', order=$order)"
    }

}