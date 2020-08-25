package za.ac.sun.plume

import za.ac.sun.plume.domain.enums.DispatchType
import za.ac.sun.plume.domain.enums.EvaluationStrategy
import za.ac.sun.plume.domain.enums.ModifierType
import za.ac.sun.plume.domain.models.vertices.*

class TestDomainResources {
    companion object {
        const val STRING_1 = "TEST1"
        const val STRING_2 = "TEST2"
        const val INT_1 = 0
        const val INT_2 = 1
        val DISPATCH_1 = DispatchType.DYNAMIC_DISPATCH
        val DISPATCH_2 = DispatchType.STATIC_DISPATCH
        val EVAL_1 = EvaluationStrategy.BY_REFERENCE
        val EVAL_2 = EvaluationStrategy.BY_SHARING
        val MOD_1 = ModifierType.ABSTRACT
        val MOD_2 = ModifierType.CONSTRUCTOR

        val vertices = listOf(
                ArrayInitializerVertex(INT_1),
                BindingVertex(STRING_1, STRING_2),
                BlockVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1),
                CallVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1),
                ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1),
                FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1),
                FileVertex(STRING_1, INT_1),
                IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1),
                JumpTargetVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1),
                LiteralVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1),
                LocalVertex(STRING_1, STRING_1, INT_1, STRING_1, INT_1),
                MemberVertex(STRING_1, STRING_1, STRING_1, INT_1),
                MetaDataVertex(STRING_1, STRING_1),
                MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_1, INT_1),
                MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1),
                MethodReturnVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_1),
                MethodVertex(STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1),
                ModifierVertex(MOD_1, INT_1),
                NamespaceBlockVertex(STRING_1, STRING_1, INT_1),
                ReturnVertex(INT_1, INT_1, INT_1, STRING_1),
                TypeArgumentVertex(INT_1),
                TypeDeclVertex(STRING_1, STRING_1, STRING_1, INT_1),
                TypeParameterVertex(STRING_1, INT_1),
                TypeVertex(STRING_1, STRING_1, STRING_1),
                UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1)
        )
    }
}