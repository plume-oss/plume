package io.github.plume.oss.passes.method

import io.github.plume.oss.GlobalCache
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IMethodPass
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.TYPE_DECL
import io.shiftleft.codepropertygraph.generated.nodes.*
import scala.Option
import soot.SootMethod

/**
 * Builds the method stubs which includes modifiers, parameters, and method returns.
 */
class MethodStubPass(private val driver: IDriver) : IMethodPass {

    private val nodeCache = mutableSetOf<NewNodeBuilder>()

    /**
     * Builds method stubs and connects them to their respective TYPE_DECLs, i.e.
     *
     *     TYPE_DECL -AST-> METHOD
     *     TYPE_DECL -CONTAINS-> CONTAINS
     *     METHOD -SOURCE_FILE-> FILE
     */
    override fun runPass(ms: List<SootMethod>): List<SootMethod> {
        ms.map { sm -> Pair(sm, buildMethodStub(sm)) }.forEach { p ->
            val (sm, m) = p
            val typeFullName = sm.declaringClass.type.toQuotedString()
            val filename = SootToPlumeUtil.sootClassToFileName(sm.declaringClass)
            getTypeDecl(typeFullName).let { t ->
                    driver.addEdge(t, m, AST)
                    driver.addEdge(t, m, CONTAINS)
                }
            getSourceFile(filename).let { f -> driver.addEdge(m, f, SOURCE_FILE) }
        }
        return ms
    }

    private fun getTypeDecl(typeFullName: String): NewNodeBuilder {
        return nodeCache.filterIsInstance<NewTypeDeclBuilder>()
            .find { it.build().properties().get(FULL_NAME).get() == typeFullName }
            ?: driver.getVerticesByProperty(FULL_NAME, typeFullName, TYPE_DECL)
                .first().apply { nodeCache.add(this) }
    }

    private fun getSourceFile(filename: String): NewNodeBuilder {
        return nodeCache.filterIsInstance<NewFileBuilder>()
            .find { it.build().properties().get(NAME).get() == filename }
            ?: driver.getVerticesByProperty(NAME, filename, FILE)
                .first().apply { nodeCache.add(this) }
    }

    private fun buildMethodStub(m: SootMethod): NewMethodBuilder {
        val currentLine = m.javaSourceStartLineNumber
        val currentCol = m.javaSourceStartColumnNumber
        var childIdx = 1
        val (fullName, signature, code) = SootToPlumeUtil.methodToStrings(m)
        // Method vertex
        val mtdVertex = NewMethodBuilder()
            .name(m.name)
            .fullName(fullName)
            .signature(signature)
            .filename(SootToPlumeUtil.sootClassToFileName(m.declaringClass))
            .code(code)
            .isExternal(!m.declaringClass.isApplicationClass)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx++)
            .astParentFullName("${m.declaringClass}")
            .astParentType(TYPE_DECL)
        GlobalCache.addSootAssoc(m, mtdVertex)
        // Store method vertex
        NewBlockBuilder()
            .typeFullName(m.returnType.toQuotedString())
            .code(ExtractorConst.ENTRYPOINT)
            .order(childIdx++)
            .argumentIndex(0)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { driver.addEdge(mtdVertex, this, AST); GlobalCache.addSootAssoc(m, this) }
        // Store return type
        projectMethodReturnVertex(m.returnType, currentLine, currentCol, childIdx++)
            .apply { driver.addEdge(mtdVertex, this, AST); GlobalCache.addSootAssoc(m, this) }
        // Modifier vertices
        SootParserUtil.determineModifiers(m.modifiers, m.name)
            .map { NewModifierBuilder().modifierType(it).order(childIdx++) }
            .forEach { driver.addEdge(mtdVertex, it, AST) }
        return mtdVertex
    }

    private fun projectMethodReturnVertex(
        type: soot.Type,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 0
    ): NewMethodReturnBuilder =
        NewMethodReturnBuilder()
            .code(type.toQuotedString())
            .evaluationStrategy(SootParserUtil.determineEvaluationStrategy(type.toQuotedString(), true))
            .typeFullName(type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

}