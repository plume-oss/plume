package io.github.plume.oss

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.DispatchTypes.DYNAMIC_DISPATCH
import io.shiftleft.codepropertygraph.generated.DispatchTypes.STATIC_DISPATCH
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.BY_REFERENCE
import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.BY_SHARING
import io.shiftleft.codepropertygraph.generated.ModifierTypes.ABSTRACT
import io.shiftleft.codepropertygraph.generated.ModifierTypes.CONSTRUCTOR
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.Option

class TestDomainResources {
    companion object {
        const val STRING_1 = "TEST1"
        const val STRING_2 = "TEST2"
        const val INT_1 = 0
        const val INT_2 = 1
        val DISPATCH_1 = DYNAMIC_DISPATCH
        val DISPATCH_2 = STATIC_DISPATCH
        val EVAL_1 = BY_REFERENCE
        val EVAL_2 = BY_SHARING
        val MOD_1 = ABSTRACT
        val MOD_2 = CONSTRUCTOR
        val BOOL_1 = false

        val vertices = listOf<NewNodeBuilder>(
            NewBindingBuilder().name(STRING_1).signature(STRING_2),
            NewBlockBuilder().typeFullName(STRING_1).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewCallBuilder().methodFullName(STRING_1).argumentIndex(INT_1).dispatchType(DISPATCH_1)
                .name(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1)),
            NewControlStructureBuilder().controlStructureType(STRING_2).code(STRING_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1)).order(INT_1).argumentIndex(INT_1),
            NewFieldIdentifierBuilder().canonicalName(STRING_1).code(STRING_1).argumentIndex(INT_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewFileBuilder().name(STRING_1).hash(Option.apply(STRING_2)).order(INT_1),
            NewIdentifierBuilder().name(STRING_1).typeFullName(STRING_1).code(STRING_1).order(INT_1)
                .argumentIndex(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewJumpTargetBuilder().name(STRING_1).argumentIndex(INT_1).code(STRING_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewLiteralBuilder().typeFullName(STRING_1).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewLocalBuilder().code(STRING_1).typeFullName(STRING_1).name(STRING_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewMemberBuilder().code(STRING_1).name(STRING_1).typeFullName(STRING_1).order(INT_1),
            NewMetaDataBuilder().language(STRING_1).version(STRING_1).hash(Option.apply(STRING_2)),
            NewMethodParameterInBuilder().code(STRING_1).evaluationStrategy(EVAL_1).typeFullName(STRING_1)
                .name(STRING_1).order(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewMethodRefBuilder().methodFullName(STRING_1).code(STRING_1)
                .order(INT_1).argumentIndex(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewMethodReturnBuilder().typeFullName(STRING_1).evaluationStrategy(EVAL_1).code(STRING_1)
                .order(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)),
            NewMethodBuilder().name(STRING_1).fullName(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)).isExternal(BOOL_1)
                .hash(Option.apply("123")).filename(STRING_1),
            NewModifierBuilder().modifierType(MOD_1).order(INT_1),
            NewNamespaceBlockBuilder().name(STRING_1).fullName(STRING_1).order(INT_1).filename(STRING_1),
            NewReturnBuilder().order(INT_1).argumentIndex(INT_1).code(STRING_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1)),
            NewTypeArgumentBuilder().order(INT_1),
            NewTypeDeclBuilder().name(STRING_1).fullName(STRING_2).order(INT_1).isExternal(BOOL_1).filename(STRING_1),
            NewTypeParameterBuilder().name(STRING_1).order(INT_1),
            NewTypeRefBuilder().typeFullName(STRING_1)
                .code(STRING_1).argumentIndex(INT_1).order(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1)),
            NewTypeBuilder().name(STRING_1).fullName(STRING_1).typeDeclFullName(STRING_1),
            NewUnknownBuilder().typeFullName(STRING_1).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        )

        val methodVertex: NewMethodBuilder =
            NewMethodBuilder().code(STRING_1).name(STRING_1).fullName(STRING_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1)).signature(STRING_2)
                .filename(STRING_1).hash(Option.apply("456"))
                .astParentFullName(STRING_1).astParentType(STRING_2).isExternal(BOOL_1)
        val mtdParamInVertex: NewMethodParameterInBuilder =
            NewMethodParameterInBuilder().code(STRING_1).evaluationStrategy(EVAL_1).typeFullName(STRING_1)
                .name(STRING_1).order(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val blockVertex: NewBlockBuilder =
            NewBlockBuilder().typeFullName(STRING_1).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val callVertex: NewCallBuilder =
            NewCallBuilder().methodFullName(STRING_1).argumentIndex(INT_1).dispatchType(DISPATCH_1)
                .name(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1))
        val localVertex: NewLocalBuilder =
            NewLocalBuilder().code(STRING_1).typeFullName(STRING_1).name(STRING_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val identifierVertex: NewIdentifierBuilder =
            NewIdentifierBuilder().name(STRING_1).typeFullName(STRING_1).code(STRING_1).order(INT_1)
                .argumentIndex(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val typeDeclVertex: NewTypeDeclBuilder = NewTypeDeclBuilder().name(STRING_1).fullName(STRING_1).order(INT_1)
            .astParentFullName(STRING_1).astParentType(STRING_2).filename(STRING_1)
        val literalVertex: NewLiteralBuilder =
            NewLiteralBuilder().typeFullName(STRING_1).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val returnVertex: NewReturnBuilder =
            NewReturnBuilder().order(INT_1).argumentIndex(INT_1).code(STRING_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1))
        val mtdRtnVertex: NewMethodReturnBuilder =
            NewMethodReturnBuilder().typeFullName(STRING_1).evaluationStrategy(EVAL_1).code(STRING_1)
                .order(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val fileVertex: NewFileBuilder = NewFileBuilder().name(STRING_1).hash(Option.apply(STRING_2)).order(INT_1)
        val namespaceBlockVertex1: NewNamespaceBlockBuilder =
            NewNamespaceBlockBuilder().name(STRING_1).fullName(STRING_1).order(INT_1).filename(STRING_1)
        val namespaceBlockVertex2: NewNamespaceBlockBuilder =
            NewNamespaceBlockBuilder().name(STRING_2).fullName(STRING_2).order(INT_1).filename(STRING_2)
        val metaDataVertex: NewMetaDataBuilder = NewMetaDataBuilder().language(STRING_1).version(STRING_2)
            .hash(Option.apply(STRING_2))
        val controlStructureVertex: NewControlStructureBuilder =
            NewControlStructureBuilder().controlStructureType(STRING_2).code(STRING_1).lineNumber(Option.apply(INT_1))
                .columnNumber(Option.apply(INT_1)).order(INT_1).argumentIndex(INT_1)
        val jumpTargetVertex: NewJumpTargetBuilder =
            NewJumpTargetBuilder().name(STRING_1).argumentIndex(INT_1).code(STRING_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val bindingVertex: NewBindingBuilder = NewBindingBuilder().name(STRING_1).signature(STRING_2)
        val typeArgumentVertex: NewTypeArgumentBuilder = NewTypeArgumentBuilder().order(INT_1)
        val typeParameterVertex: NewTypeParameterBuilder = NewTypeParameterBuilder().name(STRING_1).order(INT_1)
        val fldIdentVertex: NewFieldIdentifierBuilder =
            NewFieldIdentifierBuilder().canonicalName(STRING_1).code(STRING_1).argumentIndex(INT_1).order(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val methodRefVertex: NewMethodRefBuilder =
            NewMethodRefBuilder().methodFullName(STRING_1).code(STRING_1)
                .order(INT_1).argumentIndex(INT_1).lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val typeRefVertex: NewTypeRefBuilder = NewTypeRefBuilder().typeFullName(STRING_1)
            .code(STRING_1).argumentIndex(INT_1).order(INT_1).lineNumber(Option.apply(INT_1))
            .columnNumber(Option.apply(INT_1))
        val unknownVertex: NewUnknownBuilder =
            NewUnknownBuilder().typeFullName(STRING_1).code(STRING_1).order(INT_1).argumentIndex(INT_1)
                .lineNumber(Option.apply(INT_1)).columnNumber(Option.apply(INT_1))
        val modifierVertex: NewModifierBuilder = NewModifierBuilder().modifierType(MOD_1).order(INT_1)

        val simpleCpgVertices = listOf(
            methodVertex,
            mtdParamInVertex,
            blockVertex,
            callVertex,
            localVertex,
            identifierVertex,
            typeDeclVertex,
            literalVertex,
            returnVertex,
            mtdRtnVertex,
            fileVertex,
            namespaceBlockVertex1,
            namespaceBlockVertex2,
            metaDataVertex,
            controlStructureVertex,
            jumpTargetVertex,
            bindingVertex,
            typeArgumentVertex,
            typeParameterVertex,
            fldIdentVertex,
            methodRefVertex,
            typeRefVertex,
            unknownVertex,
            modifierVertex
        )

        fun generateSimpleCPG(driver: IDriver) {
            // Create program data
            driver.addVertex(metaDataVertex)
            driver.addEdge(fileVertex, namespaceBlockVertex1, AST)
            driver.addEdge(namespaceBlockVertex1, namespaceBlockVertex2, AST)
            // Create method head
            driver.addEdge(typeDeclVertex, methodVertex, AST)
            driver.addEdge(methodVertex, fileVertex, SOURCE_FILE)
            driver.addEdge(methodVertex, mtdParamInVertex, AST)
            driver.addEdge(methodVertex, localVertex, AST)
            driver.addEdge(methodVertex, blockVertex, AST)
            driver.addEdge(methodVertex, blockVertex, CFG)
            driver.addEdge(methodVertex, modifierVertex, AST)
            // Create method body
            driver.addEdge(blockVertex, callVertex, AST)
            driver.addEdge(callVertex, identifierVertex, AST)
            driver.addEdge(callVertex, literalVertex, AST)
            driver.addEdge(callVertex, identifierVertex, ARGUMENT)
            driver.addEdge(callVertex, literalVertex, ARGUMENT)
            driver.addEdge(blockVertex, returnVertex, AST)
            driver.addEdge(callVertex, fldIdentVertex, AST)
            driver.addEdge(methodVertex, mtdRtnVertex, AST)
            driver.addEdge(blockVertex, methodRefVertex, AST)
            driver.addEdge(blockVertex, typeRefVertex, AST)
            driver.addEdge(blockVertex, controlStructureVertex, AST)
            driver.addEdge(blockVertex, jumpTargetVertex, AST)

            driver.addEdge(blockVertex, callVertex, CFG)
            driver.addEdge(callVertex, fldIdentVertex, CFG)
            driver.addEdge(fldIdentVertex, methodRefVertex, CFG)
            driver.addEdge(methodRefVertex, typeRefVertex, CFG)
            driver.addEdge(typeRefVertex, controlStructureVertex, CFG)
            driver.addEdge(controlStructureVertex, jumpTargetVertex, CFG)
            driver.addEdge(jumpTargetVertex, returnVertex, CFG)
            driver.addEdge(returnVertex, mtdRtnVertex, CFG)

            // Just add some vertices to test conversion
            driver.addVertex(unknownVertex)

            // Link dependencies
            driver.addEdge(identifierVertex, localVertex, REF)
        }

    }
}