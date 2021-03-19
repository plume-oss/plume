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

import io.github.plume.oss.domain.mappers.ListMapper
import io.github.plume.oss.domain.mappers.VertexMapper.mapToVertex
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.SootParserUtil.determineEvaluationStrategy
import io.shiftleft.codepropertygraph.generated.EvaluationStrategies.BY_SHARING
import io.shiftleft.codepropertygraph.generated.NodeTypes.METHOD
import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import scala.Option
import soot.SootClass
import soot.SootMethod
import soot.Type
import soot.Value
import soot.jimple.*
import java.io.File

/**
 * A utility class of methods to convert Soot objects to [NewNodeBuilder] items and construct pieces of the CPG.
 */
object SootToPlumeUtil {

    private val logger = LogManager.getLogger(SootToPlumeUtil::class.java)

    /**
     * Given an [soot.Local], will construct method parameter in information in the graph.
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
            .code("${local.type.toQuotedString()} ${local.name}")
            .evaluationStrategy(determineEvaluationStrategy(local.type.toString(), isMethodReturn = false))
            .typeFullName(local.type.toString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

    /**
     * Given an [soot.Local], will construct method parameter out information in the graph.
     *
     * @param local The [soot.Local] from which a [NewMethodParameterOutBuilder] will be constructed.
     * @return the constructed vertex.
     */
    fun projectMethodParameterOut(
        local: soot.Local,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int
    ): NewMethodParameterOutBuilder =
        NewMethodParameterOutBuilder()
            .name(local.name)
            .code("${local.type.toQuotedString()} ${local.name}")
            .evaluationStrategy(BY_SHARING)
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

    fun methodToStrings(mtd: SootMethod): Triple<String, String, String> {
        val signature = "${mtd.returnType}(${mtd.parameterTypes.joinToString(separator = ",")})"
        val code = "${mtd.returnType} ${mtd.name}(${
            mtd.parameterTypes.zip(1..mtd.parameterCount).joinToString { (p, i) -> "$p param$i" }
        })"
        val fullName = "${mtd.declaringClass}.${mtd.name}:$signature"
        return Triple(fullName, signature, code)
    }

    /**
     * New expressions are specific to OOP languages and are thus Unknown nodes.
     */
    fun createNewExpr(expr: NewExpr, currentLine: Int, currentCol: Int, childIdx: Int): NewUnknownBuilder {
        return NewUnknownBuilder()
            .typeFullName(expr.baseType.toQuotedString())
            .code(expr.toString())
            .argumentIndex(childIdx)
            .order(childIdx)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
    }

    /**
     * Derive a file name from an object of type `SootClass`
     * @param cls the soot class
     * @return the filename in string form
     * */
    fun sootClassToFileName(cls: SootClass): String {
        val packageName = cls.packageName
        return if (packageName != null) {
            File.separator + cls.name.replace(".", File.separator) + ".class"
        } else {
            io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN()
        }
    }

    /**
     * Creates a [NewLiteral] from a [Constant].
     */
    fun createLiteralVertex(
        constant: Constant,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 1
    ): NewLiteralBuilder =
        NewLiteralBuilder()
            .code(constant.toString())
            .order(childIdx)
            .argumentIndex(childIdx)
            .typeFullName(constant.type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))

    /**
     * Creates a [NewTypeRef] from a [Value].
     */
    fun createTypeRefVertex(
        type: Type,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 1
    ): NewTypeRefBuilder =
        NewTypeRefBuilder()
            .code(type.toString())
            .order(childIdx)
            .argumentIndex(childIdx)
            .dynamicTypeHintFullName(ListMapper.stringToScalaList(type.toQuotedString()))
            .typeFullName(type.toQuotedString())
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))


    /**
     * Creates a [NewIdentifier] from a [Value].
     */
    fun createIdentifierVertex(
        local: Value,
        currentLine: Int,
        currentCol: Int,
        childIdx: Int = 1
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
        childIdx: Int = 1
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
        childIdx: Int = 1
    ): NewFieldIdentifierBuilder =
        NewFieldIdentifierBuilder()
            .canonicalName(field.field.signature)
            .code(field.field.name)
            .argumentIndex(childIdx)
            .lineNumber(Option.apply(currentLine))
            .columnNumber(Option.apply(currentCol))
            .order(childIdx)

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