package za.ac.sun.plume

import za.ac.sun.plume.domain.enums.DispatchTypes
import za.ac.sun.plume.domain.enums.EvaluationStrategies
import za.ac.sun.plume.domain.enums.ModifierTypes
import za.ac.sun.plume.domain.models.vertices.*

class TestDomainResources {
    companion object {
        const val STRING_1 = "TEST1"
        const val STRING_2 = "TEST2"
        const val INT_1 = 0
        const val INT_2 = 1
        const val INT_3 = 2
        const val INT_4 = 3
        val DISPATCH_1 = DispatchTypes.DYNAMIC_DISPATCH
        val DISPATCH_2 = DispatchTypes.STATIC_DISPATCH
        val EVAL_1 = EvaluationStrategies.BY_REFERENCE
        val EVAL_2 = EvaluationStrategies.BY_SHARING
        val MOD_1 = ModifierTypes.ABSTRACT
        val MOD_2 = ModifierTypes.CONSTRUCTOR
        const val ROOT_METHOD = "root"
        const val FIRST_BLOCK = "firstBlock"
        const val TEST_ID = "test"
        const val ROOT_PACKAGE = "za"
        const val SECOND_PACKAGE = "ac"
        const val THIRD_PACKAGE = "sun"

        val vertices = listOf(
                ArrayInitializerVertex(INT_1),
                BindingVertex(STRING_1, STRING_2),
                BlockVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1),
                CallVertex(STRING_1, STRING_1, INT_1, STRING_1, STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, INT_1),
                ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1),
                FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1),
                FileVertex(STRING_1, INT_1),
                IdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1),
                LiteralVertex(STRING_1, INT_1, INT_1, STRING_1, INT_1),
                LocalVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1),
                MemberVertex(STRING_1, STRING_1, STRING_1, INT_1),
                MetaDataVertex(STRING_1, STRING_1),
                MethodParameterInVertex(STRING_1, STRING_1, EVAL_1, STRING_1, INT_1, INT_1),
                MethodRefVertex(STRING_1, INT_1, INT_1, STRING_1, STRING_1, INT_1),
                MethodReturnVertex(STRING_1, STRING_1, EVAL_1, INT_1, INT_1),
                MethodVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1),
                ModifierVertex(MOD_1, INT_1),
                NamespaceBlockVertex(STRING_1, STRING_1, INT_1),
                ReturnVertex(INT_1, INT_1, INT_1, STRING_1),
                TypeArgumentVertex(INT_1),
                TypeDeclVertex(STRING_1, STRING_1, STRING_1),
                TypeParameterVertex(STRING_1, INT_1),
                TypeVertex(STRING_1, STRING_1, STRING_1),
                UnknownVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1)
        )
    }
}