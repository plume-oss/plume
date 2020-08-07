package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A local variable
 */
class LocalVertex(
        val code: String,
        val name: String,
        val typeFullName: String,
        val lineNumber: Int,
        order: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "LocalVertex{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", typeFullName='" + typeFullName + '\'' +
                ", lineNumber=" + lineNumber +
                ", order=" + order +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocalVertex

        if (code != other.code) return false
        if (name != other.name) return false
        if (typeFullName != other.typeFullName) return false
        if (lineNumber != other.lineNumber) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + order
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.LOCAL
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.DECLARATION,
                VertexBaseTraits.LOCAL_LIKE,
                VertexBaseTraits.CALL_REPR)
    }

}