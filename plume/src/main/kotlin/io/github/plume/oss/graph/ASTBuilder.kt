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
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.ExtractorConst.ASSIGN
import io.github.plume.oss.util.ExtractorConst.BIN_OPS
import io.github.plume.oss.util.ExtractorConst.CAST
import io.github.plume.oss.util.ExtractorConst.FALSE_TARGET
import io.github.plume.oss.util.ExtractorConst.TRUE_TARGET
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.github.plume.oss.util.SootToPlumeUtil.createScalaList
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import scala.Option
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

    override fun buildMethodBody(graph: BriefUnitGraph) {
        val mtd = graph.body.method
        this.graph = graph
        logger.debug("Building AST for ${mtd.declaration}")
        // Connect and create parameters and locals
        getSootAssociation(mtd)?.let { mtdVs ->
            mtdVs.filterIsInstance<NewMethodBuilder>().firstOrNull()?.let { mtdVert ->
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
                                fromV = getSootAssociation(mtd)!!.first { v -> v is NewBlockBuilder },
                                toV = it,
                                edge = EdgeLabel.AST
                            )
                        }.onFailure { e -> logger.warn(e.message) }
                    }
            }
    }

    private fun buildLocals(graph: BriefUnitGraph, mtdVertex: NewMethodBuilder): MutableList<NewNodeBuilder> {
        val localVertices = mutableListOf<NewNodeBuilder>()
        graph.body.parameterLocals
            .mapIndexed { i, local ->
                SootToPlumeUtil.projectMethodParameterIn(local, currentLine, currentCol, i)
                    .apply { addSootToPlumeAssociation(local, this) }
            }
            .forEach {
                runCatching {
                    driver.addEdge(mtdVertex, it, EdgeLabel.AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        graph.body.locals
            .filter { !graph.body.parameterLocals.contains(it) }
            .mapIndexed { i, local ->
                SootToPlumeUtil.projectLocalVariable(local, currentLine, currentCol, i)
                    .apply { addSootToPlumeAssociation(local, this) }
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
    private fun projectUnit(unit: Unit, childIdx: Int): NewNodeBuilder? {
        currentLine = unit.javaSourceStartLineNumber
        currentCol = unit.javaSourceStartColumnNumber

        val unitVertex: NewNodeBuilder? = when (unit) {
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
     * @param unit The [InvokeExpr] from which a [NewCall] will be constructed.
     * @return the [NewCall] constructed.
     */
    private fun projectCallVertex(unit: InvokeExpr, childIdx: Int): NewNodeBuilder {
        val callVertex = NewCallBuilder()
            .name(unit.methodRef.name)
            .signature(unit.methodRef.signature)
            .code(unit.methodRef.subSignature.toString())
            .order(childIdx)
            .dynamictypehintfullname(createScalaList(unit.methodRef.returnType.toQuotedString()))
            .linenumber(Option.apply(currentLine))
            .columnnumber(Option.apply(currentCol))
            .methodfullname(unit.methodRef.toString().removeSurrounding("<", ">"))
            .argumentindex(childIdx)
            .dispatchtype(if (unit.methodRef.isStatic) DispatchType.STATIC_DISPATCH.name else DispatchType.DYNAMIC_DISPATCH.name)
            .typefullname(unit.type.toString())
        val callVertices = mutableListOf<NewNodeBuilder>(callVertex)
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
                }.onFailure { e -> logger.warn(e.message, e) }
                runCatching {
                    driver.addEdge(callVertex, this, EdgeLabel.AST)
                }.onFailure { e -> logger.warn(e.message, e) }
            }
        }
        return callVertex
    }

    /**
     * Given an [TableSwitchStmt], will construct table switch information in the graph.
     *
     * @param unit The [TableSwitchStmt] from which a [NewControlStructureBuilder] will be constructed.
     * @return the [NewControlStructureBuilder] constructed.
     */
    private fun projectTableSwitch(unit: TableSwitchStmt, childIdx: Int): NewControlStructureBuilder {
        val switchVertex = NewControlStructureBuilder()
            .code(ExtractorConst.TABLE_SWITCH)
            .linenumber(Option.apply(currentLine))
            .columnnumber(Option.apply(currentCol))
            .order(childIdx)
            .argumentindex(childIdx)
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) {
                val tgtV = NewJumpTargetBuilder()
                    .name("CASE $i")
                    .argumentindex(i)
                    .linenumber(Option.apply(tgt.javaSourceStartLineNumber))
                    .columnnumber(Option.apply(tgt.javaSourceStartColumnNumber))
                    .code(tgt.toString())
                    .order(childIdx)
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
     * @param unit The [LookupSwitchStmt] from which a [NewControlStructureBuilder] will be constructed.
     * @return the [NewControlStructureBuilder] constructed.
     */
    private fun projectLookupSwitch(unit: LookupSwitchStmt, childIdx: Int): NewControlStructureBuilder {
        val switchVertex = NewControlStructureBuilder()
            .code(ExtractorConst.LOOKUP_ROOT)
            .linenumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnnumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentindex(childIdx)
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            if (unit.defaultTarget != tgt) {
                val lookupValue = unit.getLookupValue(i)
                val tgtV = NewJumpTargetBuilder()
                    .name("CASE $lookupValue")
                    .argumentindex(lookupValue)
                    .linenumber(Option.apply(tgt.javaSourceStartLineNumber))
                    .columnnumber(Option.apply(tgt.javaSourceStartColumnNumber))
                    .code(tgt.toString())
                    .order(childIdx)
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
     * @param unit The [LookupSwitchStmt] from which a [NewControlStructureBuilder] will be constructed.
     * @param switchVertex The [NewControlStructureBuilder] representing the switch statement to link.
     */
    private fun projectSwitchDefault(unit: SwitchStmt, switchVertex: NewControlStructureBuilder) {
        val totalTgts = unit.targets.size
        projectOp(unit.key, totalTgts + 1)?.let { driver.addEdge(switchVertex, it, EdgeLabel.CONDITION) }
        // Handle default target jump
        unit.defaultTarget.let {
            val tgtV = NewJumpTargetBuilder()
                .name("DEFAULT")
                .argumentindex(totalTgts + 2)
                .linenumber(Option.apply(it.javaSourceStartLineNumber))
                .columnnumber(Option.apply(it.javaSourceStartColumnNumber))
                .code(it.toString())
                .order(totalTgts + 2)
            runCatching {
                driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
            }.onFailure { e -> logger.warn(e.message) }
            addSootToPlumeAssociation(unit, tgtV)
        }
    }

    /**
     * Given an [IfStmt], will construct if statement information in the graph.
     *
     * @param unit The [IfStmt] from which a [NewControlStructureBuilder] will be constructed.
     * @return the [NewControlStructureBuilder] constructed.
     */
    private fun projectIfStatement(unit: IfStmt, childIdx: Int): NewControlStructureBuilder {
        val ifRootVertex = projectIfRootAndCondition(unit, childIdx)
        graph.getSuccsOf(unit).forEach {
            val condBody: NewJumpTargetBuilder = if (it == unit.target) {
                NewJumpTargetBuilder()
                    .name(FALSE_TARGET)
                    .argumentindex(0)
                    .linenumber(Option.apply(it.javaSourceStartLineNumber))
                    .columnnumber(Option.apply(it.javaSourceStartColumnNumber))
                    .code("ELSE_BODY")
                    .order(childIdx)
            } else {
                NewJumpTargetBuilder()
                    .name(TRUE_TARGET)
                    .argumentindex(1)
                    .linenumber(Option.apply(it.javaSourceStartLineNumber))
                    .columnnumber(Option.apply(it.javaSourceStartColumnNumber))
                    .code("IF_BODY")
                    .order(childIdx)
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
     * @param unit The [IfStmt] from which a [NewControlStructureBuilder] and condition [NewBlock] will be constructed.
     * @return the [NewControlStructureBuilder] constructed.
     */
    private fun projectIfRootAndCondition(unit: IfStmt, childIdx: Int): NewControlStructureBuilder {
        val ifRootVertex = NewControlStructureBuilder()
            .code(ExtractorConst.IF_ROOT)
            .linenumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnnumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentindex(childIdx)
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
     * @param unit The [AssignStmt] from which a [NewCallBuilder] and its children vertices will be constructed.
     * @return the [NewCallBuilder] constructed.
     */
    private fun projectVariableAssignment(unit: DefinitionStmt, childIdx: Int): NewCallBuilder {
        val assignVariables = mutableListOf<NewNodeBuilder>()
        val leftOp = unit.leftOp
        val rightOp = unit.rightOp
        val assignBlock = NewCallBuilder()
            .name(ASSIGN)
            .code("=")
            .signature("${leftOp.type} = ${rightOp.type}")
            .methodfullname("=")
            .dispatchtype(DispatchType.STATIC_DISPATCH.name)
            .dynamictypehintfullname(createScalaList(unit.rightOp.type.toQuotedString()))
            .order(childIdx)
            .argumentindex(childIdx)
            .typefullname(leftOp.type.toQuotedString())
            .linenumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnnumber(Option.apply(unit.javaSourceStartColumnNumber))
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
     * Given an [BinopExpr], will construct the root operand as a [NewCallBuilder] and left and right operations of the
     * binary operation.
     *
     * @param expr The [BinopExpr] from which a [NewCallBuilder] and its children vertices will be constructed.
     * @return the [NewCallBuilder] constructed.
     */
    private fun projectBinopExpr(expr: BinopExpr, childIdx: Int): NewCallBuilder {
        val binopVertices = mutableListOf<NewNodeBuilder>()
        val binOpExpr = BIN_OPS[expr.symbol.trim()] ?: throw Exception("Unknown binary operator $expr")
        val binOpBlock = NewCallBuilder()
            .name(binOpExpr)
            .code(expr.symbol.trim())
            .signature("${expr.op1.type.toQuotedString()}${expr.symbol}${expr.op2.type.toQuotedString()}")
            .methodfullname(expr.symbol.trim())
            .dispatchtype(DispatchType.STATIC_DISPATCH.name)
            .dynamictypehintfullname(createScalaList(expr.op2.type.toQuotedString()))
            .order(childIdx)
            .argumentindex(childIdx)
            .typefullname(expr.type.toQuotedString())
            .linenumber(Option.apply(currentLine))
            .columnnumber(Option.apply(currentCol))
            .apply { binopVertices.add(this) }
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

    private fun projectFlippedConditionalExpr(expr: ConditionExpr): NewCallBuilder {
        val conditionVertices = mutableListOf<NewNodeBuilder>()
        val operator = SootParserUtil.parseAndFlipEquality(expr.symbol.trim())
        val symbol = BIN_OPS.filter { it.value == operator }.keys.first()
        val binOpBlock = NewCallBuilder()
            .name(operator)
            .code(symbol)
            .signature("${expr.op1.type} $symbol ${expr.op2.type}")
            .methodfullname(symbol)
            .dispatchtype(DispatchType.STATIC_DISPATCH.name)
            .order(2)
            .argumentindex(2) // under an if-condition, the condition child will be after the two paths
            .typefullname(expr.type.toQuotedString())
            .linenumber(Option.apply(currentLine))
            .columnnumber(Option.apply(currentCol))
            .dynamictypehintfullname(createScalaList(expr.op2.type.toQuotedString()))
            .apply { conditionVertices.add(this) }
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

    private fun projectCastExpr(expr: CastExpr, childIdx: Int): NewCallBuilder {
        val castVertices = mutableListOf<NewNodeBuilder>()
        val castBlock = NewCallBuilder()
            .name(CAST)
            .code("(${expr.castType.toQuotedString()})")
            .signature("(${expr.castType.toQuotedString()}) ${expr.op.type.toQuotedString()}")
            .methodfullname("(${expr.castType.toQuotedString()})")
            .dispatchtype(DispatchType.STATIC_DISPATCH.name)
            .dynamictypehintfullname(createScalaList(expr.op.type.toQuotedString()))
            .order(childIdx)
            .argumentindex(childIdx)
            .typefullname(expr.type.toQuotedString())
            .linenumber(Option.apply(currentLine))
            .columnnumber(Option.apply(currentCol))
            .apply { castVertices.add(this) }
        projectOp(expr.op, 0)?.let {
            runCatching {
                driver.addEdge(castBlock, it, EdgeLabel.AST); castVertices.add(it)
            }.onFailure { e -> logger.warn(e.message) }
        }
        // Save PDG arguments
        addSootToPlumeAssociation(expr, castVertices)
        return castBlock
    }

    private fun projectOp(expr: Value, childIdx: Int): NewNodeBuilder? {
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
            is NewExpr -> SootToPlumeUtil.createNewExpr(expr, currentLine, currentCol, childIdx)
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

    private fun createNewArrayExpr(expr: NewArrayExpr, childIdx: Int = 0): NewTypeRefBuilder {
        val newArrayExprVertices = mutableListOf<NewNodeBuilder>()
        val typeRef = NewTypeRefBuilder()
            .typefullname(expr.type.toQuotedString())
            .code(expr.toString())
            .argumentindex(childIdx)
            .order(childIdx)
            .linenumber(Option.apply(currentLine))
            .columnnumber(Option.apply(currentCol))
            .apply { addSootToPlumeAssociation(expr, this) }
        NewArrayInitializerBuilder()
            .order(childIdx)
            .let {
                runCatching {
                    driver.addEdge(typeRef, it, EdgeLabel.AST)
                }.onFailure { e -> logger.warn(e.message) }
                newArrayExprVertices.add(it)
            }
        addSootToPlumeAssociation(expr, newArrayExprVertices)
        return typeRef
    }

    private fun projectReturnVertex(ret: ReturnStmt, childIdx: Int): NewReturnBuilder {
        val retV = NewReturnBuilder()
            .code(ret.toString())
            .argumentindex(childIdx)
            .linenumber(Option.apply(ret.javaSourceStartLineNumber))
            .columnnumber(Option.apply(ret.javaSourceStartColumnNumber))
            .order(childIdx)
        projectOp(ret.op, childIdx + 1)?.let { driver.addEdge(retV, it, EdgeLabel.AST) }
        runCatching {
            driver.addEdge(getSootAssociation(graph.body.method)?.first { it is NewBlockBuilder }!!, retV, EdgeLabel.AST)
        }.onFailure { e -> logger.warn(e.message) }
        return retV
    }

    private fun projectReturnVertex(ret: ReturnVoidStmt, childIdx: Int): NewReturnBuilder {
        val retV = NewReturnBuilder()
            .code(ret.toString())
            .argumentindex(childIdx)
            .linenumber(Option.apply(ret.javaSourceStartLineNumber))
            .columnnumber(Option.apply(ret.javaSourceStartColumnNumber))
            .order(childIdx)
        runCatching {
            driver.addEdge(getSootAssociation(graph.body.method)?.first { it is NewBlockBuilder }!!, retV, EdgeLabel.AST)
        }.onFailure { e -> logger.warn(e.message) }
        return retV
    }

}
