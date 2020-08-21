package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * A structuring block in the AST.
 */
class BlockVertex(
        val name: String,
        code: String,
        order: Int,
        argumentIndex: Int,
        val typeFullName: String,
        lineNumber: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, order) {
    override fun toString(): String {
        return "BlockVertex{" +
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

        other as BlockVertex

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
        val LABEL = VertexLabels.BLOCK
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}