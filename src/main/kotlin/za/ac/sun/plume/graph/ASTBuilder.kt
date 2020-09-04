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
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.util.ExtractorConst.ASSIGN
import za.ac.sun.plume.util.ExtractorConst.BINOPS
import za.ac.sun.plume.util.ExtractorConst.BOOLEAN
import za.ac.sun.plume.util.ExtractorConst.CAST
import za.ac.sun.plume.util.ExtractorConst.ENTRYPOINT
import za.ac.sun.plume.util.ExtractorConst.FALSE_TARGET
import za.ac.sun.plume.util.ExtractorConst.IF_ROOT
import za.ac.sun.plume.util.ExtractorConst.LOOKUP_ROOT
import za.ac.sun.plume.util.ExtractorConst.RETURN
import za.ac.sun.plume.util.ExtractorConst.SWITCH_ROOT
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET
import za.ac.sun.plume.util.ExtractorConst.VOID
import za.ac.sun.plume.util.SootParserUtil

/**
 * The [IGraphBuilder] that constructs the vertices of the package/file/method hierarchy and connects the AST edges.
 */
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
    private val namespaceMapping = LinkedHashMap<String, NamespaceBlockVertex>()
    private val classMembers = mutableListOf<PlumeVertex>()

    init {
        order = driver.maxOrder()
    }

    override fun build(mtd: SootMethod, graph: BriefUnitGraph, sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>) {
        logger.debug("Building AST for ${mtd.declaration}")
        this.graph = graph
        this.sootToPlume = sootToPlume
        // TODO: Add fields to sootToPlume here
        projectMethodHead(mtd)
        projectMethodBody(graph)
    }

    /**
     * Constructs the file and package information from the given [SootClass].
     *
     * @param cls The [SootClass] from which the file and package information is constructed from.
     */
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
        cls.fields.forEach {
            projectMember(it).let { memberVertex ->
                driver.addEdge(currentClass, memberVertex, EdgeLabel.AST)
                classMembers.add(memberVertex)
            }
        }
        // Add metadata if not present
        // TODO: This is currently erroneous and can be improved
        driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version")))
    }

    /**
     * Creates a change of [NamespaceBlockVertex]s and returns the final one in the chain.
     *
     * @param namespaceList A list of package names.
     * @return The final [NamespaceBlockVertex] in the chain (the one associated with the file).
     */
    private fun populateNamespaceChain(namespaceList: Array<String>): NamespaceBlockVertex {
        var prevNamespaceBlock = NamespaceBlockVertex(namespaceList[0], namespaceList[0], order++)
        namespaceMapping[namespaceList[0]] = prevNamespaceBlock
        if (namespaceList.size == 1) return prevNamespaceBlock

        var currNamespaceBlock: NamespaceBlockVertex? = null
        val namespaceBuilder = StringBuilder(namespaceList[0])
        for (i in 1 until namespaceList.size) {
            namespaceBuilder.append("." + namespaceList[i])
            currNamespaceBlock = NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++)
            namespaceMapping[namespaceList[i]] = currNamespaceBlock
            if (!driver.exists(prevNamespaceBlock, currNamespaceBlock, EdgeLabel.AST))
                driver.addEdge(prevNamespaceBlock, currNamespaceBlock, EdgeLabel.AST)
            prevNamespaceBlock = currNamespaceBlock
        }
        return currNamespaceBlock ?: prevNamespaceBlock
    }

    /**
     * Creates the [MethodVertex] along with the modifiers.
     *
     * @param mtd The [MethodVertex] from which the method and modifier information is constructed from.
     */
    private fun projectMethodHead(mtd: SootMethod) {
        currentLine = mtd.javaSourceStartLineNumber
        // Method vertex
        currentMethod = MethodVertex(mtd.name, "${mtd.declaringClass}.${mtd.name}", mtd.subSignature, mtd.declaration, currentLine, order++)
        val packageName = mtd.declaringClass.toString().removeSuffix(".${currentClass.name}")
        namespaceMapping.values.firstOrNull { it.fullName == packageName }?.let { driver.addEdge(it, currentMethod, EdgeLabel.AST) }
        // Store return type
        returnType = mtd.returnType
        // Modifier vertices
        SootParserUtil.determineModifiers(mtd.modifiers, mtd.name).forEach { driver.addEdge(currentMethod, ModifierVertex(it, order++), EdgeLabel.AST) }
        // Store method vertex
        sootToPlume[mtd] = mutableListOf<PlumeVertex>(currentMethod)
        methodEntryPoint = BlockVertex(ENTRYPOINT, ENTRYPOINT, order++, 0, VOID, mtd.javaSourceStartLineNumber)
        sootToPlume[mtd]?.add(methodEntryPoint)
    }

    /**
     * Projects member information from class field data.
     *
     * @param field The [SootField] from which the class member information is constructed from.
     */
    private fun projectMember(field: SootField): MemberVertex {
        return MemberVertex(
                name = field.name,
                code = field.declaration,
                typeFullName = field.type.toQuotedString(),
                order = order++
        )
    }

    /**
     * Traverses the method body's [UnitGraph] to construct the the AST from.
     *
     * @param graph The [UnitGraph] from which the vertices and AST edges are created from.
     */
    private fun projectMethodBody(graph: UnitGraph) {
        driver.addEdge(currentMethod, methodEntryPoint, EdgeLabel.AST)
        graph.body.parameterLocals.forEach(this::projectMethodParameterIn)
        graph.body.locals.filter { !graph.body.parameterLocals.contains(it) }.forEach { projectLocalVariable(it) }
        graph.body.units.forEach { u -> projectUnit(u)?.let { if (it !is MethodReturnVertex) driver.addEdge(methodEntryPoint, it, EdgeLabel.AST) } }
    }

    /**
     * Given a unit, will construct AST information in the graph.
     *
     * @param unit The [Unit] from which AST vertices and edges will be constructed.
     */
    private fun projectUnit(unit: Unit): PlumeVertex? {
        currentLine = unit.javaSourceStartLineNumber
        sootToPlume[unit] = mutableListOf()

        val unitVertex: PlumeVertex? = when (unit) {
            is IfStmt -> projectIfStatement(unit)
            is AssignStmt -> projectVariableAssignment(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is InvokeStmt -> projectCallVertex(unit)
            // TODO: Discern between method return and return statement
            is ReturnStmt -> projectReturnVertex(unit)
            is ReturnVoidStmt -> projectReturnVertex(unit)
            else -> {
                logger.debug("Unhandled class in projectUnit ${unit.javaClass}"); null
            }
        }
        return unitVertex?.apply { sootToPlume[unit]!!.add(0, unitVertex) }
    }

    /**
     * Given an [InvokeStmt], will construct Call information in the graph.
     *
     * @param unit The [InvokeStmt] from which a [CallVertex] will be constructed.
     * @return the [CallVertex] constructed.
     */
    private fun projectCallVertex(unit: InvokeStmt): CallVertex {
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
        return callVertex
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
                methodFullName = unit.method.declaration,
                methodInstFullName = unit.toString(),
                argumentIndex = 0,
                dispatchType = if (unit.methodRef.isStatic) DispatchType.STATIC_DISPATCH else DispatchType.DYNAMIC_DISPATCH,
                typeFullName = unit.type.toString()
        )
        unit.useBoxes.map { it.value }.filterIsInstance<Local>().forEach { sootToPlume[it]!!.add(callVertex) }
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
                order = order++,
                argumentIndex = 0
        )
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        unit.targets.forEach { tgt ->
            if (unit.defaultTarget != tgt) {
                val i = unit.targets.indexOf(tgt)
                val tgtV = JumpTargetVertex("CASE $i", i, tgt.javaSourceStartLineNumber, tgt.toString(), order++)
                driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
                sootToPlume[unit]!!.add(tgtV)
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
                order = order++,
                argumentIndex = 0
        )
        projectSwitchDefault(unit, switchVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            if (unit.defaultTarget != tgt) {
                val lookupValue = unit.getLookupValue(i)
                val tgtV = JumpTargetVertex("CASE $lookupValue", lookupValue, tgt.javaSourceStartLineNumber, tgt.toString(), order++)
                driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
                sootToPlume[unit]!!.add(tgtV)
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
            val tgtV = JumpTargetVertex("DEFAULT", -1, it.javaSourceStartLineNumber, it.toString(), order++)
            driver.addEdge(switchVertex, tgtV, EdgeLabel.AST)
            sootToPlume[unit]!!.add(tgtV)
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
                JumpTargetVertex(FALSE_TARGET, 0, it.javaSourceStartLineNumber, "ELSE_BODY", order++)
            } else {
                JumpTargetVertex(TRUE_TARGET, 0, it.javaSourceStartLineNumber, "IF_BODY", order++)
            }
            driver.addEdge(ifRootVertex, condBody, EdgeLabel.AST)
            sootToPlume[unit]!!.add(condBody)
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

    /**
     * Given an [Local], will construct method parameter information in the graph.
     *
     * @param local The [Local] from which a [MethodParameterInVertex] will be constructed.
     */
    private fun projectMethodParameterIn(local: Local) {
        val evalStrat = SootParserUtil.determineEvaluationStrategy(local.type.toString(), isMethodReturn = false)
        val methodParameterInVertex = MethodParameterInVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                evaluationStrategy = evalStrat,
                typeFullName = local.type.toString(),
                lineNumber = currentMethod.lineNumber,
                order = order++
        )
        driver.addEdge(currentMethod, methodParameterInVertex, EdgeLabel.AST)
        sootToPlume[local] = mutableListOf<PlumeVertex>(methodParameterInVertex)
    }

    /**
     * Given an [Local], will construct local variable information in the graph.
     *
     * @param local The [Local] from which a [LocalVertex] will be constructed.
     */
    private fun projectLocalVariable(local: Local): LocalVertex {
        val localVertex = LocalVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                typeFullName = local.type.toString(),
                lineNumber = currentLine,
                order = order++
        )
        driver.addEdge(methodEntryPoint, localVertex, EdgeLabel.AST)
        sootToPlume[local] = mutableListOf<PlumeVertex>(localVertex)
        return localVertex
    }

    /**
     * Given an [AssignStmt], will construct variable assignment edge and vertex information.
     *
     * @param unit The [AssignStmt] from which a [BlockVertex] and its children vertices will be constructed.
     * @return the [BlockVertex] constructed.
     */
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
        when (leftOp) {
            is Local -> createIdentifierVertex(leftOp)
            is StaticFieldRef -> createFieldIdentifierVertex(leftOp)
            else -> null
        }?.let { driver.addEdge(assignBlock, it, EdgeLabel.AST) }
        projectOp(rightOp)?.let { driver.addEdge(assignBlock, it, EdgeLabel.AST) }

        return assignBlock
    }

    /**
     * Given an [BinopExpr], will construct the root operand as a [BlockVertex] and left and right operations of the
     * binary operation.
     *
     * @param expr The [BinopExpr] from which a [BlockVertex] and its children vertices will be constructed.
     * @return the [BlockVertex] constructed.
     */
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
        projectOp(expr.op1)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST) }
        projectOp(expr.op2)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST) }
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
        projectOp(expr.op1)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST) }
        projectOp(expr.op2)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST) }
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
        projectOp(expr.op)?.let { driver.addEdge(castBlock, it, EdgeLabel.AST) }
        return castBlock
    }

    private fun projectOp(expr: Value): PlumeVertex? {
        return when (expr) {
            is Local -> createIdentifierVertex(expr)
            is Constant -> createLiteralVertex(expr)
            is CastExpr -> projectCastExpr(expr)
            is BinopExpr -> projectBinopExpr(expr)
            is InvokeExpr -> projectCallVertex(expr)
            is StaticFieldRef -> createFieldIdentifierVertex(expr)
            else -> {
                logger.debug("projectOp unhandled class" + expr.javaClass); null
            }
        }
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

    private fun createFieldIdentifierVertex(field: StaticFieldRef): PlumeVertex {
        // TODO: Handle PDG data
        return FieldIdentifierVertex(
                canonicalName = field.field.signature,
                code = field.field.declaration,
                argumentIndex = 0,
                lineNumber = currentLine,
                order = order++
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
        projectOp(ret.op)?.let { driver.addEdge(retV, it, EdgeLabel.AST) }
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
