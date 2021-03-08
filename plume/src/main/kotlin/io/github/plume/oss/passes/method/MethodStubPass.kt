package io.github.plume.oss.passes.method

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IMethodPass
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeTypes
import io.shiftleft.codepropertygraph.generated.nodes.NewBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodReturnBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewModifierBuilder
import scala.Option
import soot.SootMethod

/**
 * Builds the method stubs which includes modifiers, parameters, and method returns.
 */
class MethodStubPass(private val driver: IDriver) : IMethodPass {

    override fun runPass(ms: List<SootMethod>): List<SootMethod> {
        ms.map { buildMethodStub(it) }
        return ms
    }

    private fun buildMethodStub(m: SootMethod): SootMethod {
        val currentLine = m.javaSourceStartLineNumber
        val currentCol = m.javaSourceStartColumnNumber
        var childIdx = 1
        val (fullName, signature, code) = SootToPlumeUtil.parseMethodToStrings(m)
        // Method vertex
        val mtdVertex = NewMethodBuilder()
            .name(m.name)
            .fullName(fullName)
            .signature(signature)
            .filename(SootToPlumeUtil.sootClassToFileName(m.declaringClass))
            .code(code)
            .isExternal(m.hasActiveBody())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx++)
            .astParentFullName("${m.declaringClass}")
            .astParentType(NodeTypes.TYPE_DECL)
        Extractor.addSootToPlumeAssociation(m, mtdVertex)
        // Store method vertex
        NewBlockBuilder()
            .typeFullName(m.returnType.toQuotedString())
            .code(ExtractorConst.ENTRYPOINT)
            .order(childIdx++)
            .argumentIndex(0)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { driver.addEdge(mtdVertex, this, EdgeTypes.AST); Extractor.addSootToPlumeAssociation(m, this) }
        // Store return type
        projectMethodReturnVertex(m.returnType, currentLine, currentCol, childIdx++)
            .apply { driver.addEdge(mtdVertex, this, EdgeTypes.AST); Extractor.addSootToPlumeAssociation(m, this) }
        // Modifier vertices
        SootParserUtil.determineModifiers(m.modifiers, m.name)
            .map { NewModifierBuilder().modifierType(it).order(childIdx++) }
            .forEach { driver.addEdge(mtdVertex, it, EdgeTypes.AST) }
        return m
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