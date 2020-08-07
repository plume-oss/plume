package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Member of a class struct or union
 */
class MemberVertex(val code: String, val name: String, val typeFullName: String, order: Int) : ASTVertex(order) {
    override fun toString(): String {
        return "MemberVertex{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", typeFullName='" + typeFullName + '\'' +
                ", order=" + order +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MemberVertex

        if (code != other.code) return false
        if (name != other.name) return false
        if (typeFullName != other.typeFullName) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + order
        return result
    }


    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.MEMBER
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.DECLARATION,
                VertexBaseTraits.AST_NODE)
    }

}