package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.PlumeVertex
import java.util.*

/**
 * Node to save meta data about the graph on its properties. Exactly one node of this type per graph
 */
class MetaDataVertex(val language: String, val version: String) : PlumeVertex {
    companion object {
        @JvmField
        val LABEL = VertexLabel.META_DATA

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.noneOf(VertexBaseTrait::class.java)

        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun toString(): String {
        return "MetaDataVertex(language='$language', version='$version')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetaDataVertex

        if (language != other.language) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        result = 31 * result + language.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}