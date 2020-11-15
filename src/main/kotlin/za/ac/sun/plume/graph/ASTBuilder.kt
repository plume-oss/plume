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
import soot.Local
import soot.Unit
import soot.Value
import soot.jimple.*
import soot.jimple.internal.JimpleLocalBox
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.Extractor.Companion.addSootToPlumeAssociation
import za.ac.sun.plume.Extractor.Companion.getSootAssociation
import za.ac.sun.plume.domain.enums.DispatchType
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.util.ExtractorConst.ASSIGN
import za.ac.sun.plume.util.ExtractorConst.BINOPS
import za.ac.sun.plume.util.ExtractorConst.CAST
import za.ac.sun.plume.util.ExtractorConst.FALSE_TARGET
import za.ac.sun.plume.util.ExtractorConst.IF_ROOT
import za.ac.sun.plume.util.ExtractorConst.LOOKUP_ROOT
import za.ac.sun.plume.util.ExtractorConst.SWITCH_ROOT
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET
import za.ac.sun.plume.util.SootParserUtil
import za.ac.sun.plume.util.SootToPlumeUtil

/**
 * The [IGraphBuilder] that constructs the vertices of the package/file/method hierarchy and connects the AST edges.
 *
 * @param driver The driver to build the AST with.
 */
class ASTBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(ASTBuilder::javaClass)

    private var currentLine = -1
    private var currentCol = -1
    private lateinit var graph: BriefUnitGraph

    init {
        order = driver.maxOrder()
    }

    companion object {
        /**
         * General ordering property, such that the children of each AST-node are typically numbered from 1, ..., N
         * (this is not enforced). The ordering has no technical meaning, but is used for pretty printing and OUGHT TO
         * reflect order in the source code.
         */
        var order = 0
            private set

        /**
         * Returns the current order and increments it.
         */
        fun incOrder() = order++
    }

    override fun buildMethodBody(graph: BriefUnitGraph): MethodVertex {
        val mtd = graph.body.method
        this.graph = graph
        logger.debug("Building AST for ${mtd.declaration}")
        // Connect and create parameters and locals
        getSootAssociation(mtd)?.let { mtdVs ->
            mtdVs.filterIsInstance<MethodVertex>().firstOrNull()?.let { mtdVert ->
                addSootToPlumeAssociation(mtd, buildLocals(graph, mtdVert))
            }
        }
        // Build body
        graph.body.units.filterNot { it is IdentityStmt }
                .forEach { u ->
                    projectUnit(u)
                            ?.let {
                                driver.addEdge(getSootAssociation(mtd)
                                !!.first { v -> v is BlockVertex }, it, EdgeLabel.AST)
                            }
                }
        return getSootAssociation(mtd)?.first { it is MethodVertex } as MethodVertex

    }

    private fun buildLocals(graph: BriefUnitGraph, mtdVertex: MethodVertex): MutableList<PlumeVertex> {
        val localVertices = mutableListOf<PlumeVertex>()
        graph.body.parameterLocals
                .map {
                    SootToPlumeUtil.projectMethodParameterIn(it, currentLine)
                            .apply { addSootToPlumeAssociation(it, this) }
                }
                .forEach { driver.addEdge(mtdVertex, it, EdgeLabel.AST); localVertices.add(it) }
        graph.body.locals
                .filter { !graph.body.parameterLocals.contains(it) }
                .map {
                    SootToPlumeUtil.projectLocalVariable(it, currentLine, currentCol)
                            .apply { addSootToPlumeAssociation(it, this) }
                }
                .forEach { driver.addEdge(mtdVertex, it, EdgeLabel.AST); localVertices.add(it) }
        return localVertices
    }


    /**
     * Given a unit, will construct AST information in the graph.
     *
     * @param unit The [Unit] from which AST vertices and edges will be constructed.
     */
    private fun projectUnit(unit: Unit): PlumeVertex? {
        currentLine = unit.javaSourceStartLineNumber
        currentCol = unit.javaSourceStartColumnNumber

        val unitVertex: PlumeVertex? = when (unit) {
            is IfStmt -> projectIfStatement(unit)
            is AssignStmt -> projectVariableAssignment(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is InvokeStmt -> projectCallVertex(unit.invokeExpr)
            is ReturnStmt -> projectReturnVertex(unit)
            is ReturnVoidStmt -> projectReturnVertex(unit)
            else -> {
                logger.debug("Unhandled class in projectUnit ${unit.javaClass} $unit"); null
            }
        }
        return unitVertex?.apply { if (this !is InvokeStmt) addSootToPlumeAssociation(unit, this, 0) }
    }

    /**
     * Given an [InvokeExpr], will construct Call information in the graph.
     *
     * @param unit The [InvokeExpr] from which a [CallVertex] will be constructed.
     * @return the [CallVertex] constructed.
     */
    private fun projectCallVertex(unit: InvokeExpr): PlumeVertex {
        val callVertex = CallVertex(
                name = unit.methodRef.name,
                signature = unit.methodRef.signature,
                code = unit.methodRef.subSignature.toString(),
                order = order++,
                lineNumber = currentLine,
                columnNumber = currentCol,
                methodFullName = unit.methodRef.toString().removeSurrounding("<", ">"),
                argumentIndex = 0,
                dispatchType = if (unit.methodRef.isStatic) DispatchType.STATIC_DISPATCH else DispatchType.DYNAMIC_DISPATCH,
                typeFullName = unit.type.toString(),
                dynamicTypeHintFullName = unit.type.toQuotedString()
        )
        val callVertices = mutableListOf<PlumeVertex>(callVertex)
        // Create vertices for arguments
        unit.args.map { it }.forEach {
            when (it) {
                is Local -> SootToPlumeUtil.createIdentifierVertex(it, currentLine, currentCol)
                is Constant -> SootToPlumeUtil.createLiteralVertex(it, currentLine, currentCol)
                else -> null
            }?.let { expressionVertex ->
                driver.addEdge(callVertex, expressionVertex, EdgeLabel.AST)
                callVertices.add(expressionVertex)
                addSootToPlumeAssociation(it, expressionVertex)
            }
        }
        // Save PDG arguments
        addSootToPlumeAssociation(unit, callVertices)
        // Create the receiver for the call
        unit.useBoxes.filterIsInstance<JimpleLocalBox>().firstOrNull()?.let {
            SootToPlumeUtil.createIdentifierVertex(it.value, currentLine, currentCol).apply {
                addSootToPlumeAssociation(it.value, this)
                driver.addEdge(callVertex, this, EdgeLabel.RECEIVER)
            }
        }
        return callVertex
    }

    /**
     * Given an [TableSwitchStmt], will construct table switch information in the graph.
     *
     * @param unit The [TableSwitchStmt] from which a [ControlStructureVertex] will be constructed.
     * @return the [ControlStructureVertex] constructed.
     */
    private fun projectTableSwitch(unit: TableSwitchStmt): ControlStructureVertex {
        val switchVertex = ControlStructureVertex(
                name = SWITCH_ROOT,
                code = unit.toString(),
                lineNumber = unit.javaSourceStartLineNumber,
                columnNumber = unit.javaSourceStartColumnNumber,
                order = order++,
                argumentIndex = 0
        )
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) {
                val tgtV = JumpTargetVertex("CASE $i", i, tgt.javaSourceStartLineNumber, tgt.javaSourceStartColumnNumber, tgt.toString(), order++)
                driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
                addSootToPlumeAssociation(unit, tgtV)
            }
        }
        return switchVertex
    }

    /**
     * Given an [LookupSwitchStmt], will construct lookup switch information in the graph.
     *
     * @param unit The [LookupSwitchStmt] from which a [ControlStructureVertex] will be constructed.
     * @return the [ControlStructureVertex] constructed.
     */
    private fun projectLookupSwitch(unit: LookupSwitchStmt): ControlStructureVertex {
        val switchVertex = ControlStructureVertex(
                name = LOOKUP_ROOT,
                code = unit.toString(),
                lineNumber = unit.javaSourceStartLineNumber,
                columnNumber = unit.javaSourceStartColumnNumber,
                order = order++,
                argumentIndex = 0
        )
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            if (unit.defaultTarget != tgt) {
                val lookupValue = unit.getLookupValue(i)
                val tgtV = JumpTargetVertex("CASE $lookupValue", lookupValue, tgt.javaSourceStartLineNumber, tgt.javaSourceStartColumnNumber, tgt.toString(), order++)
                driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
                addSootToPlumeAssociation(unit, tgtV)
            }
        }
        return switchVertex
    }

    /**
     * Creates the default jump target for the given [SwitchStmt] and links it to the given switch vertex.
     *
     * @param unit The [LookupSwitchStmt] from which a [ControlStructureVertex] will be constructed.
     * @param switchVertex The [ControlStructureVertex] representing the switch statement to link.
     */
    private fun projectSwitchDefault(unit: SwitchStmt, switchVertex: ControlStructureVertex) {
        projectOp(unit.key)?.let { driver.addEdge(switchVertex, it, EdgeLabel.CONDITION) }
        // Handle default target jump
        unit.defaultTarget.let {
            val tgtV = JumpTargetVertex("DEFAULT", -1, it.javaSourceStartLineNumber, it.javaSourceStartColumnNumber, it.toString(), order++)
            driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
            addSootToPlumeAssociation(unit, tgtV)
        }
    }

    /**
     * Given an [IfStmt], will construct if statement information in the graph.
     *
     * @param unit The [IfStmt] from which a [ControlStructureVertex] will be constructed.
     * @return the [ControlStructureVertex] constructed.
     */
    private fun projectIfStatement(unit: IfStmt): ControlStructureVertex {
        val ifRootVertex = projectIfRootAndCondition(unit)
        graph.getSuccsOf(unit).forEach {
            val condBody: JumpTargetVertex = if (it == unit.target) {
                JumpTargetVertex(FALSE_TARGET, 0, it.javaSourceStartLineNumber, it.javaSourceStartColumnNumber, "ELSE_BODY", order++)
            } else {
                JumpTargetVertex(TRUE_TARGET, 1, it.javaSourceStartLineNumber, it.javaSourceStartColumnNumber, "IF_BODY", order++)
            }
            driver.addEdge(ifRootVertex, condBody, EdgeLabel.AST)
            addSootToPlumeAssociation(unit, condBody)
        }
        return ifRootVertex
    }

    /**
     * Given an [IfStmt], will construct condition edge and vertex information.
     *
     * @param unit The [IfStmt] from which a [ControlStructureVertex] and condition [BlockVertex] will be constructed.
     * @return the [ControlStructureVertex] constructed.
     */
    private fun projectIfRootAndCondition(unit: IfStmt): ControlStructureVertex {
        val ifRootVertex = ControlStructureVertex(
                name = IF_ROOT,
                code = unit.toString(),
                lineNumber = unit.javaSourceStartLineNumber,
                columnNumber = unit.javaSourceStartColumnNumber,
                order = order++,
                argumentIndex = 0
        )
        driver.addVertex(ifRootVertex)
        val condition = unit.condition as ConditionExpr
        val conditionExpr = projectFlippedConditionalExpr(condition)
        driver.addEdge(ifRootVertex, conditionExpr, EdgeLabel.CONDITION)
        addSootToPlumeAssociation(unit, conditionExpr)
        return ifRootVertex
    }

    /**
     * Given an [AssignStmt], will construct variable assignment edge and vertex information.
     *
     * @param unit The [AssignStmt] from which a [CallVertex] and its children vertices will be constructed.
     * @return the [CallVertex] constructed.
     */
    private fun projectVariableAssignment(unit: DefinitionStmt): CallVertex {
        val assignVariables = mutableListOf<PlumeVertex>()
        val leftOp = unit.leftOp
        val rightOp = unit.rightOp
        val assignBlock = CallVertex(
                name = ASSIGN,
                code = "=",
                signature = "${leftOp.type} = ${rightOp.type}",
                methodFullName = "=",
                dispatchType = DispatchType.STATIC_DISPATCH,
                order = order++,
                argumentIndex = 0,
                typeFullName = leftOp.type.toQuotedString(),
                dynamicTypeHintFullName = rightOp.type.toQuotedString(),
                lineNumber = unit.javaSourceStartLineNumber,
                columnNumber = unit.javaSourceStartColumnNumber
        )
        when (leftOp) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(leftOp, currentLine, currentCol).apply {
                addSootToPlumeAssociation(leftOp, this)
            }
            is FieldRef -> SootToPlumeUtil.createFieldIdentifierVertex(leftOp, currentLine, currentCol).apply {
                addSootToPlumeAssociation(leftOp.field, this)
            }
            is ArrayRef -> SootToPlumeUtil.createArrayRefIdentifier(leftOp, currentLine, currentCol).apply {
                addSootToPlumeAssociation(leftOp.base, this)
            }
            else -> {
                logger.debug("Unhandled class for leftOp under projectVariableAssignment: ${leftOp.javaClass} containing value $leftOp")
                null
            }
        }?.let { driver.addEdge(assignBlock, it, EdgeLabel.AST); assignVariables.add(it) }
        projectOp(rightOp)?.let { driver.addEdge(assignBlock, it, EdgeLabel.AST); assignVariables.add(it) }
        // Save PDG arguments
        addSootToPlumeAssociation(unit, assignVariables)
        return assignBlock
    }

    /**
     * Given an [BinopExpr], will construct the root operand as a [CallVertex] and left and right operations of the
     * binary operation.
     *
     * @param expr The [BinopExpr] from which a [CallVertex] and its children vertices will be constructed.
     * @return the [CallVertex] constructed.
     */
    private fun projectBinopExpr(expr: BinopExpr): CallVertex {
        val binopVertices = mutableListOf<PlumeVertex>()
        val binOpExpr = BINOPS[expr.symbol.trim()] ?: throw Exception("Unknown binary operator $expr")
        val binOpBlock = CallVertex(
                name = binOpExpr,
                code = expr.symbol.trim(),
                signature = "${expr.op1.type.toQuotedString()}${expr.symbol}${expr.op2.type.toQuotedString()}",
                methodFullName = expr.symbol.trim(),
                dispatchType = DispatchType.STATIC_DISPATCH,
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.type.toQuotedString(),
                dynamicTypeHintFullName = expr.type.toString(),
                lineNumber = currentLine,
                columnNumber = currentCol
        ).apply { binopVertices.add(this) }
        projectOp(expr.op1)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST); binopVertices.add(it) }
        projectOp(expr.op2)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST); binopVertices.add(it) }
        // Save PDG arguments
        addSootToPlumeAssociation(expr, binopVertices)
        return binOpBlock
    }

    private fun projectFlippedConditionalExpr(expr: ConditionExpr): CallVertex {
        val conditionVertices = mutableListOf<PlumeVertex>()
        val operator = SootParserUtil.parseAndFlipEquality(expr.symbol.trim())
        val symbol = BINOPS.filter { it.value == operator }.keys.first()
        val binOpBlock = CallVertex(
                name = operator,
                code = symbol,
                order = order++,
                argumentIndex = 0,
                dispatchType = DispatchType.STATIC_DISPATCH,
                signature = "${expr.op1.type} $symbol ${expr.op2.type}",
                methodFullName = symbol,
                typeFullName = expr.type.toQuotedString(),
                dynamicTypeHintFullName = expr.type.toQuotedString(),
                lineNumber = currentLine,
                columnNumber = currentCol
        ).apply { conditionVertices.add(this) }
        projectOp(expr.op1)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST); conditionVertices.add(it) }
        projectOp(expr.op2)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST); conditionVertices.add(it) }
        addSootToPlumeAssociation(expr, conditionVertices)
        return binOpBlock
    }

    private fun projectCastExpr(expr: CastExpr): CallVertex {
        val castVertices = mutableListOf<PlumeVertex>()
        val castBlock = CallVertex(
                name = CAST,
                code = "(${expr.castType.toQuotedString()})",
                signature = "(${expr.castType.toQuotedString()}) ${expr.op.type.toQuotedString()}",
                dispatchType = DispatchType.STATIC_DISPATCH,
                order = order++,
                argumentIndex = 0,
                typeFullName = expr.castType.toQuotedString(),
                methodFullName = "(${expr.castType.toQuotedString()})",
                dynamicTypeHintFullName = expr.castType.toQuotedString(),
                lineNumber = currentLine,
                columnNumber = currentCol
        ).apply { castVertices.add(this) }
        projectOp(expr.op)?.let {
            driver.addEdge(castBlock, it, EdgeLabel.AST); castVertices.add(it)
        }
        // Save PDG arguments
        addSootToPlumeAssociation(expr, castVertices)
        return castBlock
    }

    private fun projectOp(expr: Value): PlumeVertex? {
        return when (expr) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(expr, currentLine, currentCol)
            is Constant -> SootToPlumeUtil.createLiteralVertex(expr, currentLine, currentCol)
            is CastExpr -> projectCastExpr(expr)
            is BinopExpr -> projectBinopExpr(expr)
            is InvokeExpr -> projectCallVertex(expr)
            is StaticFieldRef -> SootToPlumeUtil.createFieldIdentifierVertex(expr, currentLine, currentCol)
            is NewExpr -> createNewExpr(expr)
            is NewArrayExpr -> createNewArrayExpr(expr)
            is CaughtExceptionRef -> SootToPlumeUtil.createIdentifierVertex(expr, currentLine, currentCol)
            else -> {
                logger.debug("projectOp unhandled class ${expr.javaClass}"); null
            }
        }
    }

    private fun createNewArrayExpr(expr: NewArrayExpr): TypeRefVertex {
        val newArrayExprVertices = mutableListOf<PlumeVertex>()
        val typeRef = TypeRefVertex(
                typeFullName = expr.type.toQuotedString(),
                dynamicTypeFullName = expr.type.toQuotedString(),
                code = expr.toString(),
                argumentIndex = 0,
                lineNumber = currentLine,
                columnNumber = currentCol,
                order = order++
        ).apply { addSootToPlumeAssociation(expr, this) }
        ArrayInitializerVertex(order++).let {
            driver.addEdge(typeRef, it, EdgeLabel.AST)
            newArrayExprVertices.add(it)
        }
        addSootToPlumeAssociation(expr, newArrayExprVertices)
        return typeRef
    }

    private fun createNewExpr(expr: NewExpr): TypeRefVertex {
        return TypeRefVertex(
                typeFullName = expr.baseType.toQuotedString(),
                dynamicTypeFullName = expr.type.toQuotedString(),
                code = expr.toString(),
                argumentIndex = 0,
                lineNumber = currentLine,
                columnNumber = currentCol,
                order = order++
        ).apply { addSootToPlumeAssociation(expr, this) }
    }

    private fun projectReturnVertex(ret: ReturnStmt): ReturnVertex {
        val retV = ReturnVertex(
                code = ret.toString(),
                argumentIndex = 0,
                lineNumber = ret.javaSourceStartLineNumber,
                columnNumber = ret.javaSourceStartColumnNumber,
                order = order++
        )
        projectOp(ret.op)?.let { driver.addEdge(retV, it, EdgeLabel.AST) }
        driver.addEdge(getSootAssociation(graph.body.method)?.first { it is BlockVertex }!!, retV, EdgeLabel.AST)
        return retV
    }

    private fun projectReturnVertex(ret: ReturnVoidStmt): ReturnVertex {
        val retV = ReturnVertex(
                code = ret.toString(),
                argumentIndex = 0,
                lineNumber = ret.javaSourceStartLineNumber,
                columnNumber = ret.javaSourceStartColumnNumber,
                order = order++
        )
        driver.addEdge(getSootAssociation(graph.body.method)?.first { it is BlockVertex }!!, retV, EdgeLabel.AST)
        return retV
    }

}
