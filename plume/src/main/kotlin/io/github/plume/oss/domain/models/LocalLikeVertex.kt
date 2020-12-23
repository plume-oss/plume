package io.github.plume.oss.domain.models

import io.github.plume.oss.domain.enums.VertexBaseTrait
import java.util.*

/**
 * Formal input parameters, locals, and identifiers.
 */
abstract class LocalLikeVertex(val name: String, order:Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.LOCAL_LIKE, VertexBaseTrait.AST_NODE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalLikeVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}