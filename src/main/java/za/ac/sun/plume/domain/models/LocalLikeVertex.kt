package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Formal input parameters, locals, and identifiers.
 */
abstract class LocalLikeVertex(val name: String) : PlumeVertex {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.LOCAL_LIKE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalLikeVertex) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "LocalLikeVertex(name='$name')"
    }
}