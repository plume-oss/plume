package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * Node representing a source file. Often also the AST root.
 */
class FileVertex(val name: String, order: Int) : ASTVertex(order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.FILE
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileVertex

        if (name != other.name) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + order
        return result
    }

    override fun toString(): String {
        return "FileVertex{" +
                "name='" + name + '\'' +
                ", order=" + order +
                '}'
    }
}