package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.ASTVertex
import java.util.*

/**
 * A node that represents which field is accessed in a <operator>.fieldAccess, in e.g. obj.field. The CODE part is used
 * for human display and matching to MEM?BER nodes. The CANONICAL_NAME is used for dataflow tracking; typically both
 * coincide. However, suppose that two fields foo and bar are a C-style union; then CODE refers to whatever the
 * programmer wrote (obj.foo or obj.bar), but both share the same CANONICAL_NAME (e.g. GENERATED_foo_bar)
</operator> */
class FieldIdentifierVertex(
        val code: String,
        val canonicalName: String,
        order: Int,
        val argumentIndex: Int,
        val lineNumber: Int
) : ASTVertex(order) {
    override fun toString(): String {
        return "FieldIdentifierVertex{" +
                "code='" + code + '\'' +
                ", canonicalName='" + canonicalName + '\'' +
                ", order=" + order +
                ", argumentIndex=" + argumentIndex +
                ", lineNumber=" + lineNumber +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FieldIdentifierVertex

        if (code != other.code) return false
        if (canonicalName != other.canonicalName) return false
        if (order != other.order) return false
        if (argumentIndex != other.argumentIndex) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + canonicalName.hashCode()
        result = 31 * result + order
        result = 31 * result + argumentIndex
        result = 31 * result + lineNumber
        return result
    }


    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.FIELD_IDENTIFIER
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.EXPRESSION)
    }

}