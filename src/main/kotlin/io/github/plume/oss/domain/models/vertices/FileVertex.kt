package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ASTVertex
import java.util.*

/**
 * Node representing a source file. Often also the AST root.
 */
class FileVertex(val name: String, val hash : String, order: Int) : ASTVertex(order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.FILE
        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)
        @JvmField
        val VALID_OUT_EDGES = mapOf(EdgeLabel.AST to listOf(VertexLabel.NAMESPACE_BLOCK))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileVertex

        if (name != other.name) return false
        if (hash != other.hash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + hash.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileVertex(name='$name', order=$order, hash='$hash')"
    }
}