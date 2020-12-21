package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ASTVertex
import java.util.*

/**
 * A type declaration
 */
class TypeDeclVertex(
        val name: String,
        val fullName: String,
        val typeDeclFullName: String,
        order: Int
) : ASTVertex(order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.TYPE_DECL

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.AST to listOf(
                        VertexLabel.TYPE_PARAMETER,
                        VertexLabel.MEMBER,
                        VertexLabel.MODIFIER,
                        VertexLabel.METHOD
                ),
                EdgeLabel.BINDS to listOf(
                        VertexLabel.BINDING
                ),
                EdgeLabel.SOURCE_FILE to listOf(
                        VertexLabel.FILE
                )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeDeclVertex) return false

        if (name != other.name) return false
        if (fullName != other.fullName) return false
        if (typeDeclFullName != other.typeDeclFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = javaClass.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + typeDeclFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "TypeDeclVertex(name='$name', fullName='$fullName', typeDeclFullName='$typeDeclFullName')"
    }
}