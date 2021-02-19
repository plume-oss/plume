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
package io.github.plume.oss.util

import io.github.plume.oss.Extractor
import io.github.plume.oss.Extractor.Companion.addSootToPlumeAssociation
import io.github.plume.oss.domain.mappers.VertexMapper.mapToVertex
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.SootParserUtil.determineEvaluationStrategy
import io.github.plume.oss.util.SootParserUtil.determineModifiers
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import scala.Option
import scala.jdk.CollectionConverters
import soot.*
import soot.jimple.ArrayRef
import soot.jimple.Constant
import soot.jimple.FieldRef
import soot.jimple.NewExpr
import soot.toolkits.graph.BriefUnitGraph

/**
 * A utility class of methods to convert Soot objects to [NewNodeBuilder] items and construct pieces of the CPG.
 */
object SootToPlumeUtil {

    private val logger = LogManager.getLogger(SootToPlumeUtil::class.java)

    /**
     * Projects member information from class field data.
     *
     * @param field The [SootField] from which the class member information is constructed from.
     */
    fun projectMember(field: SootField, childIdx: Int): NewMemberBuilder =
        NewMemberBuilder()
            .name(field.name)
            .code(field.declaration)
            .typeFullName(field.type.toQuotedString())
            .order(childIdx)

    /**
     * Given an [soot.Local], will construct method parameter information in the graph.
     *
     * @param local The [soot.Local] from which a [NewMethodParameterInBuilder] will be constructed.
     * @return the constructed vertex.
     */
    fun projectMethodParameterIn(
        local: soot.Local,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int
    ): NewMethodParameterInBuilder =
        NewMethodParameterInBuilder()
            .name(local.name)
            .code("${local.type} ${local.name}")
            .evaluationStrategy(determineEvaluationStrategy(local.type.toString(), isMethodReturn = false))
            .typeFullName(local.type.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

    /**
     * Given an [soot.Local], will construct local variable information in the graph.
     *
     * @param local The [soot.Local] from which a [NewLocal] will be constructed.
     * @return the constructed vertex.
     */
    fun projectLocalVariable(local: soot.Local, currentLine: Int, currentCol: Int, childIdx: Int): NewLocalBuilder =
        NewLocalBuilder()
            .name(local.name)
            .code("${local.type} ${local.name}")
            .typeFullName(local.type.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

    /**
     * Creates the [NewMethodBuilder] and its children vertices [NewMethodParameterInBuilder] for parameters,
     * [NewMethodReturnBuilder] for the formal return spec, [NewLocalBuilder] for all local vertices, [NewBlockBuilder]
     * the method entrypoint, and [NewModifierBuilder] for the modifiers.
     *
     * @param mtd The [SootMethod] from which the method and modifier information is constructed from.
     * @param driver The [IDriver] to which the method head is built.
     */
    fun buildMethodHead(mtd: SootMethod, driver: IDriver): NewMethodBuilder {
        val currentLine = mtd.javaSourceStartLineNumber
        val currentCol = mtd.javaSourceStartColumnNumber
        var childIdx = 1
        // Method vertex
        val mtdVertex = NewMethodBuilder()
            .name(mtd.name)
            .fullName("${mtd.declaringClass}.${mtd.name}")
            .filename(sootClassToFileName(mtd.declaringClass))
            .signature(mtd.subSignature)
            .code(mtd.declaration)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx++)
            .astParentFullName("${mtd.declaringClass}")
            .astParentType(TYPE_DECL)
        addSootToPlumeAssociation(mtd, mtdVertex)
        // Store method vertex
        NewBlockBuilder()
            .typeFullName(mtd.returnType.toQuotedString())
            .code(ExtractorConst.ENTRYPOINT)
            .order(childIdx++)
            .argumentIndex(0)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { driver.addEdge(mtdVertex, this, AST); addSootToPlumeAssociation(mtd, this) }
        // Store return type
        projectMethodReturnVertex(mtd.returnType, currentLine, currentCol, childIdx++)
            .apply { driver.addEdge(mtdVertex, this, AST); addSootToPlumeAssociation(mtd, this) }
        // Modifier vertices
        determineModifiers(mtd.modifiers, mtd.name)
            .map { NewModifierBuilder().modifierType(it).order(childIdx++) }
            .forEach { driver.addEdge(mtdVertex, it, AST) }
        return mtdVertex
    }

    /**
     * Given a method whose body cannot be retrieved, will construct a call-to-return graph with known information. This
     * construction includes building the type declaration and program structure.
     *
     * @param mtd The known method information to construct from.
     * @param driver The driver to construct the phantom to.
     * @return the [NewMethod] representing the phantom method.
     */
    fun constructPhantom(mtd: SootMethod, driver: IDriver): NewNodeBuilder {
        val mtdVertex = buildMethodHead(mtd, driver)
        val currentLine = mtd.javaSourceStartLineNumber
        val currentCol = mtd.javaSourceStartColumnNumber
        // Connect and create parameters with placeholder names
        connectCallToReturn(
            mtd,
            driver,
            mtdVertex,
            currentLine,
            currentCol,
            (Extractor.getSootAssociation(mtd)?.size ?: 0) + 1
        )
        // Create program structure
        val cls = mtd.declaringClass
        buildClassStructure(cls, driver)
        val typeDecl = buildTypeDeclaration(cls.type)
        determineModifiers(cls.modifiers)
            .mapIndexed { i, m -> NewModifierBuilder().modifierType(m).order(i + 1) }
            .forEach { driver.addEdge(typeDecl, it, AST) }
        addSootToPlumeAssociation(cls, typeDecl)
        cls.fields.forEachIndexed { i, field ->
            projectMember(field, i + 1).let { memberVertex ->
                driver.addEdge(typeDecl, memberVertex, AST)
                addSootToPlumeAssociation(field, memberVertex)
            }
        }
        connectMethodToTypeDecls(mtd, driver)
        return mtdVertex
    }

    private fun connectCallToReturn(
        mtd: SootMethod,
        driver: IDriver,
        mtdVertex: NewMethodBuilder,
        currentLine: Int,
        currentCol: Int,
        initialChildIdx: Int = 1
    ) {
        var childIdx = initialChildIdx
        mtd.parameterTypes.forEachIndexed { i, type ->
            NewMethodParameterInBuilder()
                .code("$type param$i")
                .name("param$i")
                .evaluationStrategy(determineEvaluationStrategy(type.toString(), isMethodReturn = false))
                .typeFullName(type.toString())
                .lineNumber(Option.apply(mtd.javaSourceStartLineNumber))
                .columnNumber(Option.apply(mtd.javaSourceStartColumnNumber))
                .order(childIdx++)
                .apply { driver.addEdge(mtdVertex, this, AST); addSootToPlumeAssociation(mtd, this) }
        }
        // Connect a call to return
        val entryPoint = Extractor.getSootAssociation(mtd)?.filterIsInstance<NewBlockBuilder>()?.firstOrNull()
        val mtdReturn = Extractor.getSootAssociation(mtd)?.filterIsInstance<NewMethodReturnBuilder>()?.firstOrNull()
        NewReturnBuilder()
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx++)
            .argumentIndex(initialChildIdx)
            .code("return ${mtd.returnType.toQuotedString()}")
            .apply {
                driver.addEdge(entryPoint!!, this, CFG)
                driver.addEdge(this, mtdReturn!!, CFG)
            }
    }

    fun createNewExpr(expr: NewExpr, currentLine: Int, currentCol: Int, childIdx: Int): NewTypeRefBuilder {
        return NewTypeRefBuilder()
            .typeFullName(expr.baseType.toQuotedString())
            .code(expr.toString())
            .argumentIndex(childIdx)
            .order(childIdx)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .apply { addSootToPlumeAssociation(expr, this) }
    }

    /**
     * Constructs the file, package, and type information from the given [SootClass].
     *
     * @param cls The [SootClass] from which the file and package information is constructed from.
     */
    fun buildClassStructure(cls: SootClass, driver: IDriver): NewFileBuilder {
        val classChildrenVertices = mutableListOf<NewNodeBuilder>()
        val fileHash = Extractor.getFileHashPair(cls)
        val filename = sootClassToFileName(cls)
        var nbv: NewNamespaceBlockBuilder? = null
        if (cls.packageName.isNotEmpty()) {
            // Populate namespace block chain
            val namespaceList = arrayOf(cls.packageName)
            // TODO : the CPG spec doesn't know these chains, simplify
            if (namespaceList.isNotEmpty()) nbv = populateNamespaceChain(namespaceList, filename, driver)
        }
        val order = if (nbv != null) {
            driver.getNeighbours(nbv).use { ns ->
                ns.node(nbv.id())?.outE()?.asSequence()?.toList()?.size ?: 1
            }
        } else 1
        return NewFileBuilder()
            .name(sootClassToFileName(cls))
            .hash(Option.apply(fileHash.toString()))
            .order(order)
            .apply {
                // Join FILE and NAMESPACE_BLOCK if namespace is present
                if (nbv != null) {
                    driver.addEdge(this, nbv, AST); classChildrenVertices.add(nbv)
                }
                classChildrenVertices.add(0, this)
                addSootToPlumeAssociation(cls, classChildrenVertices)
            }
    }

    /**
     * Derive a file name from an object of type `SootClass`
     * @param cls the soot class
     * @return the filename in string form
     * */
    fun sootClassToFileName(cls: SootClass): String {
        val packageName = cls.packageName
        return if (packageName != null) {
            "/" + cls.name.replace(".", "/") + ".class"
        } else {
            io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN()
        }
    }

    /**
     * Creates a change of [NewNamespaceBlockBuilder]s and returns the final one in the chain.
     *
     * @param namespaceList A list of package names.
     * @return The final [NewNamespaceBlockBuilder] in the chain (the one associated with the file).
     */
    private fun populateNamespaceChain(
        namespaceList: Array<String>,
        filename: String,
        driver: IDriver
    ): NewNamespaceBlockBuilder {
        var prevNamespaceBlock: NewNamespaceBlockBuilder
        var currNamespaceBlock: NewNamespaceBlockBuilder? = null
        driver.getProgramStructure().use { programStructure ->
            val maybePrevNamespaceBlock = programStructure.nodes { it == NamespaceBlock.Label() }.asSequence()
                .firstOrNull {
                    it.property(FULL_NAME) == namespaceList[0] && it.property(NAME) == namespaceList[0]
                }?.let { mapToVertex(it) } as NewNamespaceBlockBuilder?
            prevNamespaceBlock = maybePrevNamespaceBlock
                ?: NewNamespaceBlockBuilder()
                    .name(namespaceList[0])
                    .fullName(namespaceList[0])
                    .filename(filename)
                    .order(1)
            if (namespaceList.size == 1) return prevNamespaceBlock

            val namespaceBuilder = StringBuilder(namespaceList[0])
            for (i in 1 until namespaceList.size) {
                namespaceBuilder.append("." + namespaceList[i])
                val order: Int
                driver.getNeighbours(prevNamespaceBlock).use { ns ->
                    order = 1 + (ns.node(prevNamespaceBlock.id())?.outE()?.asSequence()?.toList()?.size ?: 0)
                }
                val maybeCurrNamespaceBlock = programStructure.nodes { it == NamespaceBlock.Label() }.asSequence()
                    .firstOrNull {
                        it.property(FULL_NAME) == namespaceBuilder.toString() && it.property(NAME) == namespaceList[i]
                    }?.let { mapToVertex(it) } as NewNamespaceBlockBuilder?
                currNamespaceBlock = maybeCurrNamespaceBlock ?: NewNamespaceBlockBuilder()
                    .name(namespaceList[i])
                    .fullName(namespaceBuilder.toString())
                    .filename(filename)
                    .order(order)
                if (currNamespaceBlock != null) {
                    driver.addEdge(currNamespaceBlock!!, prevNamespaceBlock, AST)
                    prevNamespaceBlock = currNamespaceBlock as NewNamespaceBlockBuilder
                }
            }
        }
        return currNamespaceBlock ?: prevNamespaceBlock
    }

    /**
     * Given a type will construct a type declaration with members.
     *
     * @param type The [soot.Type] to create the declaration from.
     * @param isExternal Whether the type is part of the application or it is external.
     * @return The [NewTypeDecl] representing this newly created vertex.
     */
    fun buildTypeDeclaration(type: soot.Type, isExternal: Boolean = true): NewTypeDeclBuilder {
        val filename = if (isExternal) {
            io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN()
        } else {
            if (type.toQuotedString().contains('.')) "/${
                type.toQuotedString().replace(".", "/").removeSuffix("[]")
            }.class"
            else type.toQuotedString()
        }
        val parentType = if (type.toQuotedString().contains('.')) type.toQuotedString().substringBeforeLast(".")
        else type.toQuotedString()
        val shortName = if (type.toQuotedString().contains('.')) type.toQuotedString().substringAfterLast('.')
        else type.toQuotedString()

        return NewTypeDeclBuilder()
            .name(shortName)
            .fullName(type.toQuotedString())
            .filename(filename)
            .astParentFullName(parentType)
            .astParentType(NAMESPACE_BLOCK)
            .order(if (isExternal) -1 else 1)
            .isExternal(isExternal)
            .apply { addSootToPlumeAssociation(type, this) }
    }

    /**
     * Obtains modifiers from the given [NewTypeDeclBuilder] by finding the class in Soot and checking the corresponding
     * class' modifier information.
     *
     * @param type The [NewTypeDeclBuilder] to obtain modifiers from.
     * @return a [Set] of modifiers.
     */
    fun obtainModifiersFromTypeDeclVert(type: NewTypeDeclBuilder): Set<String> {
        Scene.v().getSootClass(type.build().fullName())?.let { return determineModifiers(it.modifiers) }
        return emptySet()
    }

    /**
     * Connects the given method's [BriefUnitGraph] to its type declaration and source file (if present).
     *
     * @param mtd The [SootMethod] to connect and extract type and source information from.
     * @param driver The [IDriver] to construct to.
     */
    fun connectMethodToTypeDecls(mtd: SootMethod, driver: IDriver) {
        Extractor.getSootAssociation(mtd.declaringClass)?.let { classVertices ->
            if (classVertices.none { it is NewTypeDeclBuilder }) return
            val typeDeclVertex = classVertices.first { it is NewTypeDeclBuilder }
            val clsVertex = classVertices.first { it is NewFileBuilder }
            val methodVertex = getMethodFromSootMethod(mtd, driver)
            if (methodVertex != null) {
                // Connect method to type declaration
                driver.addEdge(typeDeclVertex, methodVertex, AST)
                // Connect method to source file
                driver.addEdge(methodVertex, clsVertex, SOURCE_FILE)
            } else {
                logger.warn("Unable to obtain $mtd from driver while trying to connect to $typeDeclVertex.")
            }
        }
    }

    /**
     * Obtains corresponding [SootMethod] from the database.
     *
     * @param mtd The [SootMethod] to obtain from the database.
     * @param driver The [IDriver] via which the method should get obtained from.
     * @return The method vertex if found, null if otherwise.
     */
    fun getMethodFromSootMethod(mtd: SootMethod, driver: IDriver): NewMethodBuilder? {
        val fullName = "${mtd.declaringClass}.${mtd.name}"
        val signature = mtd.subSignature
        var returnMtd: NewMethodBuilder? = null
        driver.getMethod(fullName, signature).use { g ->
            if (g.nodes(METHOD).hasNext()) returnMtd = mapToVertex(g.nodes(METHOD).next()) as NewMethodBuilder
        }
        return returnMtd
    }

    private fun projectMethodReturnVertex(
        type: soot.Type,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 0
    ): NewMethodReturnBuilder =
        NewMethodReturnBuilder()
            .code(type.toQuotedString())
            .evaluationStrategy(determineEvaluationStrategy(type.toQuotedString(), true))
            .typeFullName(type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

    /**
     * Creates a [NewLiteral] from a [Constant].
     */
    fun createLiteralVertex(
        constant: Constant,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 0
    ): NewLiteralBuilder =
        NewLiteralBuilder()
            .code(constant.toString())
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(constant.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))

    /**
     * Creates a [NewIdentifier] from a [Value].
     */
    fun createIdentifierVertex(
        local: Value,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 0
    ): NewIdentifierBuilder =
        NewIdentifierBuilder()
            .code(local.toString())
            .name(local.toString())
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(local.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))

    /**
     * Creates a [NewIdentifier] from an [ArrayRef].
     */
    fun createArrayRefIdentifier(
        arrRef: ArrayRef,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 0
    ): NewIdentifierBuilder =
        NewIdentifierBuilder()
            .code(arrRef.toString())
            .name(arrRef.toString())
            .order(childIdx)
            .argumentIndex(arrRef.index.toString().toIntOrNull() ?: childIdx)
            .typeFullName(arrRef.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))

    /**
     * Creates a [NewFieldIdentifier] from a [FieldRef].
     */
    fun createFieldIdentifierVertex(
        field: FieldRef,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 0
    ): NewFieldIdentifierBuilder =
        NewFieldIdentifierBuilder()
            .canonicalName(field.field.signature)
            .code(field.field.declaration)
            .argumentIndex(childIdx)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

    fun <T> createScalaList(vararg item: T): scala.collection.immutable.List<T> {
        val list = listOf(*item)
        return CollectionConverters.ListHasAsScala(list).asScala().toList() as scala.collection.immutable.List<T>
    }
}