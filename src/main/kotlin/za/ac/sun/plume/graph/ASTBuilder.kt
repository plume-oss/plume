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
import za.ac.sun.plume.domain.enums.DispatchType
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
import za.ac.sun.plume.util.ExtractorConst.SWITCH_ROOT
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET
import za.ac.sun.plume.util.ExtractorConst.VOID
import za.ac.sun.plume.util.SootParserUtil

class ASTBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(ASTBuilder::javaClass)

    private var order = 0
    private var currentLine = -1
    private lateinit var currentClass: FileVertex
    private lateinit var currentMethod: MethodVertex
    private lateinit var methodEntryPoint: BlockVertex
    private lateinit var graph: BriefUnitGraph
    private lateinit var sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>
    private lateinit var returnType: Type
    private val namespaceMapping = mutableMapOf<String, NamespaceBlockVertex>()
    private val typeSwitch = PlumeTypeSwitch()

    init {
        order = driver.maxOrder()
    }

    override fun build(mtd: SootMethod, graph: BriefUnitGraph, sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>) {
        logger.debug("Building AST for ${mtd.declaration}")
        this.graph = graph
        this.sootToPlume = sootToPlume
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
        // Store method vertex
        sootToPlume[mtd] = mutableListOf<PlumeVertex>(currentMethod)
        methodEntryPoint = BlockVertex(ENTRYPOINT, ENTRYPOINT, order++, 0, VOID, mtd.javaSourceStartLineNumber)
        sootToPlume[mtd]?.add(methodEntryPoint)
    }

    private fun projectMethodBody(graph: UnitGraph) {
        driver.addEdge(currentMethod, methodEntryPoint, EdgeLabel.AST)
        graph.body.parameterLocals.forEach(this::projectMethodParameterIn)
        graph.body.locals.filter { !graph.body.parameterLocals.contains(it) }.forEach { projectLocalVariable(it) }
        graph.body.units.forEach { projectUnit(it, methodEntryPoint) }
    }

    private fun projectUnit(unit: Unit, parentV: PlumeVertex): PlumeVertex? {
        currentLine = unit.javaSourceStartLineNumber
        sootToPlume[unit] = mutableListOf()

        val unitVertex: PlumeVertex? = when (unit) {
            is IfStmt -> projectIfStatement(unit)
            is AssignStmt -> projectVariableAssignment(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is InvokeStmt -> projectCallVertex(unit, parentV)
            // TODO: Discern between method return and return statement
            is ReturnStmt -> projectReturnVertex(unit)
            is ReturnVoidStmt -> projectReturnVertex(unit)
            else -> null
        }
        if (unitVertex != null) {
            driver.addEdge(parentV, unitVertex, EdgeLabel.AST)
            sootToPlume[unit]!!.add(0, unitVertex)
        }
        return unitVertex
    }

    private fun projectCallVertex(unit: InvokeStmt, parentV: PlumeVertex): PlumeVertex? {
        val callVertex = CallVertex(
                name = unit.invokeExpr.methodRef.name,
                signature = unit.invokeExpr.methodRef.signature,
                code = unit.invokeExpr.methodRef.subSignature.toString(),
                order = order++,
                lineNumber = unit.javaSourceStartLineNumber,
                methodFullName = unit.invokeExpr.method.declaration,
                methodInstFullName = unit.invokeExpr.toString(),
                argumentIndex = 0,
                dispatchType = if (unit.invokeExpr.methodRef.isStatic) DispatchType.STATIC_DISPATCH else DispatchType.DYNAMIC_DISPATCH,
                typeFullName = unit.invokeExpr.type.toString()
        )
        unit.invokeExpr.useBoxes.forEach { sootToPlume[it.value]!!.add(callVertex) }
        driver.addEdge(parentV, callVertex, EdgeLabel.AST)
        return callVertex
    }

    private fun projectTableSwitch(unit: TableSwitchStmt): PlumeVertex? {
        val switchVertex = ControlStructureVertex(
                name = SWITCH_ROOT,
                code = unit.toString(),
                lineNumber = unit.javaSourceStartLineNumber,
                order = order++,
                argumentIndex = 0
        )
        println(unit::class.java)
        println("${unit.lowIndex} -> ${unit.highIndex}")
        unit.targets.forEach {
            projectUnit(it, switchVertex)
        }
        return null
    }

    private fun projectLookupSwitch(unit: LookupSwitchStmt): PlumeVertex {
        val switchVertex = ControlStructureVertex(
                name = SWITCH_ROOT,
                code = unit.toString(),
                lineNumber = unit.javaSourceStartLineNumber,
                order = order++,
                argumentIndex = 0
        )
        unit.targets.forEachIndexed { i: Int, tgt: Unit ->
            println("${unit.getLookupValue(i)} -> $tgt")
            projectUnit(tgt, switchVertex)
        }
        return ArrayInitializerVertex(1)
    }

    private fun projectIfStatement(unit: IfStmt): ControlStructureVertex {
        val ifRootVertex = projectIfRootAndCondition(unit)
        graph.getSuccsOf(unit).forEach {
            val condBody: JumpTargetVertex = if (it == unit.target) {
                JumpTargetVertex(FALSE_TARGET, 0, it.javaSourceStartLineNumber, "ELSE_BODY", order++)
            } else {
                JumpTargetVertex(TRUE_TARGET, 0, it.javaSourceStartLineNumber, "IF_BODY", order++)
            }
            driver.addEdge(ifRootVertex, condBody, EdgeLabel.AST)
            sootToPlume[unit]!!.add(condBody)
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
        sootToPlume[unit]!!.add(conditionExpr)
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
        sootToPlume[local] = mutableListOf<PlumeVertex>(methodParameterInVertex)
    }

    private fun projectLocalVariable(local: Local): LocalVertex {
        local.type.apply(typeSwitch)
        val localVertex = LocalVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                typeFullName = typeSwitch.result.toString(),
                lineNumber = currentLine,
                order = order++
        )
        driver.addEdge(methodEntryPoint, localVertex, EdgeLabel.AST)
        sootToPlume[local] = mutableListOf<PlumeVertex>(localVertex)
        return localVertex
    }

    private fun projectVariableAssignment(unit: AssignStmt): BlockVertex {
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
        val identifierVertex = createIdentifierVertex(leftOp)
        driver.addEdge(assignBlock, identifierVertex, EdgeLabel.AST)
        projectOp(rightOp, assignBlock)
        return assignBlock
    }

    private fun projectBinopExpr(expr: BinopExpr): BlockVertex {
        val binOpExpr = BINOPS[expr.symbol.trim()] ?: throw Exception("Unknown binary operator $expr")
        val binOpBlock = BlockVertex(
                name = binOpExpr,
                code = expr.symbol.trim(),
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.type.toQuotedString(),
                lineNumber = currentLine
        )
        projectOp(expr.op1, binOpBlock)
        projectOp(expr.op2, binOpBlock)
        return binOpBlock
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

    private fun projectCastExpr(expr: CastExpr): BlockVertex {
        val castBlock = BlockVertex(
                name = CAST,
                code = "(${expr.castType.toQuotedString()})",
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.castType.toQuotedString(),
                lineNumber = currentLine
        )
        projectOp(expr.op, castBlock)
        return castBlock
    }

    private fun projectOp(expr: Value, parentV: PlumeVertex): PlumeVertex? {
        val operatorVertex = when (expr) {
            is Local -> createIdentifierVertex(expr)
            is Constant -> createLiteralVertex(expr)
            is CastExpr -> projectCastExpr(expr)
            is BinopExpr -> projectBinopExpr(expr)
            else -> null
        }
        if (operatorVertex != null) {
            driver.addEdge(parentV, operatorVertex, EdgeLabel.AST)
        }
        return operatorVertex
    }

    private fun createLiteralVertex(constant: Constant): LiteralVertex {
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
        val identifierVertex = IdentifierVertex(
                code = "$local",
                name = local.toString(),
                order = order++,
                argumentIndex = 0,
                typeFullName = local.type.toQuotedString(),
                lineNumber = currentLine
        )
        sootToPlume[local]!!.add(identifierVertex)
        return identifierVertex
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
