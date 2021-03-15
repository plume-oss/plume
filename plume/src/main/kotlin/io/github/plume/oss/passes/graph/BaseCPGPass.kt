package io.github.plume.oss.passes.graph

import io.github.plume.oss.GlobalCache
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.util.ExtractorConst
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes
import io.shiftleft.codepropertygraph.generated.DispatchTypes
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import scala.Option
import soot.Local
import soot.Unit
import soot.Value
import soot.jimple.*
import soot.jimple.internal.JimpleLocalBox
import soot.toolkits.graph.BriefUnitGraph

class BaseCPGPass(private val g: BriefUnitGraph) {

    private val logger = LogManager.getLogger(BaseCPGPass::javaClass)
    private val builder = DeltaGraph.Builder()
    private var currentLine = -1
    private var currentCol = -1
    private val sootCache = mutableMapOf<Any, List<NewNodeBuilder>>()

    fun runPass(): DeltaGraph {
        runAstPass()
        runCfgPass()
        runPdgPass()
        return builder.build()
    }

    private fun runAstPass() {
        val mtd = g.body.method
        logger.debug("Building AST for ${mtd.declaration}")
        GlobalCache.getSootAssoc(mtd)?.let { mtdVs ->
            mtdVs.filterIsInstance<NewMethodBuilder>().firstOrNull()?.let { mtdVert ->
                GlobalCache.addSootAssoc(mtd, buildLocals(g, mtdVert))
            }
        }
        g.body.units.filterNot { it is IdentityStmt }
            .forEachIndexed { idx, u ->
                projectUnitAsAst(u, idx + 1)
                    ?.let {
                        runCatching {
                            builder.addEdge(
                                src = GlobalCache.getSootAssoc(mtd)!!.first { v -> v is NewBlockBuilder },
                                tgt = it,
                                e = AST
                            )
                        }.onFailure { e ->
                            logger.warn(
                                "Exception while adding AST edge between ${mtd.name} block and $it. " +
                                        "Details: ${e.message}."
                            )
                        }
                    }
            }
    }

    private fun runCfgPass() {
        val mtd = g.body.method
        logger.debug("Building CFG for ${mtd.declaration}")
        // Connect entrypoint to the first CFG vertex
        this.g.heads.forEach { head ->
            // Select appropriate successor to start CFG chain at
            var startingUnit = head
            while (startingUnit is IdentityStmt) startingUnit = g.getSuccsOf(startingUnit).firstOrNull() ?: break
            startingUnit?.let {
                GlobalCache.getSootAssoc(it)?.firstOrNull()?.let { succVert ->
                    val mtdV = GlobalCache.getSootAssoc(mtd)
                    val bodyVertex = mtdV?.first { mtdVertices -> mtdVertices is NewBlockBuilder }!!
                    mtdV.firstOrNull()?.let { mtdVertex -> builder.addEdge(mtdVertex, bodyVertex, CFG) }
                    runCatching {
                        builder.addEdge(bodyVertex, succVert, CFG)
                    }.onFailure { e -> logger.warn(e.message) }
                }
            }
        }
        // Connect all units to their successors
        this.g.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnitAsCfg)
    }

    private fun runPdgPass() {
        val mtd = g.body.method
        logger.debug("Building PDG for ${mtd.declaration}")
        // Identifier REF edges
        (this.g.body.parameterLocals + this.g.body.locals).forEach(this::projectLocalVariable)
        // Operator and Cast ARGUMENT edges
//        this.g.body.units.filterIsInstance<AssignStmt>().map { projectCallArg(it); it.rightOp }.forEach {
//            when (it) {
//                is CastExpr -> projectCallArg(it)
//                is BinopExpr -> projectCallArg(it)
//                is InvokeExpr -> projectCallArg(it)
//            }
//        }
        // Control structure condition vertex ARGUMENT edges
        this.g.body.units.filterIsInstance<IfStmt>().map { it.condition }.forEach(this::projectCallArg)
        // Invoke ARGUMENT edges
        this.g.body.units
            .filterIsInstance<InvokeStmt>()
            .map { it.invokeExpr as InvokeExpr }
            .forEach(this::projectCallArg)
    }

    private fun projectCallArg(value: Any) {
        GlobalCache.getSootAssoc(value)?.firstOrNull { it is NewCallBuilder }?.let { src ->
            GlobalCache.getSootAssoc(value)?.filterNot { it == src }
                ?.forEach { tgt ->
                    runCatching {
                        builder.addEdge(src, tgt, ARGUMENT)
                    }.onFailure { e -> logger.warn(e.message) }
                }
        }
    }

    private fun projectLocalVariable(local: Local) {
        GlobalCache.getSootAssoc(local)?.let { assocVertices ->
            assocVertices.filterIsInstance<NewIdentifierBuilder>().forEach { identifierV ->
                assocVertices.firstOrNull { it is NewLocalBuilder || it is NewMethodParameterInBuilder }?.let { src ->
                    runCatching {
                        builder.addEdge(identifierV, src, REF)
                    }.onFailure { e -> logger.warn(e.message) }
                }
            }
        }
    }

    private fun projectUnitAsCfg(unit: Unit) {
        when (unit) {
            is GotoStmt -> projectUnitAsCfg(unit.target)
            is IfStmt -> projectIfStatement(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is ReturnStmt -> projectReturnEdge(unit)
            is ReturnVoidStmt -> projectReturnEdge(unit)
            is ThisRef -> Unit
            is IdentityRef -> Unit
            else -> {
                val sourceUnit = if (unit is GotoStmt) unit.target else unit
                val sourceVertex = GlobalCache.getSootAssoc(sourceUnit)?.firstOrNull()
                g.getSuccsOf(sourceUnit).forEach {
                    val targetUnit = if (it is GotoStmt) it.target else it
                    if (sourceVertex != null) {
                        GlobalCache.getSootAssoc(targetUnit)?.let { vList ->
                            runCatching {
                                builder.addEdge(sourceVertex, vList.first(), CFG)
                            }.onFailure { e -> logger.warn(e.message) }
                        }
                    }
                }
            }
        }
    }

    private fun projectTableSwitch(unit: TableSwitchStmt) {
        val switchVertices = GlobalCache.getSootAssoc(unit)!!
        val switchVertex = switchVertices.first { it is NewControlStructureBuilder } as NewControlStructureBuilder
        // Handle default target jump
        projectSwitchDefault(unit, switchVertices, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) projectSwitchTarget(switchVertices, i, switchVertex, tgt)
        }
    }

    private fun projectLookupSwitch(unit: LookupSwitchStmt) {
        val lookupVertices = GlobalCache.getSootAssoc(unit)!!
        val lookupVertex = lookupVertices.first { it is NewControlStructureBuilder } as NewControlStructureBuilder
        // Handle default target jump
        projectSwitchDefault(unit, lookupVertices, lookupVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            val lookupValue = unit.getLookupValue(i)
            if (unit.defaultTarget != tgt) projectSwitchTarget(lookupVertices, lookupValue, lookupVertex, tgt)
        }
    }

    private fun projectSwitchTarget(
        lookupVertices: List<NewNodeBuilder>,
        lookupValue: Int,
        lookupVertex: NewControlStructureBuilder,
        tgt: Unit
    ) {
        val tgtV = lookupVertices.first { it is NewJumpTargetBuilder && it.build().argumentIndex() == lookupValue }
        projectTargetPath(lookupVertex, tgtV, tgt)
    }

    private fun projectSwitchDefault(
        unit: SwitchStmt,
        switchVertices: List<NewNodeBuilder>,
        switchVertex: NewControlStructureBuilder
    ) {
        unit.defaultTarget.let { defaultUnit ->
            val tgtV = switchVertices.first { it is NewJumpTargetBuilder && it.build().name() == "DEFAULT" }
            projectTargetPath(switchVertex, tgtV, defaultUnit)
        }
    }

    private fun projectTargetPath(
        lookupVertex: NewControlStructureBuilder,
        tgtV: NewNodeBuilder,
        tgt: Unit
    ) {
        runCatching {
            builder.addEdge(lookupVertex, tgtV, CFG)
        }.onFailure { e -> logger.warn(e.message) }
        GlobalCache.getSootAssoc(tgt)?.let { vList ->
            runCatching {
                builder.addEdge(tgtV, vList.first(), CFG)
            }.onFailure { e -> logger.warn(e.message) }
        }
    }

    private fun projectIfStatement(unit: IfStmt) {
        val ifVertices = GlobalCache.getSootAssoc(unit)!!
        g.getSuccsOf(unit).forEach {
            val srcVertex = if (it == unit.target) {
                ifVertices.first { vert ->
                    vert is NewJumpTargetBuilder && vert.build().name() == ExtractorConst.FALSE_TARGET
                }
            } else {
                ifVertices.first { vert ->
                    vert is NewJumpTargetBuilder && vert.build().name() == ExtractorConst.TRUE_TARGET
                }
            }
            val tgtVertices = if (it is GotoStmt) GlobalCache.getSootAssoc(it.target)
            else GlobalCache.getSootAssoc(it)
            tgtVertices?.let { vList ->
                runCatching {
                    builder.addEdge(ifVertices.first(), srcVertex, CFG)
                }.onFailure { e -> logger.warn(e.message) }
                runCatching {
                    builder.addEdge(srcVertex, vList.first(), CFG)
                }.onFailure { e -> logger.warn(e.message) }
            }
        }
    }

    private fun projectReturnEdge(unit: Stmt) {
        GlobalCache.getSootAssoc(unit)?.firstOrNull()?.let { src ->
            GlobalCache.getSootAssoc(g.body.method)?.filterIsInstance<NewMethodReturnBuilder>()?.firstOrNull()
                ?.let { tgt ->
                    runCatching {
                        builder.addEdge(src, tgt, CFG)
                    }.onFailure { e -> logger.warn(e.message) }
                }
        }
    }

    private fun buildLocals(graph: BriefUnitGraph, mtdVertex: NewMethodBuilder): MutableList<NewNodeBuilder> {
        val localVertices = mutableListOf<NewNodeBuilder>()
        graph.body.parameterLocals
            .mapIndexed { i, local ->
                SootToPlumeUtil.projectMethodParameterIn(local, currentLine, currentCol, i + 1)
                    .apply {
                        GlobalCache.addSootAssoc(local, this)
                    }
            }
            .forEach {
                runCatching {
                    builder.addEdge(mtdVertex, it, AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        graph.body.locals
            .filter { !graph.body.parameterLocals.contains(it) }
            .mapIndexed { i, local ->
                SootToPlumeUtil.projectLocalVariable(local, currentLine, currentCol, i)
                    .apply { GlobalCache.addSootAssoc(local, this) }
            }
            .forEach {
                runCatching {
                    builder.addEdge(mtdVertex, it, AST); localVertices.add(it)
                }.onFailure { e -> logger.warn(e.message) }
            }
        return localVertices
    }

    /**
     * Given a unit, will construct AST information in the graph.
     *
     * @param unit The [Unit] from which AST vertices and edges will be constructed.
     */
    private fun projectUnitAsAst(unit: Unit, childIdx: Int): NewNodeBuilder? {
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
        return unitVertex?.apply { if (this !is InvokeStmt) GlobalCache.addSootAssoc(unit, this, 0) }
    }

    /**
     * Given an [InvokeExpr], will construct Call information in the graph.
     *
     * @param unit The [InvokeExpr] from which a [NewCall] will be constructed.
     * @return the [NewCall] constructed.
     */
    private fun projectCallVertex(unit: InvokeExpr, childIdx: Int): NewNodeBuilder {
        val signature = "${unit.type}(${unit.methodRef.parameterTypes.joinToString(separator = ",")})"
        val code = "${unit.methodRef.name}(${unit.args.joinToString(separator = ", ")})"
        val callVertex = NewCallBuilder()
            .name(unit.methodRef.name)
            .methodFullName("${unit.methodRef.declaringClass}.${unit.methodRef.name}:$signature")
            .signature(signature)
            .code(code)
            .order(childIdx)
            .dynamicTypeHintFullName(SootToPlumeUtil.createScalaList(unit.methodRef.returnType.toQuotedString()))
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .argumentIndex(childIdx)
            .dispatchType(if (unit.methodRef.isStatic) DispatchTypes.STATIC_DISPATCH else DispatchTypes.DYNAMIC_DISPATCH)
            .typeFullName(unit.type.toString())
        val callVertices = mutableListOf<NewNodeBuilder>(callVertex)
        // Create vertices for arguments
        unit.args.forEachIndexed { i, arg ->
            when (arg) {
                is Local -> SootToPlumeUtil.createIdentifierVertex(arg, currentLine, currentCol, i + 1)
                is Constant -> SootToPlumeUtil.createLiteralVertex(arg, currentLine, currentCol, i + 1)
                else -> null
            }?.let { expressionVertex ->
                runCatching {
                    builder.addEdge(callVertex, expressionVertex, AST)
                    builder.addEdge(callVertex, expressionVertex, ARGUMENT)
                }.onFailure { e -> logger.warn(e.message) }
                callVertices.add(expressionVertex)
                GlobalCache.addSootAssoc(arg, expressionVertex)
            }
        }
        // Save PDG arguments
        GlobalCache.addSootAssoc(unit, callVertices)
        // Create the receiver for the call
        unit.useBoxes.filterIsInstance<JimpleLocalBox>().firstOrNull()?.let {
            SootToPlumeUtil.createIdentifierVertex(it.value, currentLine, currentCol, unit.useBoxes.indexOf(it)).apply {
                GlobalCache.addSootAssoc(it.value, this)
                runCatching {
                    builder.addEdge(callVertex, this, RECEIVER)
                }.onFailure { e -> logger.warn(e.message, e) }
                runCatching {
                    builder.addEdge(callVertex, this, AST)
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
            .controlStructureType(ControlStructureTypes.SWITCH)
            .code(unit.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)
            .argumentIndex(childIdx)
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) {
                val tgtV = NewJumpTargetBuilder()
                    .name("CASE $i")
                    .argumentIndex(i)
                    .lineNumber(Option.apply(tgt.javaSourceStartLineNumber))
                    .columnNumber(Option.apply(tgt.javaSourceStartColumnNumber))
                    .code(tgt.toString())
                    .order(childIdx)
                runCatching {
                    builder.addEdge(switchVertex, tgtV, AST)
                }.onFailure { e -> logger.warn(e.message) }
                GlobalCache.addSootAssoc(unit, tgtV)
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
            .controlStructureType(ControlStructureTypes.SWITCH)
            .code(unit.toString())
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentIndex(childIdx)
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            if (unit.defaultTarget != tgt) {
                val lookupValue = unit.getLookupValue(i)
                val tgtV = NewJumpTargetBuilder()
                    .name("CASE $lookupValue")
                    .argumentIndex(lookupValue)
                    .lineNumber(Option.apply(tgt.javaSourceStartLineNumber))
                    .columnNumber(Option.apply(tgt.javaSourceStartColumnNumber))
                    .code(tgt.toString())
                    .order(childIdx)
                runCatching {
                    builder.addEdge(switchVertex, tgtV, AST)
                }.onFailure { e -> logger.warn(e.message) }
                GlobalCache.addSootAssoc(unit, tgtV)
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
        projectOp(unit.key, totalTgts + 1)?.let { builder.addEdge(switchVertex, it, CONDITION) }
        // Handle default target jump
        unit.defaultTarget.let {
            val tgtV = NewJumpTargetBuilder()
                .name("DEFAULT")
                .argumentIndex(totalTgts + 2)
                .lineNumber(Option.apply(it.javaSourceStartLineNumber))
                .columnNumber(Option.apply(it.javaSourceStartColumnNumber))
                .code(it.toString())
                .order(totalTgts + 2)
            runCatching {
                builder.addEdge(switchVertex, tgtV, AST)
            }.onFailure { e -> logger.warn(e.message) }
            GlobalCache.addSootAssoc(unit, tgtV)
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
        g.getSuccsOf(unit).forEach {
            val condBody: NewJumpTargetBuilder = if (it == unit.target) {
                NewJumpTargetBuilder()
                    .name(ExtractorConst.FALSE_TARGET)
                    .argumentIndex(0)
                    .lineNumber(Option.apply(it.javaSourceStartLineNumber))
                    .columnNumber(Option.apply(it.javaSourceStartColumnNumber))
                    .code("ELSE_BODY")
                    .order(childIdx)
            } else {
                NewJumpTargetBuilder()
                    .name(ExtractorConst.TRUE_TARGET)
                    .argumentIndex(1)
                    .lineNumber(Option.apply(it.javaSourceStartLineNumber))
                    .columnNumber(Option.apply(it.javaSourceStartColumnNumber))
                    .code("IF_BODY")
                    .order(childIdx)
            }
            runCatching {
                builder.addEdge(ifRootVertex, condBody, AST)
            }.onFailure { e -> logger.warn(e.message) }
            GlobalCache.addSootAssoc(unit, condBody)
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
            .controlStructureType(ControlStructureTypes.IF)
            .code(unit.toString())
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentIndex(childIdx)
        builder.addVertex(ifRootVertex)
        val condition = unit.condition as ConditionExpr
        val conditionExpr = projectFlippedConditionalExpr(condition)
        runCatching {
            builder.addEdge(ifRootVertex, conditionExpr, CONDITION)
        }.onFailure { e -> logger.warn(e.message) }
        GlobalCache.addSootAssoc(unit, conditionExpr)
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
            .name(Operators.assignment)
            .code(unit.toString())
            .signature("${leftOp.type} = ${rightOp.type}")
            .methodFullName(Operators.assignment)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(SootToPlumeUtil.createScalaList(unit.rightOp.type.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(leftOp.type.toQuotedString())
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
        when (leftOp) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(leftOp, currentLine, currentCol, 0).apply {
                GlobalCache.addSootAssoc(leftOp, this)
            }
            is FieldRef -> SootToPlumeUtil.createFieldIdentifierVertex(leftOp, currentLine, currentCol, 0)
                .apply {
                    GlobalCache.addSootAssoc(leftOp.field, this)
                }
            is ArrayRef -> SootToPlumeUtil.createArrayRefIdentifier(leftOp, currentLine, currentCol, 0)
                .apply {
                    GlobalCache.addSootAssoc(leftOp.base, this)
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
                builder.addEdge(assignBlock, it, AST)
                builder.addEdge(assignBlock, it, ARGUMENT)
            }.onFailure { e -> logger.warn(e.message) }
            assignVariables.add(it)
            GlobalCache.addSootAssoc(leftOp, it)
        }
        projectOp(rightOp, 1)?.let {
            runCatching {
                builder.addEdge(assignBlock, it, AST)
                builder.addEdge(assignBlock, it, ARGUMENT)
            }.onFailure { e -> logger.warn(e.message) }
            assignVariables.add(it)
            GlobalCache.addSootAssoc(rightOp, it)
        }
        // Save PDG arguments
        GlobalCache.addSootAssoc(unit, assignVariables)
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
        val binOpExpr = SootToPlumeUtil.parseBinopExpr(expr)
        val binOpBlock = NewCallBuilder()
            .name(binOpExpr)
            .code(expr.toString())
            .signature("${expr.op1.type.toQuotedString()}${expr.symbol}${expr.op2.type.toQuotedString()}")
            .methodFullName(binOpExpr)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(SootToPlumeUtil.createScalaList(expr.op2.type.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(expr.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { binopVertices.add(this) }
        projectOp(expr.op1, 0)?.let {
            runCatching {
                builder.addEdge(binOpBlock, it, AST)
                builder.addEdge(binOpBlock, it, ARGUMENT)
            }.onFailure { e -> logger.warn(e.message) }
            binopVertices.add(it)
            GlobalCache.addSootAssoc(expr.op1, it)
        }
        projectOp(expr.op2, 1)?.let {
            runCatching {
                builder.addEdge(binOpBlock, it, AST)
                builder.addEdge(binOpBlock, it, ARGUMENT)
            }.onFailure { e -> logger.warn(e.message) }
            binopVertices.add(it)
            GlobalCache.addSootAssoc(expr.op2, it)
        }
        // Save PDG arguments
        GlobalCache.addSootAssoc(expr, binopVertices)
        return binOpBlock
    }

    private fun projectFlippedConditionalExpr(expr: ConditionExpr): NewCallBuilder {
        val conditionVertices = mutableListOf<NewNodeBuilder>()
        val operator = SootParserUtil.parseAndFlipEquality(expr.symbol.trim())
        val binOpBlock = NewCallBuilder()
            .name(operator)
            .code(expr.toString())
            .signature("${expr.op1.type} $operator ${expr.op2.type}")
            .methodFullName(operator)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .order(3)
            .argumentIndex(3) // under an if-condition, the condition child will be after the two paths
            .typeFullName(expr.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .dynamicTypeHintFullName(SootToPlumeUtil.createScalaList(expr.op2.type.toQuotedString()))
            .apply { conditionVertices.add(this) }
        projectOp(expr.op1, 1)?.let {
            runCatching {
                builder.addEdge(binOpBlock, it, AST)
                builder.addEdge(binOpBlock, it, ARGUMENT)
            }.onFailure { e -> logger.warn(e.message) }
            conditionVertices.add(it)
            GlobalCache.addSootAssoc(expr.op1, it)
        }
        projectOp(expr.op2, 2)?.let {
            runCatching {
                builder.addEdge(binOpBlock, it, AST)
                builder.addEdge(binOpBlock, it, ARGUMENT)
            }.onFailure { e -> logger.warn(e.message) }
            conditionVertices.add(it)
            GlobalCache.addSootAssoc(expr.op2, it)
        }
        GlobalCache.addSootAssoc(expr, conditionVertices)
        return binOpBlock
    }

    private fun projectCastExpr(expr: CastExpr, childIdx: Int): NewCallBuilder {
        val castVertices = mutableListOf<NewNodeBuilder>()
        val castBlock = NewCallBuilder()
            .name(Operators.cast)
            .code(expr.toString())
            .signature("(${expr.castType.toQuotedString()}) ${expr.op.type.toQuotedString()}")
            .methodFullName(Operators.cast)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(SootToPlumeUtil.createScalaList(expr.op.type.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(expr.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { castVertices.add(this) }
        projectOp(expr.op, 1)?.let {
            runCatching {
                builder.addEdge(castBlock, it, AST)
                builder.addEdge(castBlock, it, ARGUMENT)
                castVertices.add(it)
            }.onFailure { e -> logger.warn(e.message) }
        }
        // Save PDG arguments
        GlobalCache.addSootAssoc(expr, castVertices)
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

    private fun createNewArrayExpr(expr: NewArrayExpr, childIdx: Int = 0) =
        NewIdentifierBuilder()
            .order(childIdx + 1)
            .argumentIndex(childIdx + 1)
            .code(expr.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { GlobalCache.addSootAssoc(expr, this) }


    private fun projectReturnVertex(ret: ReturnStmt, childIdx: Int): NewReturnBuilder {
        val retV = NewReturnBuilder()
            .code(ret.toString())
            .argumentIndex(childIdx)
            .lineNumber(Option.apply(ret.javaSourceStartLineNumber))
            .columnNumber(Option.apply(ret.javaSourceStartColumnNumber))
            .order(childIdx)
        projectOp(ret.op, childIdx + 1)?.let { builder.addEdge(retV, it, AST) }
        runCatching {
            builder.addEdge(
                GlobalCache.getSootAssoc(g.body.method)?.first { it is NewBlockBuilder }!!,
                retV,
                AST
            )
        }.onFailure { e -> logger.warn(e.message) }
        return retV
    }

    private fun projectReturnVertex(ret: ReturnVoidStmt, childIdx: Int): NewReturnBuilder {
        val retV = NewReturnBuilder()
            .code(ret.toString())
            .argumentIndex(childIdx)
            .lineNumber(Option.apply(ret.javaSourceStartLineNumber))
            .columnNumber(Option.apply(ret.javaSourceStartColumnNumber))
            .order(childIdx)
        runCatching {
            builder.addEdge(
                GlobalCache.getSootAssoc(g.body.method)?.first { it is NewBlockBuilder }!!,
                retV,
                AST
            )
        }.onFailure { e -> logger.warn(e.message) }
        return retV
    }
}