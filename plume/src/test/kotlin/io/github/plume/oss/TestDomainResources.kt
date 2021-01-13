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

        val vertices = listOf(
            NewArrayInitializerBuilder().order(INT_1).build(),
            NewBindingBuilder().name(STRING_1).signature(STRING_2).build(),
            NewBlockBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewCallBuilder().methodfullname(STRING_1).argumentindex(INT_1).dispatchtype(DISPATCH_1.name)
                .typefullname(STRING_1)
                .dynamictypehintfullname(createSingleItemScalaList(STRING_1) as scala.collection.immutable.List<String>)
                .name(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build(),
            NewControlStructureBuilder().code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).order(INT_1).argumentindex(INT_1).build(),
            NewFieldIdentifierBuilder().canonicalname(STRING_1).code(STRING_1).argumentindex(INT_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewFileBuilder().name(STRING_1).hash(Option.apply(STRING_2)).order(INT_1).build(),
            NewIdentifierBuilder().name(STRING_1).typefullname(STRING_1).code(STRING_1).order(INT_1)
                .argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewJumpTargetBuilder().name(STRING_1).argumentindex(INT_1).code(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewLiteralBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewLocalBuilder().code(STRING_1).typefullname(STRING_1).name(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewMemberBuilder().code(STRING_1).name(STRING_1).typefullname(STRING_1).order(INT_1).build(),
            NewMetaDataBuilder().language(STRING_1).version(STRING_1).build(),
            NewMethodParameterInBuilder().code(STRING_1).evaluationstrategy(EVAL_1.name).typefullname(STRING_1)
                .name(STRING_1).order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                .build(),
            NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_1)).methodfullname(STRING_1).code(STRING_1)
                .order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                .build(),
            NewMethodReturnBuilder().typefullname(STRING_1).evaluationstrategy(EVAL_1.name).code(STRING_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewMethodBuilder().name(STRING_1).fullname(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build(),
            NewModifierBuilder().modifiertype(MOD_1.name).order(INT_1).build(),
            NewNamespaceBlockBuilder().name(STRING_1).fullname(STRING_1).order(INT_1).build(),
            NewReturnBuilder().order(INT_1).argumentindex(INT_1).code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build(),
            NewTypeArgumentBuilder().order(INT_1),
            NewTypeDeclBuilder().name(STRING_1).fullname(STRING_1).order(INT_1).build(),
            NewTypeParameterBuilder().name(STRING_1).order(INT_1).build(),
            NewTypeRefBuilder().typefullname(STRING_1)
                .dynamictypehintfullname(createSingleItemScalaList(STRING_1) as scala.collection.immutable.List<String>)
                .code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build(),
            NewTypeBuilder().name(STRING_1).fullname(STRING_1).typedeclfullname(STRING_1).build(),
            NewUnknownBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        )

        val methodVertex: NewMethod =
            NewMethodBuilder().code(STRING_1).name(STRING_1).fullname(STRING_1).order(INT_1).build()
        val methodParameterInVertex: NewMethodParameterIn =
            NewMethodParameterInBuilder().code(STRING_1).evaluationstrategy(EVAL_1.name).typefullname(STRING_1)
                .name(STRING_1).order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                .build()
        val blockVertex: NewBlock =
            NewBlockBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val callVertex: NewCall =
            NewCallBuilder().methodfullname(STRING_1).argumentindex(INT_1).dispatchtype(DISPATCH_1.name)
                .typefullname(STRING_1)
                .dynamictypehintfullname(createSingleItemScalaList(STRING_1) as scala.collection.immutable.List<String>)
                .name(STRING_1).signature(STRING_1).code(STRING_1).order(INT_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build()
        val localVertex: NewLocal = NewLocalBuilder().code(STRING_1).typefullname(STRING_1).name(STRING_1).order(INT_1)
            .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val identifierVertex: NewIdentifier =
            NewIdentifierBuilder().name(STRING_1).typefullname(STRING_1).code(STRING_1).order(INT_1)
                .argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val typeDeclVertex: NewTypeDecl = NewTypeDeclBuilder().name(STRING_1).fullname(STRING_1).order(INT_1).build()
        val literalVertex: NewLiteral =
            NewLiteralBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val returnVertex: NewReturn =
            NewReturnBuilder().order(INT_1).argumentindex(INT_1).code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).build()
        val methodReturnVertex: NewMethodReturn =
            NewMethodReturnBuilder().typefullname(STRING_1).evaluationstrategy(EVAL_1.name).code(STRING_1)
                .order(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val fileVertex: NewFile = NewFileBuilder().name(STRING_1).hash(Option.apply(STRING_2)).order(INT_1).build()
        val namespaceBlockVertex1: NewNamespaceBlock =
            NewNamespaceBlockBuilder().name(STRING_1).fullname(STRING_1).order(INT_1).build()
        val namespaceBlockVertex2: NewNamespaceBlock =
            NewNamespaceBlockBuilder().name(STRING_2).fullname(STRING_2).order(INT_1).build()
        val metaDataVertex: NewMetaData = NewMetaDataBuilder().language(STRING_1).version(STRING_2).build()
        val controlStructureVertex: NewControlStructure =
            NewControlStructureBuilder().code(STRING_1).linenumber(Option.apply(INT_1))
                .columnnumber(Option.apply(INT_1)).order(INT_1).argumentindex(INT_1).build()
        val jumpTargetVertex: NewJumpTarget =
            NewJumpTargetBuilder().name(STRING_1).argumentindex(INT_1).code(STRING_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val bindingVertex: NewBinding = NewBindingBuilder().name(STRING_1).signature(STRING_2).build()
        val typeArgumentVertex: NewTypeArgumentBuilder = NewTypeArgumentBuilder().order(INT_1)
        val typeParameterVertex: NewTypeParameter = NewTypeParameterBuilder().name(STRING_1).order(INT_1).build()
        val fieldIdentifierVertex: NewFieldIdentifier =
            NewFieldIdentifierBuilder().canonicalname(STRING_1).code(STRING_1).argumentindex(INT_1).order(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val methodRefVertex: NewMethodRef =
            NewMethodRefBuilder().methodinstfullname(Option.apply(STRING_1)).methodfullname(STRING_1).code(STRING_1)
                .order(INT_1).argumentindex(INT_1).linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1))
                .build()
        val typeRefVertex: NewTypeRef = NewTypeRefBuilder().typefullname(STRING_1)
            .dynamictypehintfullname(createSingleItemScalaList(STRING_1) as scala.collection.immutable.List<String>)
            .code(STRING_1).argumentindex(INT_1).order(INT_1).linenumber(Option.apply(INT_1))
            .columnnumber(Option.apply(INT_1)).build()
        val unknownVertex: NewUnknown =
            NewUnknownBuilder().typefullname(STRING_1).code(STRING_1).order(INT_1).argumentindex(INT_1)
                .linenumber(Option.apply(INT_1)).columnnumber(Option.apply(INT_1)).build()
        val modifierVertex: NewModifier = NewModifierBuilder().modifiertype(MOD_1.name).order(INT_1).build()

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