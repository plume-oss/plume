package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.DeclarationVertex
import java.util.*

/**
 * This node represents a formal parameter going towards the callee side
 */
class MethodParameterInVertex(
        val code: String,
        val evaluationStrategy: EvaluationStrategies,
        val typeFullName: String,
        val lineNumber: Int,
        name: String,
        order: Int
) : DeclarationVertex(name, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.METHOD_PARAMETER_IN
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.AST_NODE,
                VertexBaseTraits.DECLARATION,
                VertexBaseTraits.CFG_NODE)
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