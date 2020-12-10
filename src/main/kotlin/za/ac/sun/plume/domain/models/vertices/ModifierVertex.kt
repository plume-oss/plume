package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.ModifierType
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A modifier, e.g., static, public, private
 */
class ModifierVertex(val modifierType: ModifierType, order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.MODIFIER

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)

        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModifierVertex

        if (modifierType != other.modifierType) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = modifierType.hashCode()
        result = 31 * result + order
        return result
    }

    override fun toString(): String {
        return "ModifierVertex(modifierType=$modifierType, order=$order)"
    }
}