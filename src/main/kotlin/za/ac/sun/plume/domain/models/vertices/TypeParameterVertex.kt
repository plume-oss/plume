package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Type parameter of TYPE_DECL or METHOD.
 */
class TypeParameterVertex(val name: String, order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.TYPE_PARAMETER
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)
        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeParameterVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "TypeParameterVertex(name='$name', order=$order)"
    }
}