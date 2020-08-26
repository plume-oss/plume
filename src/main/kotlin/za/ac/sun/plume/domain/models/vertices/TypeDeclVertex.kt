package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.ASTVertex
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
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.TYPE_DECL
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.AST_NODE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeDeclVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (fullName != other.fullName) return false
        if (typeDeclFullName != other.typeDeclFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + typeDeclFullName.hashCode()
        return result
    }

    override fun toString(): String {
        return "TypeDeclVertex(name='$name', fullName='$fullName', typeDeclFullName='$typeDeclFullName')"
    }
}