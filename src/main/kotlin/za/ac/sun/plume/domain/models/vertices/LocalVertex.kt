package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.LocalLikeVertex
import java.util.*

/**
 * A local variable
 */
class LocalVertex(
        val code: String,
        val typeFullName: String,
        val lineNumber: Int,
        name: String,
        order: Int
) : LocalLikeVertex(name, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabel.LOCAL
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.DECLARATION,
                VertexBaseTrait.LOCAL_LIKE,
                VertexBaseTrait.CALL_REPR
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalVertex) return false
        if (!super.equals(other)) return false

        if (code != other.code) return false
        if (typeFullName != other.typeFullName) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + lineNumber
        return result
    }

    override fun toString(): String {
        return "LocalVertex(code='$code', typeFullName='$typeFullName', lineNumber=$lineNumber)"
    }
}