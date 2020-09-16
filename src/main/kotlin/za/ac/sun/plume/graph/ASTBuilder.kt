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
 *
 * @param driver The driver to build the AST with.
 * @param sootToPlume A pointer to the map that keeps track of the Soot object to its respective [PlumeVertex].
 */
class ASTBuilder(private val driver: IDriver, private val sootToPlume: MutableMap<Any, MutableList<PlumeVertex>>) : IGraphBuilder {
    private val logger = LogManager.getLogger(ASTBuilder::javaClass)

    private var order = 0
    private var currentLine = -1
    private var currentCol = -1
    private lateinit var currentClass: FileVertex
    private lateinit var currentMethod: MethodVertex
    private lateinit var methodEntryPoint: BlockVertex
    private lateinit var currentType: TypeDeclVertex
    private lateinit var graph: BriefUnitGraph
    private val classMembers = mutableListOf<PlumeVertex>()

    init {
        order = driver.maxOrder()
    }

    override fun build(mtd: SootMethod, graph: BriefUnitGraph) {
        logger.debug("Building AST for ${mtd.declaration}")
        this.graph = graph
        projectMethodHead(mtd)
        projectMethodBody(graph)
    }

    /**
     * Constructs the file, package, and type information from the given [SootClass].
     *
     * @param cls The [SootClass] from which the file and package information is constructed from.
     */
    fun buildProgramStructure(cls: SootClass) {
        logger.debug("Building file and namespace for ${cls.name}")
        val classChildrenVertices = mutableListOf<PlumeVertex>()
        var nbv: NamespaceBlockVertex? = null
        if (cls.packageName.isNotEmpty()) {
            // Populate namespace block chain
            val namespaceList = cls.packageName.split(".").toTypedArray()
            if (namespaceList.isNotEmpty()) nbv = populateNamespaceChain(namespaceList)
        }
        currentClass = FileVertex(cls.shortName, order++)
        // Join FILE and NAMESPACE_BLOCK if namespace is present
        if (nbv != null) {
            driver.addEdge(currentClass, nbv, EdgeLabel.AST); classChildrenVertices.add(nbv)
        }
        // Create TYPE_DECL
        currentType = TypeDeclVertex(
                name = cls.shortName,
                fullName = cls.name,
                typeDeclFullName = cls.javaStyleName,
                order = order++
        ).apply {
            driver.addEdge(this, currentClass, EdgeLabel.SOURCE_FILE)
            // Attach fields to the TypeDecl
            cls.fields.forEach { field ->
                projectMember(field).let { memberVertex ->
                    driver.addEdge(this, memberVertex, EdgeLabel.AST)
                    classMembers.add(memberVertex)
                    sootToPlume[field] = mutableListOf<PlumeVertex>(memberVertex)
                }
            }
            classChildrenVertices.add(this)
        }
        sootToPlume[currentClass] = classChildrenVertices
    }

    /**
     * Creates a change of [NamespaceBlockVertex]s and returns the final one in the chain.
     *
     * @param namespaceList A list of package names.
     * @return The final [NamespaceBlockVertex] in the chain (the one associated with the file).
     */
    private fun populateNamespaceChain(namespaceList: Array<String>): NamespaceBlockVertex {
        var prevNamespaceBlock = NamespaceBlockVertex(namespaceList[0], namespaceList[0], order++)
        if (namespaceList.size == 1) return prevNamespaceBlock

        var currNamespaceBlock: NamespaceBlockVertex? = null
        val namespaceBuilder = StringBuilder(namespaceList[0])
        for (i in 1 until namespaceList.size) {
            namespaceBuilder.append("." + namespaceList[i])
            currNamespaceBlock = NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), order++)
            driver.addEdge(currNamespaceBlock, prevNamespaceBlock, EdgeLabel.AST)
            prevNamespaceBlock = currNamespaceBlock
        }
        return currNamespaceBlock ?: prevNamespaceBlock
    }

    /**
     * Creates the [MethodVertex] and its children vertices [MethodParameterInVertex] for parameters,
     * [MethodReturnVertex] for the formal return spec, [LocalVertex] for all local vertices, [BlockVertex] the method
     * entrypoint, and [ModifierVertex] for the modifiers.
     *
     * @param mtd The [MethodVertex] from which the method and modifier information is constructed from.
     */
    private fun projectMethodHead(mtd: SootMethod) {
        val methodHeadChildren = mutableListOf<PlumeVertex>()
        currentLine = mtd.javaSourceStartLineNumber
        currentCol = mtd.javaSourceStartColumnNumber
        // Method vertex
        currentMethod = MethodVertex(mtd.name, "${mtd.declaringClass}.${mtd.name}", mtd.subSignature, mtd.declaration, currentLine, currentCol, order++)
        methodHeadChildren.add(currentMethod)
        // Add to package
        driver.addEdge(currentType, currentMethod, EdgeLabel.AST)
        // Add to source file
        driver.addEdge(currentMethod, currentClass, EdgeLabel.SOURCE_FILE)
        // Store return type
        projectMethodReturnVertex(mtd.returnType)
                .apply { driver.addEdge(currentMethod, this, EdgeLabel.AST); methodHeadChildren.add(this) }
        // Modifier vertices
        SootParserUtil.determineModifiers(mtd.modifiers, mtd.name)
                .map { ModifierVertex(it, order++) }
                .forEach { driver.addEdge(currentMethod, it, EdgeLabel.AST); methodHeadChildren.add(it) }
        // Store method vertex
        methodEntryPoint = BlockVertex(ENTRYPOINT, VOID, ENTRYPOINT, order++, 0, currentLine, currentCol)
                .apply { driver.addEdge(currentMethod, this, EdgeLabel.AST); methodHeadChildren.add(this) }
        // Connect and create parameters and locals
        graph.body.parameterLocals
                .map(this::projectMethodParameterIn)
                .forEach { driver.addEdge(currentMethod, it, EdgeLabel.AST); methodHeadChildren.add(it) }
        graph.body.locals
                .filter { !graph.body.parameterLocals.contains(it) }
                .map(this::projectLocalVariable)
                .forEach { driver.addEdge(currentMethod, it, EdgeLabel.AST); methodHeadChildren.add(it) }
        // Associate all head vertices to the SootMethod
        sootToPlume[mtd] = methodHeadChildren
    }

    /**
     * Projects member information from class field data.
     *
     * @param field The [SootField] from which the class member information is constructed from.
     */
    private fun projectMember(field: SootField) = MemberVertex(
            name = field.name,
            code = field.declaration,
            typeFullName = field.type.toQuotedString(),
            order = order++
    )

    /**
     * Traverses the method body's [UnitGraph] to construct the the AST from.
     *
     * @param graph The [UnitGraph] from which the vertices and AST edges are created from.
     */
    private fun projectMethodBody(graph: UnitGraph) =
            graph.body.units.forEach { u -> projectUnit(u)?.let { driver.addEdge(methodEntryPoint, it, EdgeLabel.AST) } }

    /**
     * Given a unit, will construct AST information in the graph.
     *
     * @param unit The [Unit] from which AST vertices and edges will be constructed.
     */
    private fun projectUnit(unit: Unit): PlumeVertex? {
        currentLine = unit.javaSourceStartLineNumber
        currentCol = unit.javaSourceStartColumnNumber
        sootToPlume[unit] = mutableListOf()

        val unitVertex: PlumeVertex? = when (unit) {
            is IfStmt -> projectIfStatement(unit)
            is AssignStmt -> projectVariableAssignment(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is InvokeStmt -> projectCallVertex(unit.invokeExpr).apply { sootToPlume[unit.invokeExpr]!!.add(0, this) }
            // TODO: Discern between method return and return statement
            is ReturnStmt -> projectReturnVertex(unit)
            is ReturnVoidStmt -> projectReturnVertex(unit)
            else -> {
                logger.debug("Unhandled class in projectUnit ${unit.javaClass} $unit"); null
            }
        }
        return unitVertex?.apply { if (this !is InvokeStmt) sootToPlume[unit]!!.add(0, this) }
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
                methodFullName = unit.method.declaration,
                argumentIndex = 0,
                dispatchType = if (unit.methodRef.isStatic) DispatchType.STATIC_DISPATCH else DispatchType.DYNAMIC_DISPATCH,
                typeFullName = unit.type.toString(),
                dynamicTypeHintFullName = unit.type.toQuotedString()
        )
        val callVertices = mutableListOf<PlumeVertex>(callVertex)
        unit.args.map { it }.forEach {
            when (it) {
                is Local -> createIdentifierVertex(it).apply { sootToPlume[it]!!.add(this) }
                is Constant -> createLiteralVertex(it)
                else -> null
            }?.let { expressionVertex ->
                driver.addEdge(callVertex, expressionVertex, EdgeLabel.AST);
                callVertices.add(expressionVertex)
            }
        }
        // Save PDG arguments
        if (sootToPlume[unit] == null) sootToPlume[unit] = callVertices
        else sootToPlume[unit]?.addAll(callVertices)
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
        unit.targets.forEach { tgt ->
            if (unit.defaultTarget != tgt) {
                val i = unit.targets.indexOf(tgt)
                val tgtV = JumpTargetVertex("CASE $i", i, tgt.javaSourceStartLineNumber, tgt.javaSourceStartColumnNumber, tgt.toString(), order++)
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
            val tgtV = JumpTargetVertex("DEFAULT", -1, it.javaSourceStartLineNumber, it.javaSourceStartColumnNumber, it.toString(), order++)
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
                JumpTargetVertex(FALSE_TARGET, 0, it.javaSourceStartLineNumber, it.javaSourceStartColumnNumber, "ELSE_BODY", order++)
            } else {
                JumpTargetVertex(TRUE_TARGET, 1, it.javaSourceStartLineNumber, it.javaSourceStartColumnNumber, "IF_BODY", order++)
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
                lineNumber = unit.javaSourceStartLineNumber,
                columnNumber = unit.javaSourceStartColumnNumber,
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
     * @return the constructed vertex.
     */
    private fun projectMethodParameterIn(local: Local): MethodParameterInVertex {
        val evalStrat = SootParserUtil.determineEvaluationStrategy(local.type.toString(), isMethodReturn = false)
        val methodParameterInVertex = MethodParameterInVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                evaluationStrategy = evalStrat,
                typeFullName = local.type.toString(),
                lineNumber = currentMethod.lineNumber,
                order = order++
        )
        if (sootToPlume[local] == null) sootToPlume[local] = mutableListOf<PlumeVertex>(methodParameterInVertex)
        else sootToPlume[local]!!.add(methodParameterInVertex)
        return methodParameterInVertex
    }

    /**
     * Given an [Local], will construct local variable information in the graph.
     *
     * @param local The [Local] from which a [LocalVertex] will be constructed.
     * @return the constructed vertex.
     */
    private fun projectLocalVariable(local: Local): LocalVertex {
        val localVertex = LocalVertex(
                code = "${local.type} ${local.name}",
                name = local.name,
                typeFullName = local.type.toString(),
                lineNumber = currentLine,
                columnNumber = currentCol,
                order = order++
        )
        if (sootToPlume[local] == null) sootToPlume[local] = mutableListOf<PlumeVertex>(localVertex)
        else sootToPlume[local]!!.add(localVertex)
        return localVertex
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
            is Local -> createIdentifierVertex(leftOp)
            is FieldRef -> createFieldIdentifierVertex(leftOp)
            else -> {
                logger.debug("Unhandled class for leftOp under projectVariableAssignment: ${leftOp.javaClass} containing value ${leftOp}")
                null
            }
        }?.let { driver.addEdge(assignBlock, it, EdgeLabel.AST); assignVariables.add(it) }
        projectOp(rightOp)?.let { driver.addEdge(assignBlock, it, EdgeLabel.AST); assignVariables.add(it) }
        // Save PDG arguments
        if (sootToPlume[unit] == null) sootToPlume[unit] = assignVariables
        else sootToPlume[unit]?.addAll(assignVariables)
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
        projectOp(expr.op1)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST);binopVertices.add(it) }
        projectOp(expr.op2)?.let { driver.addEdge(binOpBlock, it, EdgeLabel.AST);binopVertices.add(it) }
        // Save PDG arguments
        if (sootToPlume[expr] == null) sootToPlume[expr] = binopVertices
        else sootToPlume[expr]?.addAll(binopVertices)
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
        sootToPlume[expr] = conditionVertices
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
        if (sootToPlume[expr] == null) sootToPlume[expr] = castVertices
        else sootToPlume[expr]?.addAll(castVertices)
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
                lineNumber = currentLine,
                columnNumber = currentCol
        )
    }

    private fun createIdentifierVertex(local: Value): IdentifierVertex {
        return IdentifierVertex(
                code = "$local",
                name = local.toString(),
                order = order++,
                argumentIndex = 0,
                typeFullName = local.type.toQuotedString(),
                lineNumber = currentLine,
                columnNumber = currentCol
        ).apply {
            if (sootToPlume[local] == null) sootToPlume[local] = mutableListOf<PlumeVertex>(this)
            else sootToPlume[local]?.add(this)
        }
    }

    private fun createFieldIdentifierVertex(field: FieldRef): PlumeVertex {
        return FieldIdentifierVertex(
                canonicalName = field.field.signature,
                code = field.field.declaration,
                argumentIndex = 0,
                lineNumber = currentLine,
                columnNumber = currentCol,
                order = order++
        ).apply { sootToPlume[field.field]?.add(this) }
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
        driver.addEdge(methodEntryPoint, retV, EdgeLabel.AST)
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
        driver.addEdge(methodEntryPoint, retV, EdgeLabel.AST)
        return retV
    }

    private fun projectMethodReturnVertex(type: Type): MethodReturnVertex {
        return MethodReturnVertex(
                name = RETURN,
                code = "RETURN ${type.toQuotedString()}",
                evaluationStrategy = SootParserUtil.determineEvaluationStrategy(type.toQuotedString(), true),
                typeFullName = type.toQuotedString(),
                lineNumber = currentLine,
                columnNumber = currentCol,
                order = order++
        )
    }

}
