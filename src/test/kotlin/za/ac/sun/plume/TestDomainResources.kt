package za.ac.sun.plume

import za.ac.sun.plume.domain.enums.DispatchType
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.enums.EvaluationStrategy
import za.ac.sun.plume.domain.enums.ModifierType
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.drivers.TinkerGraphDriverTest

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
                BlockVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1),
                ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                FileVertex(STRING_1, STRING_2, INT_1),
                IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1),
                LiteralVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                LocalVertex(STRING_1, STRING_1, INT_1, INT_1, STRING_1, INT_1),
                MemberVertex(STRING_1, STRING_1, STRING_1, INT_1),
                MetaDataVertex(STRING_1, STRING_1),
                MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_1, INT_1),
                MethodRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1),
                MethodVertex(STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1),
                ModifierVertex(MOD_1, INT_1),
                NamespaceBlockVertex(STRING_1, STRING_1, INT_1),
                ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1),
                TypeArgumentVertex(INT_1),
                TypeDeclVertex(STRING_1, STRING_1, STRING_1, INT_1),
                TypeParameterVertex(STRING_1, INT_1),
                TypeRefVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                TypeVertex(STRING_1, STRING_1, STRING_1),
                UnknownVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        )

        val v1 = MethodVertex(STRING_1, STRING_1, STRING_2, STRING_1, INT_1, INT_2, INT_1)
        val v2 = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_2, INT_2)
        val v3 = BlockVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_2, INT_2, INT_1)
        val v4 = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_2, STRING_2, STRING_2, INT_1, INT_1, INT_1)
        val v5 = LocalVertex(STRING_1, STRING_2, INT_1, INT_1, STRING_1, INT_1)
        val v6 = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        val v7 = TypeDeclVertex(STRING_1, STRING_2, STRING_1, INT_1)
        val v8 = LiteralVertex(STRING_1, STRING_2, STRING_2, INT_1, INT_1, INT_1, INT_1)
        val v9 = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
        val v10 = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
        val v11 = FileVertex(STRING_1, STRING_2, INT_1)
        val v12 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
        val v13 = NamespaceBlockVertex(STRING_2, STRING_2, INT_1)
        val v14 = MetaDataVertex(STRING_1, STRING_2)
        val v15 = ControlStructureVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        val v16 = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1)
        val v17 = BindingVertex(STRING_1, STRING_2)
        val v18 = TypeArgumentVertex(INT_1)
        val v19 = TypeParameterVertex(STRING_1, INT_1)

        fun generateSimpleCPG(driver: IDriver) {
            // Create program data
            driver.addVertex(v14)
            driver.addEdge(v11, v12, EdgeLabel.AST)
            driver.addEdge(v12, v13, EdgeLabel.AST)
            // Create method head
            driver.addEdge(v7, v1, EdgeLabel.AST)
            driver.addEdge(v1, v11, EdgeLabel.SOURCE_FILE)
            driver.addEdge(v1, v2, EdgeLabel.AST)
            driver.addEdge(v1, v5, EdgeLabel.AST)
            driver.addEdge(v1, v3, EdgeLabel.AST)
            driver.addEdge(v1, v3, EdgeLabel.CFG)
            // Create method body
            driver.addEdge(v3, v4, EdgeLabel.AST)
            driver.addEdge(v3, v4, EdgeLabel.CFG)
            driver.addEdge(v4, v6, EdgeLabel.AST)
            driver.addEdge(v4, v8, EdgeLabel.AST)
            driver.addEdge(v4, v6, EdgeLabel.ARGUMENT)
            driver.addEdge(v4, v8, EdgeLabel.ARGUMENT)
            driver.addEdge(v3, v9, EdgeLabel.AST)
            driver.addEdge(v4, v9, EdgeLabel.CFG)
            driver.addEdge(v1, v10, EdgeLabel.AST)
            driver.addEdge(v9, v10, EdgeLabel.CFG)
            // Link dependencies
            driver.addEdge(v6, v5, EdgeLabel.REF)
        }
        
    }
}