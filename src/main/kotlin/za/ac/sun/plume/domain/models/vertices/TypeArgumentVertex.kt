package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Argument for a TYPE_PARAMETER that belongs to a TYPE. It binds another TYPE to a TYPE_PARAMETER
 */
class TypeArgumentVertex(order: Int) : ASTVertex(order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.TYPE_ARGUMENT
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)
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