package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A language-specific node
 */
class UnknownVertex(
        val code: String,
        order: Int,
        val argumentIndex: Int,
        val lineNumber: Int,
        val typeFullName: String
) : ASTVertex(order) {
    override fun toString(): String {
        return "UnknownVertex{" +
                "code='" + code + '\'' +
                ", order=" + order +
                ", argumentIndex=" + argumentIndex +
                ", lineNumber=" + lineNumber +
                ", typeFullName='" + typeFullName + '\'' +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UnknownVertex

        if (code != other.code) return false
        if (order != other.order) return false
        if (argumentIndex != other.argumentIndex) return false
        if (lineNumber != other.lineNumber) return false
        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + order
        result = 31 * result + argumentIndex
        result = 31 * result + lineNumber
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.UNKNOWN
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}