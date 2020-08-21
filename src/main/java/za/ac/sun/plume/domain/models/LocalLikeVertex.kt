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
}