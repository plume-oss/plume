package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import java.util.*

/**
 * A binding of a METHOD into a TYPE_DECL
 */
class BindingVertex(val name: String, val signature: String) : PlumeVertex {
    companion object {
        @JvmField
        val LABEL = VertexLabel.BINDING

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.noneOf(VertexBaseTrait::class.java)

        @JvmField
        val VALID_OUT_EDGES = mapOf(EdgeLabel.REF to listOf(VertexLabel.METHOD))
    }

    override fun toString(): String {
        return "BindingVertex(name='$name', signature='$signature')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BindingVertex

        if (name != other.name) return false
        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }
}