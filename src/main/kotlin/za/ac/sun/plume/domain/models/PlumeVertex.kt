package za.ac.sun.plume.domain.models

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import java.util.*

/**
 * A CFG vertex used by Plume vertex classes.
 */
interface PlumeVertex {
    companion object {
        @JvmField
        val LABEL = VertexLabel.UNKNOWN
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.noneOf(VertexBaseTrait::class.java)
    }
}