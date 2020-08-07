package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Literal/Constant
 */
class LiteralVertex(
        val name: String,
        order: Int,
        val argumentIndex: Int,
        val typeFullName: String,
        val lineNumber: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "LiteralVertex{" +
                "name='" + name + '\'' +
                ", order=" + order +
                ", argumentIndex=" + argumentIndex +
                ", typeFullName='" + typeFullName + '\'' +
                ", lineNumber=" + lineNumber +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LiteralVertex

        if (name != other.name) return false
        if (order != other.order) return false
        if (argumentIndex != other.argumentIndex) return false
        if (typeFullName != other.typeFullName) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + order
        result = 31 * result + argumentIndex
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + lineNumber
        return result
    }


    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.LITERAL
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}