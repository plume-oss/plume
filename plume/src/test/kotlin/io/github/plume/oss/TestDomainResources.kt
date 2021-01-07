package io.github.plume.oss

import io.github.plume.oss.domain.enums.DispatchType
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.EvaluationStrategy
import io.github.plume.oss.domain.enums.ModifierType
import io.github.plume.oss.domain.models.vertices.*
import io.github.plume.oss.drivers.IDriver

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
                BlockVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1),
                ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1, INT_1),
                FieldIdentifierVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                FileVertex(STRING_1, STRING_2, INT_1),
                IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
                JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1),
                LiteralVertex(STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1),
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

        val methodVertex = MethodVertex(STRING_1, STRING_1, STRING_2, STRING_1, INT_1, INT_2, INT_1)
        val methodParameterInVertex = MethodParameterInVertex(STRING_1, EVAL_1, STRING_1, INT_1, STRING_2, INT_2)
        val blockVertex = BlockVertex(STRING_1, STRING_1, INT_1, INT_2, INT_2, INT_1)
        val callVertex = CallVertex(STRING_1, INT_1, DISPATCH_1, STRING_1, STRING_1, STRING_2, STRING_2, STRING_2, INT_1, INT_1, INT_1)
        val localVertex = LocalVertex(STRING_1, STRING_2, INT_1, INT_1, STRING_1, INT_1)
        val identifierVertex = IdentifierVertex(STRING_1, STRING_1, STRING_1, INT_1, INT_1, INT_1, INT_1)
        val typeDeclVertex = TypeDeclVertex(STRING_1, STRING_2, STRING_1, INT_1)
        val literalVertex = LiteralVertex(STRING_2, STRING_2, INT_1, INT_1, INT_1, INT_1)
        val returnVertex = ReturnVertex(INT_1, INT_1, INT_1, INT_1, STRING_1)
        val methodReturnVertex = MethodReturnVertex(STRING_1, EVAL_1, STRING_1, INT_1, INT_1, INT_1)
        val fileVertex = FileVertex(STRING_1, STRING_2, INT_1)
        val namespaceBlockVertex1 = NamespaceBlockVertex(STRING_1, STRING_1, INT_1)
        val namespaceBlockVertex2 = NamespaceBlockVertex(STRING_2, STRING_2, INT_1)
        val metaDataVertex = MetaDataVertex(STRING_1, STRING_2)
        val controlStructureVertex = ControlStructureVertex(STRING_1, INT_1, INT_1, INT_1, INT_1)
        val jumpTargetVertex = JumpTargetVertex(STRING_1, INT_1, INT_1, INT_1, STRING_1, INT_1)
        val bindingVertex = BindingVertex(STRING_1, STRING_2)
        val typeArgumentVertex = TypeArgumentVertex(INT_1)
        val typeParameterVertex = TypeParameterVertex(STRING_1, INT_1)
        val fieldIdentifierVertex = FieldIdentifierVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
        val methodRefVertex = MethodRefVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
        val typeRefVertex = TypeRefVertex(STRING_1, STRING_2, STRING_1, INT_1, INT_1, INT_1, INT_1)
        val unknownVertex = UnknownVertex(STRING_1, STRING_2, INT_1, INT_1, INT_1, INT_1)
        val modifierVertex = ModifierVertex(MOD_1, INT_2)

        fun generateSimpleCPG(driver: IDriver) {
            // Create program data
            driver.addVertex(metaDataVertex)
            driver.addEdge(fileVertex, namespaceBlockVertex1, EdgeLabel.AST)
            driver.addEdge(namespaceBlockVertex1, namespaceBlockVertex2, EdgeLabel.AST)
            // Create method head
            driver.addEdge(typeDeclVertex, methodVertex, EdgeLabel.AST)
            driver.addEdge(methodVertex, fileVertex, EdgeLabel.SOURCE_FILE)
            driver.addEdge(methodVertex, methodParameterInVertex, EdgeLabel.AST)
            driver.addEdge(methodVertex, localVertex, EdgeLabel.AST)
            driver.addEdge(methodVertex, blockVertex, EdgeLabel.AST)
            driver.addEdge(methodVertex, blockVertex, EdgeLabel.CFG)
            driver.addEdge(methodVertex, modifierVertex, EdgeLabel.AST)
            // Create method body
            driver.addEdge(blockVertex, callVertex, EdgeLabel.AST)
            driver.addEdge(callVertex, identifierVertex, EdgeLabel.AST)
            driver.addEdge(callVertex, literalVertex, EdgeLabel.AST)
            driver.addEdge(callVertex, identifierVertex, EdgeLabel.ARGUMENT)
            driver.addEdge(callVertex, literalVertex, EdgeLabel.ARGUMENT)
            driver.addEdge(blockVertex, returnVertex, EdgeLabel.AST)
            driver.addEdge(callVertex, fieldIdentifierVertex, EdgeLabel.AST)
            driver.addEdge(methodVertex, methodReturnVertex, EdgeLabel.AST)
            driver.addEdge(blockVertex, methodRefVertex, EdgeLabel.AST)
            driver.addEdge(blockVertex, typeRefVertex, EdgeLabel.AST)
            driver.addEdge(blockVertex, controlStructureVertex, EdgeLabel.AST)
            driver.addEdge(blockVertex, jumpTargetVertex, EdgeLabel.AST)

            driver.addEdge(blockVertex, callVertex, EdgeLabel.CFG)
            driver.addEdge(callVertex, fieldIdentifierVertex, EdgeLabel.CFG)
            driver.addEdge(fieldIdentifierVertex, methodRefVertex, EdgeLabel.CFG)
            driver.addEdge(methodRefVertex, typeRefVertex, EdgeLabel.CFG)
            driver.addEdge(typeRefVertex, controlStructureVertex, EdgeLabel.CFG)
            driver.addEdge(controlStructureVertex, jumpTargetVertex, EdgeLabel.CFG)
            driver.addEdge(jumpTargetVertex, returnVertex, EdgeLabel.CFG)
            driver.addEdge(returnVertex, methodReturnVertex, EdgeLabel.CFG)

            // Just add some vertices to test conversion
            driver.addVertex(unknownVertex)

            // Link dependencies
            driver.addEdge(identifierVertex, localVertex, EdgeLabel.REF)
        }
        
    }
}