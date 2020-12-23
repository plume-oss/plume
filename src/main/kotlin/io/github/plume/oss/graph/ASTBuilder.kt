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
package io.github.plume.oss.graph

import io.github.plume.oss.Extractor.Companion.addSootToPlumeAssociation
import io.github.plume.oss.Extractor.Companion.getSootAssociation
import io.github.plume.oss.domain.enums.DispatchType
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.PlumeVertex
import io.github.plume.oss.domain.models.vertices.*
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.ExtractorConst.ASSIGN
import io.github.plume.oss.util.ExtractorConst.BIN_OPS
import io.github.plume.oss.util.ExtractorConst.CAST
import io.github.plume.oss.util.ExtractorConst.FALSE_TARGET
import io.github.plume.oss.util.ExtractorConst.TRUE_TARGET
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import org.apache.logging.log4j.LogManager
import soot.Local
import soot.Unit
import soot.Value
import soot.jimple.*
import soot.jimple.internal.JimpleLocalBox
import soot.toolkits.graph.BriefUnitGraph

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

    override fun buildMethodBody(graph: BriefUnitGraph) {
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
            .forEachIndexed { idx, u ->
                projectUnit(u, idx)
                    ?.let {
                        runCatching {
                            driver.addEdge(
                                fromV = getSootAssociation(mtd)!!.first { v -> v is BlockVertex },
                                toV = it,
                                edge = EdgeLabel.AST
                            )
                        }.onFailure { e -> logger.warn(e.message) }
                    }
            }
    }

    private fun buildLocals(graph: BriefUnitGraph, mtdVertex: MethodVertex): MutableList<PlumeVertex> {
        val localVertices = mutableListOf<PlumeVertex>()
        graph.body.parameterLocals
            .map {
                SootToPlumeUtil.projectMethodParameterIn(it, currentLine)
                    .apply { addSootToPlumeAssociation(it, this) }
            }
            .forEach {
                runCatching {
                    driver.addEdge(mtdVertex, it, EdgeLabel.AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        graph.body.locals
            .filter { !graph.body.parameterLocals.contains(it) }
            .map {
                SootToPlumeUtil.projectLocalVariable(it, currentLine, currentCol)
                    .apply { addSootToPlumeAssociation(it, this) }
            }
            .forEach {
                runCatching {
                    driver.addEdge(mtdVertex, it, EdgeLabel.AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        return localVertices
    }


    /**
     * Given a unit, will construct AST information in the graph.
     *
     * @param unit The [Unit] from which AST vertices and edges will be constructed.
     */
    private fun projectUnit(unit: Unit, childIdx: Int): PlumeVertex? {
        currentLine = unit.javaSourceStartLineNumber
        currentCol = unit.javaSourceStartColumnNumber

        val unitVertex: PlumeVertex? = when (unit) {
            is IfStmt -> projectIfStatement(unit, childIdx)
            is AssignStmt -> projectVariableAssignment(unit, childIdx)
            is LookupSwitchStmt -> projectLookupSwitch(unit, childIdx)
            is TableSwitchStmt -> projectTableSwitch(unit, childIdx)
            is InvokeStmt -> projectCallVertex(unit.invokeExpr, childIdx)
            is ReturnStmt -> projectReturnVertex(unit, childIdx)
            is ReturnVoidStmt -> projectReturnVertex(unit, childIdx)
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
    private fun projectCallVertex(unit: InvokeExpr, childIdx: Int): PlumeVertex {
        val callVertex = CallVertex(
            name = unit.methodRef.name,
            signature = unit.methodRef.signature,
            code = unit.methodRef.subSignature.toString(),
            order = order++,
            lineNumber = currentLine,
            columnNumber = currentCol,
            methodFullName = unit.methodRef.toString().removeSurrounding("<", ">"),
            argumentIndex = childIdx,
            dispatchType = if (unit.methodRef.isStatic) DispatchType.STATIC_DISPATCH else DispatchType.DYNAMIC_DISPATCH,
            typeFullName = unit.type.toString(),
            dynamicTypeHintFullName = unit.type.toQuotedString()
        )
        val callVertices = mutableListOf<PlumeVertex>(callVertex)
        // Create vertices for arguments
        unit.args.forEachIndexed { i, arg ->
            when (arg) {
                is Local -> SootToPlumeUtil.createIdentifierVertex(arg, currentLine, currentCol, i)
                is Constant -> SootToPlumeUtil.createLiteralVertex(arg, currentLine, currentCol, i)
                else -> null
            }?.let { expressionVertex ->
                runCatching {
                    driver.addEdge(callVertex, expressionVertex, EdgeLabel.AST)
                }.onFailure { e -> logger.warn(e.message) }
                callVertices.add(expressionVertex)
                addSootToPlumeAssociation(arg, expressionVertex)
            }
        }
        // Save PDG arguments
        addSootToPlumeAssociation(unit, callVertices)
        // Create the receiver for the call
        unit.useBoxes.filterIsInstance<JimpleLocalBox>().firstOrNull()?.let {
            SootToPlumeUtil.createIdentifierVertex(it.value, currentLine, currentCol, unit.useBoxes.indexOf(it)).apply {
                addSootToPlumeAssociation(it.value, this)
                runCatching {
                    driver.addEdge(callVertex, this, EdgeLabel.RECEIVER)
                }.onFailure { e -> logger.warn(e.message) }
                runCatching {
                    driver.addEdge(callVertex, this, EdgeLabel.AST)
                }.onFailure { e -> logger.warn(e.message) }
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
    private fun projectTableSwitch(unit: TableSwitchStmt, childIdx: Int): ControlStructureVertex {
        val switchVertex = ControlStructureVertex(
            code = ExtractorConst.TABLE_SWITCH,
            lineNumber = unit.javaSourceStartLineNumber,
            columnNumber = unit.javaSourceStartColumnNumber,
            order = order++,
            argumentIndex = childIdx
        )
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) {
                val tgtV = JumpTargetVertex(
                    "CASE $i",
                    i,
                    tgt.javaSourceStartLineNumber,
                    tgt.javaSourceStartColumnNumber,
                    tgt.toString(),
                    order++
                )
                runCatching {
                    driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
                }.onFailure { e -> logger.warn(e.message) }
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
    private fun projectLookupSwitch(unit: LookupSwitchStmt, childIdx: Int): ControlStructureVertex {
        val switchVertex = ControlStructureVertex(
            code = ExtractorConst.LOOKUP_ROOT,
            lineNumber = unit.javaSourceStartLineNumber,
            columnNumber = unit.javaSourceStartColumnNumber,
            order = order++,
            argumentIndex = childIdx
        )
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            if (unit.defaultTarget != tgt) {
                val lookupValue = unit.getLookupValue(i)
                val tgtV = JumpTargetVertex(
                    "CASE $lookupValue",
                    lookupValue,
                    tgt.javaSourceStartLineNumber,
                    tgt.javaSourceStartColumnNumber,
                    tgt.toString(),
                    order++
                )
                runCatching {
                    driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
                }.onFailure { e -> logger.warn(e.message) }
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
        val totalTgts = unit.targets.size
        projectOp(unit.key, totalTgts + 1)?.let { driver.addEdge(switchVertex, it, EdgeLabel.CONDITION) }
        // Handle default target jump
        unit.defaultTarget.let {
            val tgtV = JumpTargetVertex(
                "DEFAULT",
                totalTgts + 2,
                it.javaSourceStartLineNumber,
                it.javaSourceStartColumnNumber,
                it.toString(),
                order++
            )
            runCatching {
                driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            addSootToPlumeAssociation(unit, tgtV)
        }
    }

    /**
     * Given an [IfStmt], will construct if statement information in the graph.
     *
     * @param unit The [IfStmt] from which a [ControlStructureVertex] will be constructed.
     * @return the [ControlStructureVertex] constructed.
     */
    private fun projectIfStatement(unit: IfStmt, childIdx: Int): ControlStructureVertex {
        val ifRootVertex = projectIfRootAndCondition(unit, childIdx)
        graph.getSuccsOf(unit).forEach {
            val condBody: JumpTargetVertex = if (it == unit.target) {
                JumpTargetVertex(
                    FALSE_TARGET,
                    0,
                    it.javaSourceStartLineNumber,
                    it.javaSourceStartColumnNumber,
                    "ELSE_BODY",
                    order++
                )
            } else {
                JumpTargetVertex(
                    TRUE_TARGET,
                    1,
                    it.javaSourceStartLineNumber,
                    it.javaSourceStartColumnNumber,
                    "IF_BODY",
                    order++
                )
            }
            runCatching {
                driver.addEdge(ifRootVertex, condBody, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
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
    private fun projectIfRootAndCondition(unit: IfStmt, childIdx: Int): ControlStructureVertex {
        val ifRootVertex = ControlStructureVertex(
            code = ExtractorConst.IF_ROOT,
            lineNumber = unit.javaSourceStartLineNumber,
            columnNumber = unit.javaSourceStartColumnNumber,
            order = order++,
            argumentIndex = childIdx
        )
        driver.addVertex(ifRootVertex)
        val condition = unit.condition as ConditionExpr
        val conditionExpr = projectFlippedConditionalExpr(condition)
        runCatching {
            driver.addEdge(ifRootVertex, conditionExpr, EdgeLabel.CONDITION)
        }.onFailure { e -> logger.warn(e.message) }
        addSootToPlumeAssociation(unit, conditionExpr)
        return ifRootVertex
    }

    /**
     * Given an [AssignStmt], will construct variable assignment edge and vertex information.
     *
     * @param unit The [AssignStmt] from which a [CallVertex] and its children vertices will be constructed.
     * @return the [CallVertex] constructed.
     */
    private fun projectVariableAssignment(unit: DefinitionStmt, childIdx: Int): CallVertex {
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
            argumentIndex = childIdx,
            typeFullName = leftOp.type.toQuotedString(),
            dynamicTypeHintFullName = rightOp.type.toQuotedString(),
            lineNumber = unit.javaSourceStartLineNumber,
            columnNumber = unit.javaSourceStartColumnNumber
        )
        when (leftOp) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(leftOp, currentLine, currentCol, 0).apply {
                addSootToPlumeAssociation(leftOp, this)
            }
            is FieldRef -> SootToPlumeUtil.createFieldIdentifierVertex(leftOp, currentLine, currentCol, 0)
                .apply {
                    addSootToPlumeAssociation(leftOp.field, this)
                }
            is ArrayRef -> SootToPlumeUtil.createArrayRefIdentifier(leftOp, currentLine, currentCol, 0)
                .apply {
                    addSootToPlumeAssociation(leftOp.base, this)
                }
            else -> {
                logger.debug(
                    "Unhandled class for leftOp under projectVariableAssignment: ${leftOp.javaClass} " +
                            "containing value $leftOp"
                )
                null
            }
        }?.let {
            runCatching {
                driver.addEdge(assignBlock, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            assignVariables.add(it)
            addSootToPlumeAssociation(leftOp, it)
        }
        projectOp(rightOp, 1)?.let {
            runCatching {
                driver.addEdge(assignBlock, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            assignVariables.add(it)
            addSootToPlumeAssociation(rightOp, it)
        }
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
    private fun projectBinopExpr(expr: BinopExpr, childIdx: Int): CallVertex {
        val binopVertices = mutableListOf<PlumeVertex>()
        val binOpExpr = BIN_OPS[expr.symbol.trim()] ?: throw Exception("Unknown binary operator $expr")
        val binOpBlock = CallVertex(
            name = binOpExpr,
            code = expr.symbol.trim(),
            signature = "${expr.op1.type.toQuotedString()}${expr.symbol}${expr.op2.type.toQuotedString()}",
            methodFullName = expr.symbol.trim(),
            dispatchType = DispatchType.STATIC_DISPATCH,
            order = order++,
            argumentIndex = childIdx,
            typeFullName = expr.type.toQuotedString(),
            dynamicTypeHintFullName = expr.type.toString(),
            lineNumber = currentLine,
            columnNumber = currentCol
        ).apply { binopVertices.add(this) }
        projectOp(expr.op1, 0)?.let {
            runCatching {
                driver.addEdge(binOpBlock, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            binopVertices.add(it)
            addSootToPlumeAssociation(expr.op1, it)
        }
        projectOp(expr.op2, 1)?.let {
            runCatching {
                driver.addEdge(binOpBlock, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            binopVertices.add(it)
            addSootToPlumeAssociation(expr.op2, it)
        }
        // Save PDG arguments
        addSootToPlumeAssociation(expr, binopVertices)
        return binOpBlock
    }

    private fun projectFlippedConditionalExpr(expr: ConditionExpr): CallVertex {
        val conditionVertices = mutableListOf<PlumeVertex>()
        val operator = SootParserUtil.parseAndFlipEquality(expr.symbol.trim())
        val symbol = BIN_OPS.filter { it.value == operator }.keys.first()
        val binOpBlock = CallVertex(
            name = operator,
            code = symbol,
            order = order++,
            argumentIndex = 2, // under an if-condition, the condition child will be after the two paths
            dispatchType = DispatchType.STATIC_DISPATCH,
            signature = "${expr.op1.type} $symbol ${expr.op2.type}",
            methodFullName = symbol,
            typeFullName = expr.type.toQuotedString(),
            dynamicTypeHintFullName = expr.type.toQuotedString(),
            lineNumber = currentLine,
            columnNumber = currentCol
        ).apply { conditionVertices.add(this) }
        projectOp(expr.op1, 0)?.let {
            runCatching {
                driver.addEdge(binOpBlock, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            conditionVertices.add(it)
            addSootToPlumeAssociation(expr.op1, it)
        }
        projectOp(expr.op2, 1)?.let {
            runCatching {
                driver.addEdge(binOpBlock, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            conditionVertices.add(it)
            addSootToPlumeAssociation(expr.op2, it)
        }
        addSootToPlumeAssociation(expr, conditionVertices)
        return binOpBlock
    }

    private fun projectCastExpr(expr: CastExpr, childIdx: Int): CallVertex {
        val castVertices = mutableListOf<PlumeVertex>()
        val castBlock = CallVertex(
            name = CAST,
            code = "(${expr.castType.toQuotedString()})",
            signature = "(${expr.castType.toQuotedString()}) ${expr.op.type.toQuotedString()}",
            dispatchType = DispatchType.STATIC_DISPATCH,
            order = order++,
            argumentIndex = childIdx,
            typeFullName = expr.castType.toQuotedString(),
            methodFullName = "(${expr.castType.toQuotedString()})",
            dynamicTypeHintFullName = expr.castType.toQuotedString(),
            lineNumber = currentLine,
            columnNumber = currentCol
        ).apply { castVertices.add(this) }
        projectOp(expr.op, 0)?.let {
            runCatching {
                driver.addEdge(castBlock, it, EdgeLabel.AST); castVertices.add(it)
            }.onFailure { e -> logger.warn(e.message) }
        }
        // Save PDG arguments
        addSootToPlumeAssociation(expr, castVertices)
        return castBlock
    }

    private fun projectOp(expr: Value, childIdx: Int): PlumeVertex? {
        return when (expr) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(expr, currentLine, currentCol, childIdx)
            is Constant -> SootToPlumeUtil.createLiteralVertex(expr, currentLine, currentCol, childIdx)
            is CastExpr -> projectCastExpr(expr, childIdx)
            is BinopExpr -> projectBinopExpr(expr, childIdx)
            is InvokeExpr -> projectCallVertex(expr, childIdx)
            is StaticFieldRef -> SootToPlumeUtil.createFieldIdentifierVertex(
                expr,
                currentLine,
                currentCol,
                childIdx
            )
            is NewExpr -> createNewExpr(expr, childIdx)
            is NewArrayExpr -> createNewArrayExpr(expr, childIdx)
            is CaughtExceptionRef -> SootToPlumeUtil.createIdentifierVertex(
                expr,
                currentLine,
                currentCol,
                childIdx
            )
            else -> {
                logger.debug("projectOp unhandled class ${expr.javaClass}"); null
            }
        }
    }

    private fun createNewArrayExpr(expr: NewArrayExpr, argumentIndex: Int = 0): TypeRefVertex {
        val newArrayExprVertices = mutableListOf<PlumeVertex>()
        val typeRef = TypeRefVertex(
            typeFullName = expr.type.toQuotedString(),
            dynamicTypeFullName = expr.type.toQuotedString(),
            code = expr.toString(),
            argumentIndex = argumentIndex,
            lineNumber = currentLine,
            columnNumber = currentCol,
            order = order++
        ).apply { addSootToPlumeAssociation(expr, this) }
        ArrayInitializerVertex(order++).let {
            runCatching {
                driver.addEdge(typeRef, it, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            newArrayExprVertices.add(it)
        }
        addSootToPlumeAssociation(expr, newArrayExprVertices)
        return typeRef
    }

    private fun createNewExpr(expr: NewExpr, childIdx: Int): TypeRefVertex {
        return TypeRefVertex(
            typeFullName = expr.baseType.toQuotedString(),
            dynamicTypeFullName = expr.type.toQuotedString(),
            code = expr.toString(),
            argumentIndex = childIdx,
            lineNumber = currentLine,
            columnNumber = currentCol,
            order = order++
        ).apply { addSootToPlumeAssociation(expr, this) }
    }

    private fun projectReturnVertex(ret: ReturnStmt, childIdx: Int): ReturnVertex {
        val retV = ReturnVertex(
            code = ret.toString(),
            argumentIndex = childIdx,
            lineNumber = ret.javaSourceStartLineNumber,
            columnNumber = ret.javaSourceStartColumnNumber,
            order = order++
        )
        projectOp(ret.op, childIdx + 1)?.let { driver.addEdge(retV, it, EdgeLabel.AST) }
        runCatching {
            driver.addEdge(getSootAssociation(graph.body.method)?.first { it is BlockVertex }!!, retV, EdgeLabel.AST)
        }.onFailure { e -> logger.warn(e.message) }
        return retV
    }

    private fun projectReturnVertex(ret: ReturnVoidStmt, childIdx: Int): ReturnVertex {
        val retV = ReturnVertex(
            code = ret.toString(),
            argumentIndex = childIdx,
            lineNumber = ret.javaSourceStartLineNumber,
            columnNumber = ret.javaSourceStartColumnNumber,
            order = order++
        )
        runCatching {
            driver.addEdge(getSootAssociation(graph.body.method)?.first { it is BlockVertex }!!, retV, EdgeLabel.AST)
        }.onFailure { e -> logger.warn(e.message) }
        return retV
    }

}
