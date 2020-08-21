package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.CFGVertex
import java.util.*

/**
 * A jump target made explicit in the code using a label.
 */
class JumpTargetVertex(
        val name: String,
        val argumentIndex: Int,
        lineNumber: Int,
        code: String,
        order: Int
) : CFGVertex(lineNumber, code, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.TYPE_PARAMETER
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.AST_NODE)
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