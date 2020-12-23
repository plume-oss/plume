package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ASTVertex
import java.util.*

/**
 * Argument for a TYPE_PARAMETER that belongs to a TYPE. It binds another TYPE to a TYPE_PARAMETER.
 */
class TypeArgumentVertex(order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.TYPE_ARGUMENT

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.REF to listOf(VertexLabel.TYPE),
                EdgeLabel.BINDS_TO to listOf(VertexLabel.TYPE_PARAMETER)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeArgumentVertex) return false
        if (!super.equals(other)) return false
        return true
    }

    override fun toString(): String {
        return "TypeArgumentVertex(order=$order)"
    }
}