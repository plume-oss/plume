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
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import scala.Option
import scala.jdk.CollectionConverters
import soot.*
import soot.jimple.*
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

    fun parseMethodToStrings(mtd: SootMethod): Triple<String, String, String> {
        val signature = "${mtd.returnType}(${mtd.parameterTypes.joinToString(separator = ",")})"
        val code = "${mtd.returnType} ${mtd.name}(${
            mtd.parameterTypes.zip(1..mtd.parameterCount).joinToString() { (p, i) -> "$p param$i" }
        })"
        val fullName = "${mtd.declaringClass}.${mtd.name}:$signature"
        return Triple(fullName, signature, code)
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
     * Connects the given method's [BriefUnitGraph] to its type declaration and source file (if present).
     *
     * @param mtd The [SootMethod] to connect and extract type and source information from.
     * @param driver The [IDriver] to construct to.
     */
    private fun connectMethodToTypeDecls(mtd: SootMethod, driver: IDriver) {
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
        val (fullName, _, _) = parseMethodToStrings(mtd)
        var returnMtd: NewMethodBuilder? = null
        driver.getMethod(fullName).use { g ->
            if (g.nodes(METHOD).hasNext()) returnMtd = mapToVertex(g.nodes(METHOD).next()) as NewMethodBuilder
        }
        return returnMtd
    }

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

    fun parseBinopExpr(op: BinopExpr): String = parseBinopExpr(op.symbol.trim())

    fun parseBinopExpr(sym: String): String {
        return when {
            sym.contains("cmp") -> Operators.compare
            sym == "+" -> Operators.plus
            sym == "-" -> Operators.minus
            sym == "/" -> Operators.division
            sym == "*" -> Operators.multiplication
            sym == "%" -> Operators.modulo
            sym == "&" -> Operators.logicalAnd
            sym == "&&" -> Operators.and
            sym == "|" -> Operators.logicalOr
            sym == "||" -> Operators.or
            sym == "^" -> Operators.xor
            sym == "<<" -> Operators.shiftLeft
            sym == ">>" -> Operators.logicalShiftRight
            sym == ">>>" -> Operators.arithmeticShiftRight
            sym == "==" -> Operators.equals
            sym == "<" -> Operators.lessThan
            sym == ">" -> Operators.greaterThan
            sym == "!=" -> Operators.notEquals
            sym == "~" -> Operators.logicalNot
            sym == "<=" -> Operators.lessEqualsThan
            sym == ">=" -> Operators.greaterEqualsThan
            else -> {
                logger.warn("Unknown binary operator $sym")
                sym
            }
        }
    }
}