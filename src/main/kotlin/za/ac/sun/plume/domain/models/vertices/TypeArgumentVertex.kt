package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
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