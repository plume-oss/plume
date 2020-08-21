package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.PlumeVertex
import java.util.*

/**
 * A type which always has to reference a type declaration and may have type argument children if the referred to type
 * declaration is a template
 */
class TypeVertex(val name: String, val fullName: String, val typeDeclFullName: String) : PlumeVertex {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.TYPE
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.noneOf(VertexBaseTraits::class.java)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeVertex) return false

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

    override fun toString(): String {
        return "TypeVertex(name='$name', fullName='$fullName', typeDeclFullName='$typeDeclFullName')"
    }
}