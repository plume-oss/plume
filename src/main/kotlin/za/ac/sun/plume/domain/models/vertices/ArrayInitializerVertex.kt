package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Initialization construct for arrays.
 */
class ArrayInitializerVertex(order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.ARRAY_INITIALIZER

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)

        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun toString(): String {
        return "ArrayInitializerVertex(order=$order)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayInitializerVertex

        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + ArrayInitializerVertex::class.java.name.hashCode()
        return result
    }
}