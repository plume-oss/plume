package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A method/function/procedure
 */
class MethodVertex(
        val name: String,
        val fullName: String,
        val signature: String,
        val lineNumber: Int,
        order: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "MethodVertex{" +
                "name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", signature='" + signature + '\'' +
                ", lineNumber=" + lineNumber +
                ", order=" + order +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodVertex

        if (name != other.name) return false
        if (fullName != other.fullName) return false
        if (signature != other.signature) return false
        if (lineNumber != other.lineNumber) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + signature.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + order
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.METHOD
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.AST_NODE,
                VertexBaseTraits.DECLARATION,
                VertexBaseTraits.CFG_NODE)
    }

}