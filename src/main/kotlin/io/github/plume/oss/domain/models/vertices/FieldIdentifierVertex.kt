package io.github.plume.oss.domain.models.vertices

import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.VertexBaseTrait
import io.github.plume.oss.domain.enums.VertexLabel
import io.github.plume.oss.domain.models.ExpressionVertex
import java.util.*

/**
 * A node that represents which field is accessed in a <operator>.fieldAccess, in e.g. obj.field. The CODE part is used
 * for human display and matching to MEM?BER nodes. The CANONICAL_NAME is used for dataflow tracking; typically both
 * coincide. However, suppose that two fields foo and bar are a C-style union; then CODE refers to whatever the
 * programmer wrote (obj.foo or obj.bar), but both share the same CANONICAL_NAME (e.g. GENERATED_foo_bar)</operator>
 */
class FieldIdentifierVertex(
        val canonicalName: String,
        code: String,
        argumentIndex: Int,
        lineNumber: Int,
        columnNumber: Int,
        order: Int
) : ExpressionVertex(code, argumentIndex, lineNumber, columnNumber, order) {

    companion object {
        @JvmField
        val LABEL = VertexLabel.FIELD_IDENTIFIER

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(VertexBaseTrait.EXPRESSION)

        @JvmField
        val VALID_OUT_EDGES = mapOf(
                EdgeLabel.CFG to listOf(
                        VertexLabel.CALL,
                        VertexLabel.IDENTIFIER,
                        VertexLabel.FIELD_IDENTIFIER,
                        VertexLabel.LITERAL,
                        VertexLabel.METHOD_REF,
                        VertexLabel.TYPE_REF,
                        VertexLabel.RETURN,
                        VertexLabel.BLOCK,
                        VertexLabel.JUMP_TARGET,
                        VertexLabel.CONTROL_STRUCTURE,
                        VertexLabel.UNKNOWN
                )
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldIdentifierVertex) return false
        if (!super.equals(other)) return false

        if (canonicalName != other.canonicalName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + canonicalName.hashCode()
        return result
    }

    override fun toString(): String {
        return "FieldIdentifierVertex(canonicalName='$canonicalName', order=$order)"
    }

}