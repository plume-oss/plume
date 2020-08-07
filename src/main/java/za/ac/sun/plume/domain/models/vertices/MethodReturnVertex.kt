package za.ac.sun.plume.domain.models.vertices

import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.VertexBaseTraits
import za.ac.sun.plume.domain.enums.VertexLabels
import za.ac.sun.plume.domain.models.MethodDescriptorVertex
import java.util.*

/**
 * A formal method return
 */
class MethodReturnVertex(
        name: String,
        typeFullName: String,
        val evaluationStrategy: EvaluationStrategies,
        val lineNumber: Int,
        order: Int
) :  MethodDescriptorVertex(name, typeFullName, order) {
    override fun toString(): String {
        return "MethodReturnVertex{" +
                "name='" + name + '\'' +
                ", evaluationStrategy=" + evaluationStrategy +
                ", typeFullName='" + typeFullName + '\'' +
                ", lineNumber=" + lineNumber +
                ", order=" + order +
                '}'
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodReturnVertex

        if (name != other.name) return false
        if (typeFullName != other.typeFullName) return false
        if (evaluationStrategy != other.evaluationStrategy) return false
        if (lineNumber != other.lineNumber) return false
        if (order != other.order) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + typeFullName.hashCode()
        result = 31 * result + evaluationStrategy.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + order
        return result
    }

    companion object {
        @kotlin.jvm.JvmField
        val LABEL = VertexLabels.METHOD_RETURN
        val TRAITS: EnumSet<VertexBaseTraits> = EnumSet.of(VertexBaseTraits.CFG_NODE,
                VertexBaseTraits.TRACKING_POINT)
    }

}