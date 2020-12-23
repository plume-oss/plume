package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
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

        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }
}