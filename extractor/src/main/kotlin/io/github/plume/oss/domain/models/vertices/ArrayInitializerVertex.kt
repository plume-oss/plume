package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ASTVertex
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