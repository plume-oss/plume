package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.CFGVertex
import java.util.*

/**
 * A method/function/procedure
 */
class MethodVertex(
        val name: String,
        val fullName: String,
        val signature: String,
        code: String,
        lineNumber: Int,
        columnNumber: Int,
        order: Int
) : CFGVertex(lineNumber, columnNumber, code, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.METHOD

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.AST_NODE,
                VertexBaseTrait.DECLARATION,
                VertexBaseTrait.CFG_NODE
        )

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.AST to listOf(
                        VertexLabel.METHOD_RETURN,
                        VertexLabel.METHOD_PARAMETER_IN,
                        VertexLabel.MODIFIER,
                        VertexLabel.BLOCK,
                        VertexLabel.TYPE_PARAMETER,
                        VertexLabel.LOCAL
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
                ),
                EdgeLabel.SOURCE_FILE to listOf(VertexLabel.FILE)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodVertex) return false

        if (fullName != other.fullName) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = this.javaClass.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }

    override fun toString(): String {
        return "MethodVertex(name='$name', fullName='$fullName', signature='$signature')"
    }
}