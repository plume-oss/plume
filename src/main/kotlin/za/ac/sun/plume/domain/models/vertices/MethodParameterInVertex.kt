package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.EvaluationStrategy
import za.ac.sun.plume.domain.enums.VertexBaseTrait
import za.ac.sun.plume.domain.enums.VertexLabel
import za.ac.sun.plume.domain.models.DeclarationVertex
import java.util.*

/**
 * This node represents a formal parameter going towards the callee side
 */
class MethodParameterInVertex(
        val code: String,
        val evaluationStrategy: EvaluationStrategy,
        val typeFullName: String,
        val lineNumber: Int,
        name: String,
        order: Int
) : DeclarationVertex(name, order) {
    companion object {
        @JvmField
        val LABEL = VertexLabel.METHOD_PARAMETER_IN

        @JvmField
        val TRAITS: EnumSet<VertexBaseTrait> = EnumSet.of(
                VertexBaseTrait.AST_NODE,
                VertexBaseTrait.DECLARATION,
                VertexBaseTrait.CFG_NODE
        )

        @JvmField
        val VALID_OUT_EDGES = emptyMap<EdgeLabel, List<VertexLabel>>()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodParameterInVertex) return false
        if (!super.equals(other)) return false

        if (code != other.code) return false
        if (evaluationStrategy != other.evaluationStrategy) return false
        if (typeFullName != other.typeFullName) return false
        if (lineNumber != other.lineNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + evaluationStrategy.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + lineNumber
        return result
    }

    override fun toString(): String {
        return "MethodParameterInVertex(code='$code', evaluationStrategy=$evaluationStrategy, typeFullName='$typeFullName', lineNumber=$lineNumber)"
    }
}