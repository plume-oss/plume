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
}