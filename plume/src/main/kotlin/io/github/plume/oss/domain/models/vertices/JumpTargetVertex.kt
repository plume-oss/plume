package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.CFGVertex
import java.util.*

/**
 * A jump target made explicit in the code using a label.
 */
class JumpTargetVertex(
        val name: String,
        val argumentIndex: Int,
        lineNumber: Int,
        columnNumber: Int,
        code: String,
        order: Int
) : CFGVertex(lineNumber, columnNumber, code, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.JUMP_TARGET

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)

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
                )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JumpTargetVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (argumentIndex != other.argumentIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + argumentIndex
        return result
    }

    override fun toString(): String {
        return "JumpTargetVertex(name='$name', argumentIndex=$argumentIndex)"
    }
}