package io.github.plume.oss.passes.graph

import io.github.plume.oss.domain.mappers.ListMapper
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.store.LocalCache
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.ControlStructureTypes
import io.shiftleft.codepropertygraph.generated.DispatchTypes
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.BY_REFERENCE
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import scala.Option
import soot.Local
import soot.Unit
import soot.Value
import soot.jimple.*
import soot.jimple.internal.JimpleLocalBox
import soot.tagkit.BytecodeOffsetTag
import soot.toolkits.graph.BriefUnitGraph

/**
 * Runs a AST, CFG, and PDG pass on the method body.
 */
class BaseCPGPass(private val g: BriefUnitGraph) {

    private val logger = LogManager.getLogger(BaseCPGPass::javaClass)
    private val builder = DeltaGraph.Builder()
    private val methodStore = mutableMapOf<Any, List<NewNodeBuilder>>()
    private val methodLocals = mutableMapOf<Value, NewLocalBuilder>()
    private lateinit var methodVertex: NewMethodBuilder
    private var currentLine = -1
    private var currentCol = -1

    private fun addToStore(e: Any, vararg ns: NewNodeBuilder, index: Int = -1) {
        if (index == -1)
            methodStore.computeIfPresent(e) { _: Any, u: List<NewNodeBuilder> -> u + ns.toList() }
        else methodStore.computeIfPresent(e) { _: Any, u: List<NewNodeBuilder> ->
            u.subList(0, index) + ns.toList() + u.subList(index, u.size)
        }
        methodStore.computeIfAbsent(e) { ns.toList() }
    }

    private fun getFromStore(e: Any): List<NewNodeBuilder> = methodStore[e] ?: emptyList()

    /**
     * Constructs a AST, CFG, PDG pass on the [BriefUnitGraph] constructed with this object. Returns the result as a
     * [DeltaGraph] object.
     */
    fun runPass(): DeltaGraph {
        val (fullName, _, _) = SootToPlumeUtil.methodToStrings(g.body.method)
        methodVertex = PlumeStorage.getMethod(fullName)!!
        runAstPass()
        runCfgPass()
        runPdgPass()
        // METHOD -CONTAINS-> NODE (excluding head nodes)
        PlumeStorage.getMethodStore(g.body.method).let { mvs ->
            mvs.firstOrNull { it is NewMethodBuilder }?.let { m ->
                methodStore.let { cache ->
                    cache.values.flatten()
                        .minus(mvs)
                        .minus(methodLocals.values)
                        .forEach { n -> builder.addEdge(m, n, CONTAINS) }
                }
                methodStore.clear()
            }
        }
        return builder.build()
    }

    private fun runAstPass() {
        val mtd = g.body.method
        logger.debug("Building AST for ${mtd.declaration}")
        currentLine = mtd.javaSourceStartLineNumber
        currentCol = mtd.javaSourceStartColumnNumber
        // METHOD -AST-> METHOD_PARAM_*
        PlumeStorage.storeMethodNode(mtd, buildParameters(g).onEach { builder.addEdge(methodVertex, it, AST) })
        // BLOCK -AST-> LOCAL
        PlumeStorage.getMethodStore(mtd).firstOrNull { v -> v is NewBlockBuilder }?.let { block ->
            buildLocals(g).onEach { builder.addEdge(block, it, AST) }
        }
        g.body.units.forEachIndexed { idx, u ->
            projectUnitAsAst(u, idx + 1)?.let {
                PlumeStorage.getMethodStore(mtd).firstOrNull { v -> v is NewBlockBuilder }?.let { block ->
                    builder.addEdge(block, it, AST)
                }
            }
        }
    }

    private fun runCfgPass() {
        val mtd = g.body.method
        logger.debug("Building CFG for ${mtd.declaration}")
        currentLine = mtd.javaSourceStartLineNumber
        currentCol = mtd.javaSourceStartColumnNumber
        // Connect all units to their successors
        val startUnits = g.body.units.filter { bu -> g.heads.map { it.toString() }.contains(bu.toString()) }
        startUnits.forEach { start ->
            getFromStore(start).firstOrNull()?.let { headNode -> builder.addEdge(methodVertex, headNode, CFG) }
        }
        this.g.body.units.forEach { unit -> projectUnitAsCfg(unit) }
    }

    private fun runPdgPass() {
        val mtd = g.body.method
        logger.debug("Building PDG for ${mtd.declaration}")
        currentLine = mtd.javaSourceStartLineNumber
        currentCol = mtd.javaSourceStartColumnNumber
        // Identifier REF edges
        g.heads.asSequence().map { it.useBoxes }.flatten().map { it.value }.forEach(this::projectLocalVariable)
        this.g.body.locals.forEach(this::projectLocalVariable)
        // Control structure condition vertex ARGUMENT edges
        this.g.body.units.filterIsInstance<IfStmt>().map { it.condition }.forEach(this::projectCallArg)
        // Invoke ARGUMENT edges
        this.g.body.units
            .filterIsInstance<InvokeStmt>()
            .map { it.invokeExpr as InvokeExpr }
            .forEach(this::projectCallArg)
    }

    private fun projectCallArg(value: Any) {
        getFromStore(value).firstOrNull { it is NewCallBuilder }?.let { src ->
            getFromStore(value).filterNot { it == src }
                .forEach { tgt -> builder.addEdge(src, tgt, ARGUMENT) }
        }
    }

    private fun projectLocalVariable(local: Value) {
        getFromStore(local).let { assocVertices ->
            assocVertices.filterIsInstance<NewIdentifierBuilder>().forEach { identifierV ->
                assocVertices.firstOrNull { it is NewLocalBuilder }?.let { src ->
                    builder.addEdge(identifierV, src, REF)
                }
            }
        }
    }

    /**
     * Given a unit, will construct AST information in the graph.
     *
     * @param unit The [Unit] from which AST vertices and edges will be constructed.
     */
    private fun projectUnitAsAst(unit: Unit, childIdx: Int): NewNodeBuilder? {
        currentLine = unit.javaSourceStartLineNumber
        currentCol = unit.javaSourceStartColumnNumber

        return when (unit) {
            is IfStmt -> projectIfStatement(unit, childIdx)
            is GotoStmt -> projectGotoStatement(unit, childIdx)
            is IdentityStmt -> projectVariableAssignment(unit, childIdx)
            is AssignStmt -> projectVariableAssignment(unit, childIdx)
            is LookupSwitchStmt -> projectLookupSwitch(unit, childIdx)
            is TableSwitchStmt -> projectTableSwitch(unit, childIdx)
            is InvokeStmt -> projectCallVertex(unit.invokeExpr, childIdx).apply { addToStore(unit, this, index = 0) }
            is ReturnStmt -> projectReturnVertex(unit, childIdx)
            is ReturnVoidStmt -> projectReturnVertex(unit, childIdx)
            is ThrowStmt -> projectThrowStmt(unit, childIdx)
            else -> {
                logger.debug("Unhandled class in projectUnitAsAst ${unit.javaClass} $unit"); null
            }
        }
    }

    private fun projectUnitAsCfg(unit: Unit) {
        when (unit) {
            is IfStmt -> projectIfStatementAsCfg(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is ReturnStmt -> projectReturnEdge(unit)
            is ReturnVoidStmt -> projectReturnEdge(unit)
            is IdentityStmt -> connectAssignmentCfg(unit)
            is AssignStmt -> connectAssignmentCfg(unit)
            is ThrowStmt -> Unit // Control ends at throw statements
            else -> {
                getFromStore(unit).firstOrNull()?.let { sourceVertex ->
                    g.getSuccsOf(unit).forEach { targetUnit ->
                        getFromStore(targetUnit).let { vList ->
                            builder.addEdge(sourceVertex, vList.first(), CFG)
                        }
                    }
                }
            }
        }
    }

    private fun connectAssignmentCfg(unit: DefinitionStmt) {
        val sourceVertex = getFromStore(unit).filterIsInstance<NewCallBuilder>()
            .firstOrNull { it.build().name().contains("assignment") }
        g.getSuccsOf(unit).forEach { succ ->
            if (sourceVertex != null) {
                getFromStore(succ).let { vList ->
                    val tgtVert = vList.first()
                    builder.addEdge(sourceVertex, tgtVert, CFG)
                }
            }
        }
    }

    private fun projectThrowStmt(unit: ThrowStmt, childIdx: Int): NewNodeBuilder {
        val (op, _) = projectOp(unit.op, 1)
        val throwVertex = NewUnknownBuilder()
            .order(childIdx)
            .code(unit.toString())
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
            .typeFullName("void")
        builder.addEdge(op, throwVertex, CFG)
        builder.addEdge(throwVertex, op, AST)
        addToStore(unit, op, throwVertex)
        LocalCache.getType("void")?.let { t -> builder.addEdge(throwVertex, t, EVAL_TYPE) }
        PlumeStorage.getMethodStore(g.body.method)
            .firstOrNull { it is NewBlockBuilder }
            ?.let { block -> builder.addEdge(block, throwVertex, AST) }
        return throwVertex
    }

    private fun projectTableSwitch(unit: TableSwitchStmt) {
        val switchVertices = getFromStore(unit)
        val switchCondition = switchVertices.first()
        // Handle default target jump
        projectDefaultAndCondition(unit, switchVertices, switchCondition)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) projectSwitchTarget(switchVertices, i, switchCondition, tgt)
        }
    }

    private fun projectLookupSwitch(unit: LookupSwitchStmt) {
        val lookupVertices = getFromStore(unit)
        val lookupVertex = lookupVertices.first()
        // Handle default target jump
        projectDefaultAndCondition(unit, lookupVertices, lookupVertex)
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
        conditionVertex: NewNodeBuilder,
        tgt: Unit
    ) {
        val tgtV = lookupVertices.first { it is NewJumpTargetBuilder && it.build().argumentIndex() == lookupValue }
        projectTargetPath(conditionVertex, tgtV, tgt)
    }

    private fun projectDefaultAndCondition(
        unit: SwitchStmt,
        switchVertices: List<NewNodeBuilder>,
        conditionalVertex: NewNodeBuilder
    ) {
        unit.defaultTarget.let { defaultUnit ->
            val tgtV = switchVertices.first { it is NewJumpTargetBuilder && it.build().name() == "default" }
            projectTargetPath(conditionalVertex, tgtV, defaultUnit)
        }
    }

    private fun projectTargetPath(
        lookupVertex: NewNodeBuilder,
        tgtV: NewNodeBuilder,
        tgt: Unit
    ) {
        builder.addEdge(lookupVertex, tgtV, CFG)
        getFromStore(tgt).let { vList -> builder.addEdge(tgtV, vList.first(), CFG) }
    }

    private fun projectReturnEdge(unit: Stmt) {
        getFromStore(unit).filterIsInstance<NewReturnBuilder>().firstOrNull()?.let { src ->
            PlumeStorage.getMethodStore(g.body.method)
                .filterIsInstance<NewMethodReturnBuilder>()
                .firstOrNull()?.let { tgt -> builder.addEdge(src, tgt, CFG) }
        }
    }

    /**
     * METHOD_PARAMETER_IN -EVAL_TYPE-> TYPE
     * METHOD_PARAMETER_OUT -EVAL_TYPE-> TYPE
     * METHOD_PARAMETER_IN -PARAMETER_LINK-> METHOD_PARAMETER_OUT
     */
    private fun buildParameters(graph: BriefUnitGraph): List<NewNodeBuilder> {
        val params = mutableListOf<NewNodeBuilder>()
        graph.body.parameterLocals
            .forEachIndexed { i, local ->
                SootToPlumeUtil.projectMethodParameterIn(local, currentLine, currentCol, i + 1)
                    .let { mpi ->
                        params.add(mpi)
                        val t = LocalCache.getType(mpi.build().typeFullName())
                        if (t != null) builder.addEdge(mpi, t, EVAL_TYPE)
                        if (mpi.build().evaluationStrategy() == BY_REFERENCE) {
                            SootToPlumeUtil.projectMethodParameterOut(local, currentLine, currentCol, i + 1)
                                .let { mpo ->
                                    params.add(mpo)
                                    if (t != null) builder.addEdge(mpo, t, EVAL_TYPE)
                                    builder.addEdge(mpi, mpo, PARAMETER_LINK)
                                }
                        }
                    }
            }
        return params
    }

    /**
     * LOCAL -EVAL_TYPE-> TYPE
     */
    private fun buildLocals(graph: BriefUnitGraph): List<NewLocalBuilder> {
        val paramLocals = g.heads.asSequence().map { it.useBoxes }.flatten().map { it.value }
            .filterIsInstance<IdentityRef>()
            .mapIndexed { i, head: IdentityRef ->
                projectIdentityStatement(head, currentLine, currentCol, i)
                    .apply {
                        LocalCache.getType(this.build().typeFullName())?.let { t ->
                            builder.addEdge(this, t, EVAL_TYPE)
                        }
                        methodLocals[head] = this
                        addToStore(head, this)
                    }
            }.toList()
        val locals = graph.body.locals.mapIndexed { i, local: Local ->
            SootToPlumeUtil.projectLocalVariable(local, currentLine, currentCol, i)
                .apply {
                    LocalCache.getType(this.build().typeFullName())?.let { t ->
                        builder.addEdge(this, t, EVAL_TYPE)
                    }
                    methodLocals[local] = this
                    addToStore(local, this)
                }
        }.toList()
        return locals + paramLocals
    }

    private fun projectIdentityStatement(
        identityRef: IdentityRef,
        currentLine: Int,
        currentCol: Int,
        i: Int
    ): NewLocalBuilder {
        val name = identityRef.toString().removeSuffix(": ${identityRef.type}")
        return NewLocalBuilder()
            .name(name)
            .code("${identityRef.type} $name")
            .typeFullName(identityRef.type.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(i)
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
            .dynamicTypeHintFullName(ListMapper.stringToScalaList(unit.methodRef.returnType.toQuotedString()))
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .argumentIndex(childIdx)
            .dispatchType(if (unit.methodRef.isStatic) DispatchTypes.STATIC_DISPATCH else DispatchTypes.DYNAMIC_DISPATCH)
            .typeFullName(unit.type.toString())
        val argVertices = mutableListOf<NewNodeBuilder>(callVertex)
        PlumeStorage.addCall(unit, callVertex)
        // Create vertices for arguments
        unit.args.forEachIndexed { i, arg ->
            when (arg) {
                is Local -> SootToPlumeUtil.createIdentifierVertex(arg, currentLine, currentCol, i + 1)
                is Constant -> SootToPlumeUtil.createLiteralVertex(arg, currentLine, currentCol, i + 1)
                else -> null
            }?.let { expressionVertex ->
                builder.addEdge(callVertex, expressionVertex, AST)
                builder.addEdge(callVertex, expressionVertex, ARGUMENT)
                argVertices.add(expressionVertex)
                addToStore(arg, expressionVertex)
            }
        }
        // Save PDG arguments
        addToStore(unit, *argVertices.toTypedArray())
        // Create the receiver for the call
        unit.useBoxes.filterIsInstance<JimpleLocalBox>().firstOrNull()?.let {
            SootToPlumeUtil.createIdentifierVertex(it.value, currentLine, currentCol, 0).apply {
                addToStore(it.value, this)
                builder.addEdge(callVertex, this, RECEIVER)
                builder.addEdge(callVertex, this, ARGUMENT)
                builder.addEdge(callVertex, this, AST)
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
            .code(unit.toString().replaceAfter("{", "").replace("{", ""))
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)
            .argumentIndex(childIdx)
        addToStore(unit, switchVertex)
        projectDefaultAndCondition(unit, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) {
                val tgtV = NewJumpTargetBuilder()
                    .name("case $i")
                    .code("case $i:")
                    .argumentIndex(i)
                    .lineNumber(Option.apply(tgt.javaSourceStartLineNumber))
                    .columnNumber(Option.apply(tgt.javaSourceStartColumnNumber))
                    .order(childIdx)
                builder.addEdge(switchVertex, tgtV, AST)
                addToStore(unit, tgtV)
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
            .code(unit.toString().replaceAfter("{", "").replace("{", ""))
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentIndex(childIdx)
        addToStore(unit, switchVertex)
        projectDefaultAndCondition(unit, switchVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            if (unit.defaultTarget != tgt) {
                val lookupValue = unit.getLookupValue(i)
                val tgtV = NewJumpTargetBuilder()
                    .name("case $lookupValue")
                    .code("case $lookupValue:")
                    .argumentIndex(lookupValue)
                    .lineNumber(Option.apply(tgt.javaSourceStartLineNumber))
                    .columnNumber(Option.apply(tgt.javaSourceStartColumnNumber))
                    .order(childIdx)
                builder.addEdge(switchVertex, tgtV, AST)
                addToStore(unit, tgtV)
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
    private fun projectDefaultAndCondition(unit: SwitchStmt, switchVertex: NewControlStructureBuilder) {
        val totalTgts = unit.targets.size
        val (condition, _) = projectOp(unit.key, totalTgts + 1)
        builder.addEdge(switchVertex, condition, AST)
        builder.addEdge(switchVertex, condition, CONDITION)
        // Handle default target jump
        unit.defaultTarget.let {
            val tgtV = NewJumpTargetBuilder()
                .name("default")
                .code("default:")
                .argumentIndex(totalTgts + 2)
                .lineNumber(Option.apply(it.javaSourceStartLineNumber))
                .columnNumber(Option.apply(it.javaSourceStartColumnNumber))
                .order(totalTgts + 2)
            builder.addEdge(switchVertex, tgtV, AST)
            addToStore(unit, tgtV)
        }
        addToStore(unit, condition, index = 0)
    }


    /**
     * Given an [IfStmt], will construct if statement information in the graph.
     *
     * @param unit The [IfStmt] from which a [NewControlStructureBuilder] will be constructed.
     * @return the [NewControlStructureBuilder] constructed.
     */
    private fun projectIfStatement(unit: IfStmt, childIdx: Int): NewControlStructureBuilder {
        val ifVertex = NewControlStructureBuilder()
            .controlStructureType(ControlStructureTypes.IF)
            .code(unit.toString().replaceAfter(" goto", "").replace(" goto", ""))
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentIndex(childIdx)
        /*
            CONTROL_STRUCTURE -AST|CONDITION-> CALL
         */
        val condition = unit.condition as ConditionExpr
        val (conditionExpr, conditionalCfgStart) = projectBinopExpr(condition, 0)
        builder.addEdge(ifVertex, conditionExpr, CONDITION)
        builder.addEdge(ifVertex, conditionExpr, AST)
        addToStore(unit, conditionalCfgStart, conditionExpr, ifVertex)
        return ifVertex
    }

    private fun projectGotoStatement(unit: GotoStmt, childIdx: Int): NewControlStructureBuilder {
        val gotoVertex = NewControlStructureBuilder()
            .controlStructureType(ControlStructureTypes.GOTO)
            .code(unit.toString())
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
            .order(childIdx)
            .argumentIndex(childIdx)
        addToStore(unit, gotoVertex)
        return gotoVertex
    }

    private fun projectIfStatementAsCfg(unit: IfStmt) {
        // [CFG_START_NODE, CONDITION, IF]
        val ifVertices = getFromStore(unit)
        val cfgSource = ifVertices.filterIsInstance<NewCallBuilder>().first()
        g.getSuccsOf(unit).forEach { succ ->
            getFromStore(succ).let { tgtVertices ->
                tgtVertices.let { vList ->
                    builder.addEdge(cfgSource, vList.first(), CFG)
                }
            }
        }
    }

    /**
     * Given an [AssignStmt], will construct variable assignment edge and vertex information.
     *
     * @param unit The [AssignStmt] from which a [NewCallBuilder] and its children vertices will be constructed.
     * @return the [NewCallBuilder] constructed.
     */
    private fun projectVariableAssignment(unit: DefinitionStmt, childIdx: Int): NewCallBuilder {
        val leftOp = unit.leftOp
        val rightOp = unit.rightOp
        val assignCall = NewCallBuilder()
            .name(Operators.assignment)
            .code(unit.toString())
            .signature("${leftOp.type} = ${rightOp.type}")
            .methodFullName(Operators.assignment)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(ListMapper.stringToScalaList(unit.rightOp.type.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(leftOp.type.toQuotedString())
            .lineNumber(Option.apply(unit.javaSourceStartLineNumber))
            .columnNumber(Option.apply(unit.javaSourceStartColumnNumber))
        val leftVert = when (leftOp) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(leftOp, currentLine, currentCol, 1).apply {
                addToStore(leftOp, this)
            }
            is FieldRef -> projectFieldAccess(leftOp, 1)
                .apply {
                    addToStore(leftOp.field, this)
                }
            is ArrayRef -> SootToPlumeUtil.createArrayRefIdentifier(leftOp, currentLine, currentCol, 1)
                .apply {
                    addToStore(leftOp.base, this)
                }
            else -> {
                logger.debug(
                    "UnknownVertex created for leftOp under projectVariableAssignment: ${leftOp.javaClass} " +
                            "containing value $leftOp"
                )
                NewUnknownBuilder()
                    .lineNumber(Option.apply(currentLine))
                    .columnNumber(Option.apply(currentCol))
                    .code(leftOp.toString())
                    .typeFullName(leftOp.type.toQuotedString())
                    .order(1)
            }
        }.apply {
            builder.addEdge(assignCall, this, AST)
            builder.addEdge(assignCall, this, ARGUMENT)
            addToStore(leftOp, this)
        }
        val (rightVert, rightVertCfgStart) = projectOp(rightOp, 2).apply {
            builder.addEdge(assignCall, this.first, AST)
            builder.addEdge(assignCall, this.first, ARGUMENT)
            addToStore(rightOp, this.first)
        }

        // This handles the CFG if the rightOp is a call or similar
        if (rightVert === rightVertCfgStart) {
            builder.addEdge(leftVert, rightVert, CFG)
            builder.addEdge(rightVertCfgStart, assignCall, CFG)
        } else {
            builder.addEdge(leftVert, rightVertCfgStart, CFG)
            builder.addEdge(rightVert, assignCall, CFG)
        }
        // Save PDG arguments
        addToStore(unit, leftVert, rightVert, assignCall)
        return assignCall
    }

    /**
     * Given an [BinopExpr], will construct the root operand as a [NewCallBuilder] and left and right operations of the
     * binary operation.
     *
     * @param expr The [BinopExpr] from which a [NewCallBuilder] and its children vertices will be constructed.
     * @return the [NewCallBuilder] constructed as the first and the CFG start node as the second.
     */
    private fun projectBinopExpr(expr: BinopExpr, childIdx: Int): Pair<NewCallBuilder, NewNodeBuilder> {
        val binOpExpr = SootToPlumeUtil.parseBinopExpr(expr)
        val binOpCall = NewCallBuilder()
            .name(binOpExpr)
            .code(expr.toString())
            .signature("${expr.op1.type.toQuotedString()}${expr.symbol}${expr.op2.type.toQuotedString()}")
            .methodFullName(binOpExpr)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(ListMapper.stringToScalaList(expr.op2.type.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(expr.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
        val (op1Vert, _) = projectOp(expr.op1, 1)
        builder.addEdge(binOpCall, op1Vert, AST)
        builder.addEdge(binOpCall, op1Vert, ARGUMENT)
        addToStore(expr.op1, op1Vert)

        val (op2Vert, _) = projectOp(expr.op2, 2)
        builder.addEdge(binOpCall, op2Vert, AST)
        builder.addEdge(binOpCall, op2Vert, ARGUMENT)
        addToStore(expr.op2, op2Vert)

        // Save PDG arguments
        builder.addEdge(op1Vert, op2Vert, CFG)
        builder.addEdge(op2Vert, binOpCall, CFG)
        addToStore(expr, op1Vert, op2Vert, binOpCall)
        return Pair(binOpCall, op1Vert)
    }

    private fun projectCastExpr(expr: CastExpr, childIdx: Int): Pair<NewCallBuilder, NewNodeBuilder> {
        val castBlock = NewCallBuilder()
            .name(Operators.cast)
            .code(expr.toString())
            .signature("(${expr.castType.toQuotedString()}) ${expr.op.type.toQuotedString()}")
            .methodFullName(Operators.cast)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(ListMapper.stringToScalaList(expr.op.type.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(expr.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
        val (op1, _) = projectOp(expr.op, 1)
        builder.addEdge(castBlock, op1, AST)
        builder.addEdge(castBlock, op1, ARGUMENT)

        // Save PDG arguments
        builder.addEdge(op1, castBlock, CFG)
        addToStore(expr, op1, castBlock)
        return Pair(castBlock, op1)
    }

    /**
     * Projects an operand. Sometimes these operands are nested and so the return is a pair. The pair is:
     * (Main Vertex, CFG Start Vertex)
     */
    private fun projectOp(expr: Value, childIdx: Int): Pair<NewNodeBuilder, NewNodeBuilder> {
        val singleNode = when (expr) {
            is Local -> SootToPlumeUtil.createIdentifierVertex(expr, currentLine, currentCol, childIdx)
            is IdentityRef -> projectIdentityRef(expr, currentLine, currentCol, childIdx)
            is Constant -> SootToPlumeUtil.createLiteralVertex(expr, currentLine, currentCol, childIdx)
            is InvokeExpr -> projectCallVertex(expr, childIdx)
            is StaticFieldRef -> projectFieldAccess(expr, childIdx)
            is NewExpr -> SootToPlumeUtil.createNewExpr(expr, currentLine, currentCol, childIdx)
                .apply { addToStore(expr, this) }
            is NewArrayExpr -> createNewArrayExpr(expr, childIdx)
            is CaughtExceptionRef -> SootToPlumeUtil.createIdentifierVertex(
                expr,
                currentLine,
                currentCol,
                childIdx
            )
            is InstanceFieldRef -> projectFieldAccess(expr, childIdx)
            is InstanceOfExpr -> {
                logger.debug("projectOp unhandled class ${expr.javaClass}. Unknown vertex created.")
                NewUnknownBuilder()
                    .lineNumber(Option.apply(currentLine))
                    .columnNumber(Option.apply(currentCol))
                    .code(expr.toString())
                    .typeFullName(expr.type.toQuotedString())
                    .order(1)
            } //TODO: <operator>.instanceOf
            else -> {
                if (expr !is BinopExpr && expr !is CastExpr)
                    logger.debug("projectOp unhandled class ${expr.javaClass}. Unknown vertex created.")
                NewUnknownBuilder()
                    .lineNumber(Option.apply(currentLine))
                    .columnNumber(Option.apply(currentCol))
                    .code(expr.toString())
                    .typeFullName(expr.type.toQuotedString())
                    .order(1)
            }
        }
        // Handles constructed vertex vs cfg start node
        val pair = when (expr) {
            is BinopExpr -> projectBinopExpr(expr, childIdx)
            is CastExpr -> projectCastExpr(expr, childIdx)
            else -> null
        }
        return pair ?: Pair(singleNode, singleNode)
    }

    /**
     * This handles the identifier for @this and @parameter references.
     */
    private fun projectIdentityRef(
        param: IdentityRef,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int
    ): NewIdentifierBuilder = NewIdentifierBuilder()
        .code(param.toString())
        .name(param.toString())
        .order(childIdx)
        .argumentIndex(childIdx)
        .typeFullName(param.type.toQuotedString())
        .lineNumber(Option.apply(currentLine))
        .columnNumber(Option.apply(currentCol))

    private fun projectFieldAccess(fieldRef: FieldRef, childIdx: Int): NewCallBuilder {
        val fieldAccessVars = mutableListOf<NewNodeBuilder>()
        val leftOp = when (fieldRef) {
            is StaticFieldRef -> fieldRef.fieldRef.declaringClass().type
            is InstanceFieldRef -> fieldRef.base.type
            else -> fieldRef.fieldRef.declaringClass().type
        }

        val fieldAccessBlock = NewCallBuilder()
            .name(Operators.fieldAccess)
            .code("${leftOp.toQuotedString()}.${fieldRef.field.name}")
            .signature("")
            .methodFullName(Operators.fieldAccess)
            .dispatchType(DispatchTypes.STATIC_DISPATCH)
            .dynamicTypeHintFullName(ListMapper.stringToScalaList(leftOp.toQuotedString()))
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(leftOp.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { fieldAccessVars.add(this) }
        when (fieldRef) {
            is StaticFieldRef -> {
                Pair( // TODO: Making this use an Identifier is a temporary fix for data flow passes to work
                    SootToPlumeUtil.createIdentifierVertex(fieldRef, currentLine, currentCol, 1),
                    SootToPlumeUtil.createFieldIdentifierVertex(fieldRef, currentLine, currentCol, 2)
                )
            }
            is InstanceFieldRef -> {
                Pair(
                    SootToPlumeUtil.createIdentifierVertex(fieldRef.base, currentLine, currentCol, 1),
                    SootToPlumeUtil.createFieldIdentifierVertex(fieldRef, currentLine, currentCol, 2)
                )
            }
            else -> null
        }?.let { ns ->
            ns.toList().forEach { n ->
                builder.addEdge(fieldAccessBlock, n, AST)
                builder.addEdge(fieldAccessBlock, n, ARGUMENT)
                fieldAccessVars.add(n)
            }
        }
        // Call for <op>.fieldAccess, cast doesn't need <RECEIVER>?
        // Save PDG arguments
        addToStore(fieldRef, *fieldAccessVars.toTypedArray())
        return fieldAccessBlock
    }


    private fun createNewArrayExpr(expr: NewArrayExpr, childIdx: Int = 1) =
        NewIdentifierBuilder()
            .order(childIdx)
            .argumentIndex(childIdx)
            .code(expr.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { addToStore(expr, this) }


    private fun projectReturnVertex(ret: ReturnStmt, childIdx: Int): NewReturnBuilder {
        val retV = NewReturnBuilder()
            .code(ret.toString())
            .argumentIndex(childIdx)
            .lineNumber(Option.apply(ret.javaSourceStartLineNumber))
            .columnNumber(Option.apply(ret.javaSourceStartColumnNumber))
            .order(childIdx)
        val (op1, _) = projectOp(ret.op, childIdx + 1)
        builder.addEdge(retV, op1, AST)
        builder.addEdge(retV, op1, ARGUMENT)
        PlumeStorage.getMethodStore(g.body.method)
            .firstOrNull { it is NewBlockBuilder }
            ?.let { block -> builder.addEdge(block, retV, AST) }
        builder.addEdge(op1, retV, CFG)
        addToStore(ret, op1, retV)
        return retV
    }

    private fun projectReturnVertex(ret: ReturnVoidStmt, childIdx: Int): NewReturnBuilder {
        val retV = NewReturnBuilder()
            .code(ret.toString())
            .argumentIndex(childIdx)
            .lineNumber(Option.apply(ret.javaSourceStartLineNumber))
            .columnNumber(Option.apply(ret.javaSourceStartColumnNumber))
            .order(childIdx)
        PlumeStorage.getMethodStore(g.body.method)
            .firstOrNull { it is NewBlockBuilder }
            ?.let { block -> builder.addEdge(block, retV, AST) }
        addToStore(ret, retV)
        return retV
    }
}