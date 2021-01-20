package io.github.plume.oss

import io.github.plume.oss.domain.enums.DispatchType
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.enums.EvaluationStrategy
import io.github.plume.oss.domain.enums.ModifierType
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.SootToPlumeUtil.createSingleItemScalaList
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.Option

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

        val vertices = listOf<NewNodeBuilder>(
            NewArrayInitializerBuilder().order(INT_1),
            NewBindingBuilder().name(STRING_1).signature(STRING_2),
            NewBlockBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewCallBuilder().methodfullname(STRING_1).argumentindex(INT_1).dispatchtype(DISPATCH_1.name)
                .typefullname(STRING_1)
                .dynamictypehintfullname(createSingleItemScalaList(STRING_1))
                .name(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)),
            NewControlStructureBuilder().code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).order(INT_1).argumentindex(INT_1),
            NewFieldIdentifierBuilder().canonicalname(STRING_1).code(STRING_1).argumentindex(INT_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewFileBuilder().name(STRING_1).hash(Option.apply(STRING_2)).order(INT_1),
            NewIdentifierBuilder().name(STRING_1).typefullname(STRING_1).code(STRING_1).order(INT_1)
                .argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewJumpTargetBuilder().name(STRING_1).argumentindex(INT_1).code(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewLiteralBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewLocalBuilder().code(STRING_1).typefullname(STRING_1).name(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewMemberBuilder().code(STRING_1).name(STRING_1).typefullname(STRING_1).order(INT_1),
            NewMetaDataBuilder().language(STRING_1).version(STRING_1),
            NewMethodParameterInBuilder().code(STRING_1).evaluationstrategy(EVAL_1.name).typefullname(STRING_1)
                .name(STRING_1).order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                ,
            NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_1)).methodfullname(STRING_1).code(STRING_1)
                .order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                ,
            NewMethodReturnBuilder().typefullname(STRING_1).evaluationstrategy(EVAL_1.name).code(STRING_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewMethodBuilder().name(STRING_1).fullname(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)),
            NewModifierBuilder().modifiertype(MOD_1.name).order(INT_1),
            NewNamespaceBlockBuilder().name(STRING_1).fullname(STRING_1).order(INT_1),
            NewReturnBuilder().order(INT_1).argumentindex(INT_1).code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)),
            NewTypeArgumentBuilder().order(INT_1),
            NewTypeDeclBuilder().name(STRING_1).fullname(STRING_1).order(INT_1),
            NewTypeParameterBuilder().name(STRING_1).order(INT_1),
            NewTypeRefBuilder().typefullname(STRING_1)
                .dynamictypehintfullname(createSingleItemScalaList(STRING_1))
                .code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)),
            NewTypeBuilder().name(STRING_1).fullname(STRING_1).typedeclfullname(STRING_1),
            NewUnknownBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        )

        val methodVertex: NewMethodBuilder =
            NewMethodBuilder().code(STRING_1).name(STRING_1).fullname(STRING_1).order(INT_1)
        val methodParameterInVertex: NewMethodParameterInBuilder =
            NewMethodParameterInBuilder().code(STRING_1).evaluationstrategy(EVAL_1.name).typefullname(STRING_1)
                .name(STRING_1).order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                
        val blockVertex: NewBlockBuilder =
            NewBlockBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val callVertex: NewCallBuilder =
            NewCallBuilder().methodfullname(STRING_1).argumentindex(INT_1).dispatchtype(DISPATCH_1.name)
                .typefullname(STRING_1)
                .dynamictypehintfullname(createSingleItemScalaList(STRING_1))
                .name(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1))
        val localVertex: NewLocalBuilder = NewLocalBuilder().code(STRING_1).typefullname(STRING_1).name(STRING_1).order(INT_1)
            .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val identifierVertex: NewIdentifierBuilder =
            NewIdentifierBuilder().name(STRING_1).typefullname(STRING_1).code(STRING_1).order(INT_1)
                .argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val typeDeclVertex: NewTypeDeclBuilder = NewTypeDeclBuilder().name(STRING_1).fullname(STRING_1).order(INT_1)
        val literalVertex: NewLiteralBuilder =
            NewLiteralBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val returnVertex: NewReturnBuilder =
            NewReturnBuilder().order(INT_1).argumentindex(INT_1).code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1))
        val methodReturnVertex: NewMethodReturnBuilder =
            NewMethodReturnBuilder().typefullname(STRING_1).evaluationstrategy(EVAL_1.name).code(STRING_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val fileVertex: NewFileBuilder = NewFileBuilder().name(STRING_1).hash(Option.apply(STRING_2)).order(INT_1)
        val namespaceBlockVertex1: NewNamespaceBlockBuilder =
            NewNamespaceBlockBuilder().name(STRING_1).fullname(STRING_1).order(INT_1)
        val namespaceBlockVertex2: NewNamespaceBlockBuilder =
            NewNamespaceBlockBuilder().name(STRING_2).fullname(STRING_2).order(INT_1)
        val metaDataVertex: NewMetaDataBuilder = NewMetaDataBuilder().language(STRING_1).version(STRING_2)
        val controlStructureVertex: NewControlStructureBuilder =
            NewControlStructureBuilder().code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).order(INT_1).argumentindex(INT_1)
        val jumpTargetVertex: NewJumpTargetBuilder =
            NewJumpTargetBuilder().name(STRING_1).argumentindex(INT_1).code(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val bindingVertex: NewBindingBuilder = NewBindingBuilder().name(STRING_1).signature(STRING_2)
        val typeArgumentVertex: NewTypeArgumentBuilder = NewTypeArgumentBuilder().order(INT_1)
        val typeParameterVertex: NewTypeParameterBuilder = NewTypeParameterBuilder().name(STRING_1).order(INT_1)
        val fieldIdentifierVertex: NewFieldIdentifierBuilder =
            NewFieldIdentifierBuilder().canonicalname(STRING_1).code(STRING_1).argumentindex(INT_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val methodRefVertex: NewMethodRefBuilder =
            NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_1)).methodfullname(STRING_1).code(STRING_1)
                .order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val typeRefVertex: NewTypeRefBuilder = NewTypeRefBuilder().typefullname(STRING_1)
            .dynamictypehintfullname(createSingleItemScalaList(STRING_1))
            .code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
            .columnnumber(Option.apply(INT_1))
        val unknownVertex: NewUnknownBuilder =
            NewUnknownBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
        val modifierVertex: NewModifierBuilder = NewModifierBuilder().modifiertype(MOD_1.name).order(INT_1)

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