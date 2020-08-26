package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ExpressionVertex
import java.util.*

/**
 * Literal/Constant
 */
class LiteralVertex(
        val name: String,
        val typeFullName: String,
        code: String,
        order: Int,
        argumentIndex: Int,
        lineNumber: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.LITERAL
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.EXPRESSION)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiteralVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "LiteralVertex(name='$name', typeFullName='$typeFullName')"
    }
}