package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.DeclarationVertex
import java.util.*

/**
 * Member of a class struct or union
 */
class MemberVertex(val code: String, name: String, val typeFullName: String, order: Int) : DeclarationVertex(name, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.MEMBER
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.DECLARATION,
                VertexBaseTrait.AST_NODE
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberVertex) return false
        if (!super.equals(other)) return false

        if (code != other.code) return false
        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "MemberVertex(code='$code', typeFullName='$typeFullName')"
    }
}