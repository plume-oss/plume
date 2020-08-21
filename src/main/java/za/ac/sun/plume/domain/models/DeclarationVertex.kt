package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import java.util.*

/**
 * Declare a variable by specifying its data type and name.
 */
abstract class DeclarationVertex(val name: String) : PlumeVertex {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.DECLARATION)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DeclarationVertex) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}