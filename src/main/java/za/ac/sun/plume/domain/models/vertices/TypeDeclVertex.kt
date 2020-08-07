package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.PlumeVertex
import java.util.*

/**
 * A type declaration
 */
class TypeDeclVertex(val name: String, val fullName: String, val typeDeclFullName: String) : PlumeVertex {
    override fun toString(): String {
        return "TypeDeclVertex{" +
                "name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", typeDeclFullName='" + typeDeclFullName + '\'' +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeDeclVertex

        if (name != other.name) return false
        if (fullName != other.fullName) return false
        if (typeDeclFullName != other.typeDeclFullName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + fullName.hashCode()
        result = 31 * result + typeDeclFullName.hashCode()
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.TYPE_DECL
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.AST_NODE)
    }

}