/*
 * Copyright 2021 Plume Authors
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

import io.shiftleft.codepropertygraph.generated.Operators
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import org.apache.logging.log4j.LogManager
import soot.SootClass
import soot.SootMethod
import soot.jimple.BinopExpr
import java.io.File

/**
 * A utility class of methods to convert Soot objects to [NewNodeBuilder] items and construct pieces of the CPG.
 */
object SootToPlumeUtil {

    private val logger = LogManager.getLogger(SootToPlumeUtil::class.java)

    fun methodToStrings(mtd: SootMethod): Triple<String, String, String> {
        val signature = "${mtd.returnType}(${mtd.parameterTypes.joinToString(separator = ",")})"
        val code = "${mtd.returnType} ${mtd.name}(${
            mtd.parameterTypes.zip(1..mtd.parameterCount).joinToString { (p, i) -> "$p param$i" }
        })"
        val fullName = "${mtd.declaringClass}.${mtd.name}:$signature"
        return Triple(fullName, signature, code)
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

    fun parseBinopExpr(op: BinopExpr): String = parseBinopExpr(op.symbol.trim())

    fun parseBinopExpr(sym: String): String {
        return when {
            sym.contains("cmp") -> Operators.compare
            sym == "+" -> Operators.addition
            sym == "-" -> Operators.subtraction
            sym == "/" -> Operators.division
            sym == "*" -> Operators.multiplication
            sym == "%" -> Operators.modulo
            sym == "&" -> Operators.and
            sym == "&&" -> Operators.logicalAnd
            sym == "|" -> Operators.or
            sym == "||" -> Operators.logicalOr
            sym == "^" -> Operators.xor
            sym == "<<" -> Operators.shiftLeft
            sym == ">>" -> Operators.arithmeticShiftRight
            sym == ">>>" -> Operators.logicalShiftRight
            sym == "==" -> Operators.equals
            sym == "<" -> Operators.lessThan
            sym == ">" -> Operators.greaterThan
            sym == "!=" -> Operators.notEquals
            sym == "~" -> Operators.not
            sym == "!" -> Operators.logicalNot
            sym == "<=" -> Operators.lessEqualsThan
            sym == ">=" -> Operators.greaterEqualsThan
            sym == "-=" -> Operators.assignmentMinus
            sym == "+=" -> Operators.assignmentPlus
            sym == "/=" -> Operators.assignmentDivision
            sym == "*=" -> Operators.assignmentMultiplication
            sym == "%=" -> Operators.assignmentModulo
            sym == "<<=" -> Operators.assignmentShiftLeft
            sym == ">>=" -> Operators.assignmentArithmeticShiftRight
            sym == ">>>=" -> Operators.logicalShiftRight
            sym == "|=" -> Operators.assignmentOr
            sym == "&=" -> Operators.assignmentAnd
            sym == "^=" -> Operators.assignmentXor
            else -> {
                logger.warn("Unknown binary operator $sym")
                sym
            }
        }
    }
}