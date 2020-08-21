package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.CFGVertex
import java.util.*

/**
 * A formal method return.
 */
class MethodReturnVertex(
        val name: String,
        code: String,
        val typeFullName: String,
        val evaluationStrategy: EvaluationStrategies,
        lineNumber: Int,
        order: Int
) :  CFGVertex(lineNumber, code, order) {
    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.METHOD_RETURN
        @kotlin.jvm.JvmField
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(
                VertexBaseTraits.CFG_NODE,
                VertexBaseTraits.TRACKING_POINT
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodReturnVertex) return false
        if (!super.equals(other)) return false

        if (name != other.name) return false
        if (typeFullName != other.typeFullName) return false
        if (evaluationStrategy != other.evaluationStrategy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + evaluationStrategy.hashCode()
        return result
    }

    override fun toString(): String {
        return "MethodReturnVertex(name='$name', typeFullName='$typeFullName', evaluationStrategy=$evaluationStrategy)"
    }
}