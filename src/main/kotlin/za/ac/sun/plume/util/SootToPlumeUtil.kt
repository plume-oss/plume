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
package za.ac.sun.plume.util

import soot.*
import soot.jimple.ArrayRef
import soot.jimple.Constant
import soot.jimple.FieldRef
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.Extractor
import za.ac.sun.plume.Extractor.Companion.addSootToPlumeAssociation
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.graph.ASTBuilder
import za.ac.sun.plume.util.SootParserUtil.determineEvaluationStrategy

/**
 * A utility class of methods to convert Soot objects to [PlumeVertex] items and construct pieces of the CPG.
 */
object SootToPlumeUtil {

    /**
     * Projects member information from class field data.
     *
     * @param field The [SootField] from which the class member information is constructed from.
     */
    private fun projectMember(field: SootField) = MemberVertex(
        name = field.name,
        code = field.declaration,
        typeFullName = field.type.toQuotedString(),
        order = ASTBuilder.incOrder()
    )

    /**
     * Given an [Local], will construct method parameter information in the graph.
     *
     * @param local The [Local] from which a [MethodParameterInVertex] will be constructed.
     * @return the constructed vertex.
     */
    fun projectMethodParameterIn(local: Local, currentLine: Int) =
        MethodParameterInVertex(
            code = "${local.type} ${local.name}",
            name = local.name,
            evaluationStrategy = determineEvaluationStrategy(local.type.toString(), isMethodReturn = false),
            typeFullName = local.type.toString(),
            lineNumber = currentLine,
            order = ASTBuilder.incOrder()
        )

    /**
     * Given an [Local], will construct local variable information in the graph.
     *
     * @param local The [Local] from which a [LocalVertex] will be constructed.
     * @return the constructed vertex.
     */
    fun projectLocalVariable(local: Local, currentLine: Int, currentCol: Int) =
        LocalVertex(
            code = "${local.type} ${local.name}",
            name = local.name,
            typeFullName = local.type.toString(),
            lineNumber = currentLine,
            columnNumber = currentCol,
            order = ASTBuilder.incOrder()
        )

    /**
     * Creates the [MethodVertex] and its children vertices [MethodParameterInVertex] for parameters,
     * [MethodReturnVertex] for the formal return spec, [LocalVertex] for all local vertices, [BlockVertex] the method
     * entrypoint, and [ModifierVertex] for the modifiers.
     *
     * @param mtd The [SootMethod] from which the method and modifier information is constructed from.
     * @param driver The [IDriver] to which the method head is built.
     */
    fun buildMethodHead(mtd: SootMethod, driver: IDriver): MethodVertex {
        val methodHeadChildren = mutableListOf<PlumeVertex>()
        val currentLine = mtd.javaSourceStartLineNumber
        val currentCol = mtd.javaSourceStartColumnNumber
        // Method vertex
        val mtdVertex = MethodVertex(mtd.name,
            "${mtd.declaringClass}.${mtd.name}",
            mtd.subSignature,
            mtd.declaration,
            currentLine,
            currentCol,
            ASTBuilder.incOrder())
        methodHeadChildren.add(mtdVertex)
        // Store return type
        projectMethodReturnVertex(mtd.returnType, currentLine, currentCol)
            .apply { driver.addEdge(mtdVertex, this, EdgeLabel.AST); methodHeadChildren.add(this) }
        // Modifier vertices
        SootParserUtil.determineModifiers(mtd.modifiers, mtd.name)
            .map { ModifierVertex(it, ASTBuilder.incOrder()) }
            .forEach { driver.addEdge(mtdVertex, it, EdgeLabel.AST); methodHeadChildren.add(it) }
        // Store method vertex
        BlockVertex(ExtractorConst.ENTRYPOINT,
            ExtractorConst.VOID,
            ExtractorConst.ENTRYPOINT,
            ASTBuilder.incOrder(),
            0,
            currentLine,
            currentCol)
            .apply { driver.addEdge(mtdVertex, this, EdgeLabel.AST); methodHeadChildren.add(this) }
        // Associate all head vertices to the SootMethod
        addSootToPlumeAssociation(mtd, methodHeadChildren)
        return mtdVertex
    }

    /**
     * Given a method whose body cannot be retrieved, will construct a call-to-return graph with known information. This
     * construction includes building the type declaration and program structure.
     *
     * @param mtd The known method information to construct from.
     * @param driver The driver to construct the phantom to.
     * @return the [MethodVertex] representing the phantom method.
     */
    fun constructPhantom(mtd: SootMethod, driver: IDriver): PlumeVertex {
        val mtdVertex = buildMethodHead(mtd, driver)
        val currentLine = mtd.javaSourceStartLineNumber
        val currentCol = mtd.javaSourceStartColumnNumber
        // Connect and create parameters with placeholder names
        connectCallToReturn(mtd, driver, mtdVertex, currentLine, currentCol)
        // Create program structure
        buildClassStructure(mtd.declaringClass, driver)
        buildTypeDeclaration(mtd.declaringClass, driver)
        connectMethodToTypeDecls(mtd, driver)
        return mtdVertex
    }

    private fun connectCallToReturn(
        mtd: SootMethod,
        driver: IDriver,
        mtdVertex: MethodVertex,
        currentLine: Int,
        currentCol: Int,
        argumentIndex: Int = 0
    ) {
        mtd.parameterTypes.forEachIndexed { i, type ->
            MethodParameterInVertex(
                code = "$type param$i",
                name = "param$i",
                evaluationStrategy = determineEvaluationStrategy(type.toString(), isMethodReturn = false),
                typeFullName = type.toString(),
                lineNumber = mtd.javaSourceStartLineNumber,
                order = ASTBuilder.incOrder()
            ).apply { driver.addEdge(mtdVertex, this, EdgeLabel.AST); addSootToPlumeAssociation(mtd, this) }
        }
        // Connect a call to return
        val entryPoint = Extractor.getSootAssociation(mtd)?.filterIsInstance<BlockVertex>()?.firstOrNull()
        val mtdReturn = Extractor.getSootAssociation(mtd)?.filterIsInstance<MethodReturnVertex>()?.firstOrNull()
        ReturnVertex(
            lineNumber = currentLine,
            columnNumber = currentCol,
            order = ASTBuilder.incOrder(),
            argumentIndex = argumentIndex,
            code = "return ${mtd.returnType.toQuotedString()}"
        ).apply {
            driver.addEdge(entryPoint!!, this, EdgeLabel.CFG)
            driver.addEdge(this, mtdReturn!!, EdgeLabel.CFG)
        }
    }

    /**
     * Constructs the file, package, and type information from the given [SootClass].
     *
     * @param cls The [SootClass] from which the file and package information is constructed from.
     */
    fun buildClassStructure(cls: SootClass, driver: IDriver): FileVertex {
        val classChildrenVertices = mutableListOf<PlumeVertex>()
        val fileHash = Extractor.getFileHashPair(cls)
        var nbv: NamespaceBlockVertex? = null
        if (cls.packageName.isNotEmpty()) {
            // Populate namespace block chain
            val namespaceList = cls.packageName.split(".").toTypedArray()
            if (namespaceList.isNotEmpty()) nbv = populateNamespaceChain(namespaceList, driver)
        }
        return FileVertex(cls.name, fileHash.toString(), ASTBuilder.incOrder()).apply {
            // Join FILE and NAMESPACE_BLOCK if namespace is present
            if (nbv != null) {
                driver.addEdge(this, nbv, EdgeLabel.AST); classChildrenVertices.add(nbv)
            }
            classChildrenVertices.add(0, this)
            addSootToPlumeAssociation(cls, classChildrenVertices)
        }
    }

    /**
     * Creates a change of [NamespaceBlockVertex]s and returns the final one in the chain.
     *
     * @param namespaceList A list of package names.
     * @return The final [NamespaceBlockVertex] in the chain (the one associated with the file).
     */
    private fun populateNamespaceChain(namespaceList: Array<String>, driver: IDriver): NamespaceBlockVertex {
        var prevNamespaceBlock = NamespaceBlockVertex(namespaceList[0], namespaceList[0], ASTBuilder.incOrder())
        if (namespaceList.size == 1) return prevNamespaceBlock

        var currNamespaceBlock: NamespaceBlockVertex? = null
        val namespaceBuilder = StringBuilder(namespaceList[0])
        for (i in 1 until namespaceList.size) {
            namespaceBuilder.append("." + namespaceList[i])
            currNamespaceBlock =
                NamespaceBlockVertex(namespaceList[i], namespaceBuilder.toString(), ASTBuilder.incOrder())
            driver.addEdge(currNamespaceBlock, prevNamespaceBlock, EdgeLabel.AST)
            prevNamespaceBlock = currNamespaceBlock
        }
        return currNamespaceBlock ?: prevNamespaceBlock
    }

    /**
     * Given a class will construct a type declaration with members.
     *
     * @param cls The [SootClass] to create the declaration from.
     * @param driver The driver to construct the type declaration to.
     * @return The [TypeDeclVertex] representing this newly created vertex.
     */
    fun buildTypeDeclaration(cls: SootClass, driver: IDriver) = TypeDeclVertex(
        name = cls.shortName,
        fullName = cls.name,
        typeDeclFullName = cls.javaStyleName,
        order = ASTBuilder.incOrder()
    ).apply {
        // Attach fields to the TypeDecl
        cls.fields.forEach { field ->
            projectMember(field).let { memberVertex ->
                driver.addEdge(this, memberVertex, EdgeLabel.AST)
                addSootToPlumeAssociation(field, memberVertex)
            }
        }
        addSootToPlumeAssociation(cls, this)
    }

    /**
     * Connects the given method's [BriefUnitGraph] to its type declaration and source file (if present).
     *
     * @param mtd The [SootMethod] to connect and extract type and source information from.
     * @param driver The [IDriver] to construct to.
     */
    fun connectMethodToTypeDecls(mtd: SootMethod, driver: IDriver) {
        Extractor.getSootAssociation(mtd.declaringClass)?.let { classVertices ->
            val typeDeclVertex = classVertices.first { it is TypeDeclVertex }
            val clsVertex = classVertices.first { it is FileVertex }
            val methodVertex = Extractor.getSootAssociation(mtd)?.first { it is MethodVertex } as MethodVertex
            // Connect method to type declaration
            driver.addEdge(typeDeclVertex, methodVertex, EdgeLabel.AST)
            // Connect method to source file
            driver.addEdge(methodVertex, clsVertex, EdgeLabel.SOURCE_FILE)
        }
    }

    private fun projectMethodReturnVertex(type: Type, currentLine: Int, currentCol: Int) =
        MethodReturnVertex(
            name = ExtractorConst.RETURN,
            code = "return ${type.toQuotedString()}",
            evaluationStrategy = determineEvaluationStrategy(type.toQuotedString(), true),
            typeFullName = type.toQuotedString(),
            lineNumber = currentLine,
            columnNumber = currentCol,
            order = ASTBuilder.incOrder()
        )

    /**
     * Creates a [LiteralVertex] from a [Constant].
     */
    fun createLiteralVertex(constant: Constant, currentLine: Int, currentCol: Int, argumentIndex: Int = 0) =
        LiteralVertex(
            name = constant.toString(),
            code = constant.toString(),
            order = ASTBuilder.incOrder(),
            argumentIndex = argumentIndex,
            typeFullName = constant.type.toQuotedString(),
            lineNumber = currentLine,
            columnNumber = currentCol
        )

    /**
     * Creates a [IdentifierVertex] from a [Value].
     */
    fun createIdentifierVertex(local: Value, currentLine: Int, currentCol: Int, argumentIndex: Int = 0) =
        IdentifierVertex(
            code = "$local",
            name = local.toString(),
            order = ASTBuilder.incOrder(),
            argumentIndex = argumentIndex,
            typeFullName = local.type.toQuotedString(),
            lineNumber = currentLine,
            columnNumber = currentCol
        )

    /**
     * Creates a [IdentifierVertex] from an [ArrayRef].
     */
    fun createArrayRefIdentifier(leftOp: ArrayRef, currentLine: Int, currentCol: Int, argumentIndex: Int = 0) =
        IdentifierVertex(
            code = "$leftOp",
            name = leftOp.toString(),
            order = ASTBuilder.incOrder(),
            argumentIndex = leftOp.index.toString().toIntOrNull() ?: argumentIndex,
            typeFullName = leftOp.type.toQuotedString(),
            lineNumber = currentLine,
            columnNumber = currentCol
        )

    /**
     * Creates a [FieldIdentifierVertex] from a [FieldRef].
     */
    fun createFieldIdentifierVertex(field: FieldRef, currentLine: Int, currentCol: Int, argumentIndex: Int = 0) =
        FieldIdentifierVertex(
            canonicalName = field.field.signature,
            code = field.field.declaration,
            argumentIndex = argumentIndex,
            lineNumber = currentLine,
            columnNumber = currentCol,
            order = ASTBuilder.incOrder()
        )
}