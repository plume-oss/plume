/*
 * Copyright 2020 David Baker Effendi
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package za.ac.sun.plume.graph

import org.apache.logging.log4j.LogManager
import soot.*
import soot.Unit
import soot.jimple.BinopExpr
import soot.jimple.CastExpr
import soot.jimple.NumericConstant
import soot.jimple.internal.JAssignStmt
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.switches.PlumeTypeSwitch
import za.ac.sun.plume.util.ExtractorConst
import za.ac.sun.plume.util.ExtractorConst.CAST
import za.ac.sun.plume.util.ExtractorConst.METHOD_BODY
import za.ac.sun.plume.util.ExtractorConst.STORE
import za.ac.sun.plume.util.ExtractorConst.VOID
import za.ac.sun.plume.util.SootParserUtil

class ASTBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(ASTBuilder::javaClass)

    private var order = 0
    private var currentLine = -1
    private lateinit var currentClass: FileVertex
    private lateinit var currentMethod: MethodVertex
    private val typeSwitch = PlumeTypeSwitch()

    init {
        order = driver.maxOrder()
    }

    override fun build(cls: SootClass) {
        logger.debug("Building AST for ${cls.name}")
        projectFileAndNamespace(cls)
        cls.methods.forEach { projectMethod(cls, it); if (it.isConcrete) projectMethodBody(it) }
    }

    private fun projectFileAndNamespace(cls: SootClass) {
        var nbv: NamespaceBlockVertex? = null
        if (cls.packageName.isNotEmpty()) {
            // Populate namespace block chain
            val namespaceList = cls.packageName.split(".").toTypedArray()
            if (namespaceList.isNotEmpty()) nbv = populateNamespaceChain(namespaceList)
        }
        currentClass = FileVertex(cls.shortName, order++)
        // Join FILE and NAMESPACE_BLOCK if namespace is present
        if (nbv != null) driver.joinFileVertexTo(currentClass, nbv)
        // Add metadata if not present
        driver.createVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version")))
    }

    /**
     * Creates a change of [NamespaceBlockVertex]s and returns the final one in the chain.
     *
     * @param namespaceList a list of package names
     * @return the final [NamespaceBlockVertex] in the chain (the one associated with the file)
     */
    private fun populateNamespaceChain(namespaceList: Array<String>): NamespaceBlockVertex {
        var prevNamespaceBlock = NamespaceBlockVertex(namespaceList[0], namespaceList[0], order++)
        if (namespaceList.size == 1) return prevNamespaceBlock
        var currNamespaceBlock: NamespaceBlockVertex? = null
        val namespaceBuilder = StringBuilder(namespaceList[0])
        for (i in 1 until namespaceList.size) {
            namespaceBuilder.append("." + namespaceList[i])
            currNamespaceBlock = NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++)
            driver.joinNamespaceBlocks(prevNamespaceBlock, currNamespaceBlock)
            prevNamespaceBlock = currNamespaceBlock
        }
        return currNamespaceBlock ?: prevNamespaceBlock
    }

    private fun projectMethod(cls: SootClass, mtd: SootMethod) {
        currentLine = mtd.javaSourceStartLineNumber
        currentMethod = MethodVertex(mtd.name, "${cls.name}.${mtd.name}", mtd.subSignature, currentLine, order++)
        driver.joinFileVertexTo(currentClass, currentMethod)
        // Return type
        val returnType = mtd.returnType
        returnType.apply(typeSwitch)
        val returnEval = SootParserUtil.determineEvaluationStrategy(typeSwitch.result.toString(), isMethodReturn = true)
        driver.createAndAddToMethod(
                currentMethod,
                MethodReturnVertex(
                        returnType.toQuotedString(),
                        typeSwitch.result.toString(),
                        returnEval,
                        currentLine,
                        order++
                )
        )
        // Modifiers
        SootParserUtil.determineModifiers(mtd.modifiers, mtd.name).forEach { driver.createAndAddToMethod(currentMethod, ModifierVertex(it, order++)) }
    }

    private fun projectMethodBody(mtd: SootMethod) {
        val body = mtd.retrieveActiveBody()
        val unitGraph = BriefUnitGraph(body)
        val mainMethodBlock = BlockVertex(METHOD_BODY, order++, 0, VOID, mtd.javaSourceStartLineNumber)
        driver.createAndAssignToBlock(currentMethod, mainMethodBlock)
        unitGraph.body.parameterLocals.forEach(this::projectMethodParameterIn)
        unitGraph.body.locals.filter { !unitGraph.body.parameterLocals.contains(it) }.forEach { projectLocalVariable(it, mainMethodBlock) }
        unitGraph.forEach { unit: Unit ->
            currentLine = unit.javaSourceStartLineNumber
            when (unit) {
                is JAssignStmt -> projectVariableAssignment(unit, mainMethodBlock)
            }
        }
    }

    private fun projectMethodParameterIn(local: Local) {
        local.type.apply(typeSwitch)
        val evalStrat = SootParserUtil.determineEvaluationStrategy(typeSwitch.result.toString(), isMethodReturn = false)
        val paramVertexLabels = MethodParameterInVertex(
                "${local.type} ${local.name}",
                local.name,
                evalStrat,
                typeSwitch.result.toString(),
                currentMethod.lineNumber,
                order++
        )
        driver.createAndAddToMethod(currentMethod, paramVertexLabels)
    }

    private fun projectLocalVariable(local: Local, mainMethodBlock: BlockVertex) {
        local.type.apply(typeSwitch)
        val localVertex = LocalVertex(
                "${local.type} ${local.name}",
                local.name, typeSwitch.result.toString(),
                mainMethodBlock.lineNumber,
                order++
        )
        driver.createAndAssignToBlock(localVertex, mainMethodBlock.order)
    }

    private fun projectVariableAssignment(unit: JAssignStmt, parentBlock: BlockVertex) {
        val leftOp = unit.leftOp
        val rightOp = unit.rightOp
        val assignBlock = BlockVertex(
                name = STORE,
                order = order++,
                argumentIndex = 0,
                typeFullName = leftOp.type.toQuotedString(),
                lineNumber = currentLine
        )
        val identifier = createIdentifierVertex(leftOp)
        driver.createAndAssignToBlock(assignBlock, parentBlock.order)
        driver.createAndAssignToBlock(identifier, assignBlock.order)
        projectOp(rightOp, assignBlock)
    }

    private fun projectBinopExpr(expr: BinopExpr, parentBlock: BlockVertex) {
        val binOpExpr = ExtractorConst.BINOPS[expr.symbol.trim()] ?: throw Exception("Unknown binary operator $expr")
        val binOpBlock = BlockVertex(
                name = binOpExpr,
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.type.toQuotedString(),
                lineNumber = currentLine
        )
        driver.createAndAssignToBlock(binOpBlock, parentBlock.order)
        projectOp(expr.op1, binOpBlock)
        projectOp(expr.op2, binOpBlock)
    }

    private fun projectCastExpr(expr: CastExpr, parentBlock: BlockVertex) {
        val castBlock = BlockVertex(
                name = CAST,
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.castType.toQuotedString(),
                lineNumber = currentLine
        )
        driver.createAndAssignToBlock(castBlock, parentBlock.order)
        projectOp(expr.op, castBlock)
    }

    private fun projectOp(expr: Value, parentBlock: BlockVertex) {
        when (expr) {
            is Local -> {
                val localVertex = createIdentifierVertex(expr)
                driver.createAndAssignToBlock(localVertex, parentBlock.order)
            }
            is NumericConstant -> {
                val literalVertex = createLiteralVertex(expr)
                driver.createAndAssignToBlock(literalVertex, parentBlock.order)
            }
            is CastExpr -> projectCastExpr(expr, parentBlock)
            is BinopExpr -> projectBinopExpr(expr, parentBlock)
        }
    }

    private fun createLiteralVertex(constant: NumericConstant): LiteralVertex {
        return LiteralVertex(
                name = constant.toString(),
                order = order++,
                argumentIndex = 0,
                typeFullName = constant.type.toQuotedString(),
                lineNumber = currentLine
        )
    }

    private fun createIdentifierVertex(local: Value): IdentifierVertex {
        return IdentifierVertex(
                code = "$local",
                name = local.toString(),
                order = order++,
                argumentIndex = 0,
                typeFullName = local.type.toQuotedString(),
                lineNumber = currentLine
        )
    }

}
