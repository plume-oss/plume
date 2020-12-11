package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.DeclarationVertex
import java.util.*

/**
 * Member of a class struct or union.
 */
class MemberVertex(val code: String, name: String, val typeFullName: String, order: Int) : DeclarationVertex(name, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.MEMBER

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.DECLARATION,
                VertexBaseTrait.AST_NODE
        )

        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemberVertex) return false

        if (name != other.name) return false
        if (code != other.code) return false
        if (typeFullName != other.typeFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + typeFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "MemberVertex(code='$code', typeFullName='$typeFullName')"
    }
}