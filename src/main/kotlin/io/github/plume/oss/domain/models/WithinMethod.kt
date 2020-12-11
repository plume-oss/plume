package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.VertexBaseTrait
import java.util.*

/**
 * Any vertex that can exist in a method.
 */
abstract class WithinMethod : PlumeVertex {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.WITHIN_METHOD
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}