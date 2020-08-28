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
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph
import soot.toolkits.graph.UnitGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.ASTVertex
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.switches.PlumeTypeSwitch
import za.ac.sun.plume.util.ExtractorConst.ASSIGN
import za.ac.sun.plume.util.ExtractorConst.BINOPS
import za.ac.sun.plume.util.ExtractorConst.BOOLEAN
import za.ac.sun.plume.util.ExtractorConst.CAST
import za.ac.sun.plume.util.ExtractorConst.ENTRYPOINT
import za.ac.sun.plume.util.ExtractorConst.FALSE_TARGET
import za.ac.sun.plume.util.ExtractorConst.IF_ROOT
import za.ac.sun.plume.util.ExtractorConst.RETURN
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET
import za.ac.sun.plume.util.ExtractorConst.VOID
import za.ac.sun.plume.util.SootParserUtil

class ASTBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(ASTBuilder::javaClass)

    private var order = 0
    private var currentLine = -1
    private lateinit var currentClass: FileVertex
    private lateinit var currentMethod: MethodVertex
    private lateinit var graph: BriefUnitGraph
    private lateinit var unitToVertex: MutableMap<Unit, PlumeVertex>
    private lateinit var returnType: Type
    private val namespaceMapping = mutableMapOf<String, NamespaceBlockVertex>()
    private val typeSwitch = PlumeTypeSwitch()

    init {
        order = driver.maxOrder()
    }

    override fun build(mtd: SootMethod, graph: BriefUnitGraph, unitToVertex: MutableMap<Unit, PlumeVertex>) {
        logger.debug("Building AST for ${mtd.declaration}")
        this.graph = graph
        this.unitToVertex = unitToVertex
        projectMethodHead(mtd)
        projectMethodBody(graph)
    }

    fun buildFileAndNamespace(cls: SootClass) {
        logger.debug("Building file and namespace for ${cls.name}")
        var nbv: NamespaceBlockVertex? = null
        if (cls.packageName.isNotEmpty()) {
            // Populate namespace block chain
            val namespaceList = cls.packageName.split(".").toTypedArray()
            if (namespaceList.isNotEmpty()) nbv = populateNamespaceChain(namespaceList)
        }
        currentClass = FileVertex(cls.shortName, order++)
        // Join FILE and NAMESPACE_BLOCK if namespace is present
        if (nbv != null) driver.addEdge(nbv, currentClass, EdgeLabel.AST)
        // Add metadata if not present
        // TODO: This is currently erroneous and can be improved
        driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version")))
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
        namespaceMapping[namespaceList[0]] = prevNamespaceBlock
        for (i in 1 until namespaceList.size) {
            namespaceBuilder.append("." + namespaceList[i])
            currNamespaceBlock = NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++)
            namespaceMapping[namespaceList[0]] = prevNamespaceBlock
            if (!driver.exists(prevNamespaceBlock, currNamespaceBlock, EdgeLabel.AST))
                driver.addEdge(prevNamespaceBlock, currNamespaceBlock, EdgeLabel.AST)
            prevNamespaceBlock = currNamespaceBlock
        }
        return currNamespaceBlock ?: prevNamespaceBlock
    }

    private fun projectMethodHead(mtd: SootMethod) {
        currentLine = mtd.javaSourceStartLineNumber
        // Method vertex
        currentMethod = MethodVertex(mtd.name, "${mtd.declaringClass}.${mtd.name}", mtd.subSignature, mtd.declaration, currentLine, order++)
        driver.addEdge(currentClass, currentMethod, EdgeLabel.AST)
        // Store return type
        returnType = mtd.returnType
        // Modifier vertices
        SootParserUtil.determineModifiers(mtd.modifiers, mtd.name).forEach { driver.addEdge(currentMethod, ModifierVertex(it, order++), EdgeLabel.AST) }
    }

    private fun projectMethodBody(unitGraph: UnitGraph) {
        val mainMethodBlock = BlockVertex(ENTRYPOINT, ENTRYPOINT, order++, 0, VOID, unitGraph.heads.first().javaSourceStartLineNumber)
        driver.addEdge(currentMethod, mainMethodBlock, EdgeLabel.AST)
        unitGraph.body.parameterLocals.forEach(this::projectMethodParameterIn)
        unitGraph.body.locals.filter { !unitGraph.body.parameterLocals.contains(it) }.forEach { projectLocalVariable(it, mainMethodBlock) }
        unitGraph.body.units.forEach { projectUnit(it, mainMethodBlock) }
    }

    private fun projectUnit(unit: Unit, parentV: PlumeVertex) {
        currentLine = unit.javaSourceStartLineNumber

        val unitV: PlumeVertex? = when (unit) {
            is GotoStmt -> null
            is IfStmt -> projectIfStatement(unit, parentV)
            is AssignStmt -> projectVariableAssignment(unit, parentV)
            is LookupSwitchStmt -> TODO("Handle LookupSwitchStmt")
            is TableSwitchStmt -> TODO("Handle TableSwitchStmt")
            // TODO: Discern between method return and return statement
            is ReturnStmt -> projectReturnVertex(unit)
            is ReturnVoidStmt -> projectReturnVertex(unit)
            else -> null
        }
        if (unitV != null) unitToVertex[unit] = unitV
    }

    private fun projectIfStatement(unit: IfStmt, parentV: PlumeVertex): ControlStructureVertex {
        val ifRootVertex = projectIfRootAndCondition(unit)
        driver.addEdge(parentV, ifRootVertex, EdgeLabel.AST)
        unitToVertex[unit] = ifRootVertex
        graph.getSuccsOf(unit).forEach {
            val condBody: JumpTargetVertex = if (it == unit.target) {
                JumpTargetVertex(FALSE_TARGET, 0, it.javaSourceStartLineNumber, "ELSE_BODY", order++)
            } else {
                JumpTargetVertex(TRUE_TARGET, 0, it.javaSourceStartLineNumber, "IF_BODY", order++)
            }
            driver.addEdge(ifRootVertex, condBody, EdgeLabel.AST)
            unitToVertex[it] = condBody
        }
        return ifRootVertex
    }

    private fun projectIfRootAndCondition(unit: IfStmt): ControlStructureVertex {
        val ifRootVertex = ControlStructureVertex(
                name = IF_ROOT,
                code = unit.toString(),
                lineNumber = currentLine,
                order = order++,
                argumentIndex = 0
        )
        driver.addVertex(ifRootVertex)
        val condition = unit.condition as ConditionExpr
        val conditionExpr = projectFlippedConditionalExpr(condition)
        driver.addEdge(ifRootVertex, conditionExpr, EdgeLabel.CONDITION)
        return ifRootVertex
    }

    private fun projectMethodParameterIn(local: Local) {
        local.type.apply(typeSwitch)
        val evalStrat = SootParserUtil.determineEvaluationStrategy(typeSwitch.result.toString(), isMethodReturn = false)
        val methodParameterInVertex = MethodParameterInVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                evaluationStrategy = evalStrat,
                typeFullName = typeSwitch.result.toString(),
                lineNumber = currentMethod.lineNumber,
                order = order++
        )
        driver.addEdge(currentMethod, methodParameterInVertex, EdgeLabel.AST)
    }

    private fun projectLocalVariable(local: Local, mainMethodBlock: ASTVertex) {
        local.type.apply(typeSwitch)
        val localVertex = LocalVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                typeFullName = typeSwitch.result.toString(),
                lineNumber = currentLine,
                order = order++
        )
        driver.addEdge(mainMethodBlock, localVertex, EdgeLabel.AST)
    }

    private fun projectVariableAssignment(unit: AssignStmt, parentV: PlumeVertex): BlockVertex {
        val leftOp = unit.leftOp
        val rightOp = unit.rightOp
        val assignBlock = BlockVertex(
                name = ASSIGN,
                code = "=",
                order = order++,
                argumentIndex = 0,
                typeFullName = leftOp.type.toQuotedString(),
                lineNumber = currentLine
        )
        val identifier = createIdentifierVertex(leftOp)
        driver.addEdge(parentV, assignBlock, EdgeLabel.AST)
        driver.addEdge(assignBlock, identifier, EdgeLabel.AST)
        projectOp(rightOp, assignBlock)
        return assignBlock
    }

    private fun projectBinopExpr(expr: BinopExpr, parentV: PlumeVertex) {
        val binOpExpr = BINOPS[expr.symbol.trim()] ?: throw Exception("Unknown binary operator $expr")
        val binOpBlock = BlockVertex(
                name = binOpExpr,
                code = expr.symbol.trim(),
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.type.toQuotedString(),
                lineNumber = currentLine
        )
        driver.addEdge(parentV, binOpBlock, EdgeLabel.AST)
        projectOp(expr.op1, binOpBlock)
        projectOp(expr.op2, binOpBlock)
    }

    private fun projectFlippedConditionalExpr(expr: ConditionExpr): BlockVertex {
        val operator = SootParserUtil.parseAndFlipEquality(expr.symbol.trim())
        val binOpBlock = BlockVertex(
                name = operator,
                code = BINOPS.filter { it.value == operator }.keys.first(),
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.type.toQuotedString(),
                lineNumber = currentLine
        )
        projectOp(expr.op1, binOpBlock)
        projectOp(expr.op2, binOpBlock)
        return binOpBlock
    }

    private fun projectCastExpr(expr: CastExpr, parentV: PlumeVertex) {
        val castBlock = BlockVertex(
                name = CAST,
                code = "(${expr.castType.toQuotedString()})",
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.castType.toQuotedString(),
                lineNumber = currentLine
        )
        driver.addEdge(parentV, castBlock, EdgeLabel.AST)
        projectOp(expr.op, castBlock)
    }

    private fun projectOp(expr: Value, parentV: PlumeVertex) {
        when (expr) {
            is Local -> {
                val localVertex = createIdentifierVertex(expr)
                driver.addEdge(parentV, localVertex, EdgeLabel.AST)
            }
            is NumericConstant -> {
                val literalVertex = createLiteralVertex(expr)
                driver.addEdge(parentV, literalVertex, EdgeLabel.AST)
            }
            is CastExpr -> projectCastExpr(expr, parentV)
            is BinopExpr -> projectBinopExpr(expr, parentV)
        }
    }

    private fun createLiteralVertex(constant: NumericConstant): LiteralVertex {
        return LiteralVertex(
                name = constant.toString(),
                code = constant.toString(),
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

    private fun projectReturnVertex(ret: ReturnStmt): MethodReturnVertex {
        val vertexReturnType: String = if (returnType.toQuotedString() == BOOLEAN) BOOLEAN else ret.op.type.toQuotedString()
        val retV = MethodReturnVertex(
                name = RETURN,
                code = ret.toString(),
                evaluationStrategy = SootParserUtil.determineEvaluationStrategy(ret.op.type.toQuotedString(), true),
                typeFullName = vertexReturnType,
                lineNumber = ret.javaSourceStartLineNumber,
                order = order++
        )
        projectOp(ret.op, retV)
        driver.addEdge(currentMethod, retV, EdgeLabel.AST)
        return retV
    }

    private fun projectReturnVertex(ret: ReturnVoidStmt): MethodReturnVertex {
        val retV = MethodReturnVertex(
                name = RETURN,
                code = ret.toString(),
                evaluationStrategy = SootParserUtil.determineEvaluationStrategy(VOID, true),
                typeFullName = VOID,
                lineNumber = ret.javaSourceStartLineNumber,
                order = order++
        )
        driver.addEdge(currentMethod, retV, EdgeLabel.AST)
        return retV
    }
}
