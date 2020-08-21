package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
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
        order: Int
) : CFGVertex(lineNumber, code, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.METHOD
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.AST_NODE,
                VertexBaseTraits.DECLARATION,
                VertexBaseTraits.CFG_NODE
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (fullName != other.fullName) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }

    override fun toString(): String {
        return "MethodVertex(name='$name', fullName='$fullName', signature='$signature')"
    }
}