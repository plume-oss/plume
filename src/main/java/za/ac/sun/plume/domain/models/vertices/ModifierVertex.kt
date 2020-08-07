package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.ModifierTypes
import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A modifier, e.g., static, public, private
 */
class ModifierVertex(val name: ModifierTypes, order: Int) : ASTVertex(order) {
    override fun toString(): String {
        return "ModifierVertex{" +
                "name=" + name +
                ", order=" + order +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModifierVertex

        if (name != other.name) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + order
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.MODIFIER
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.AST_NODE)
    }

}