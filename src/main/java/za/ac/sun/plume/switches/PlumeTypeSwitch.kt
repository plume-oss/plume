/*
 * Copyright 2020 Wim Keirsgieter & David Baker Effendi
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
package za.ac.sun.plume.switches

import soot.*
import za.ac.sun.plume.util.ExtractorConst.BOOLEAN
import za.ac.sun.plume.util.ExtractorConst.BYTE
import za.ac.sun.plume.util.ExtractorConst.CHAR
import za.ac.sun.plume.util.ExtractorConst.DOUBLE
import za.ac.sun.plume.util.ExtractorConst.FLOAT
import za.ac.sun.plume.util.ExtractorConst.INT
import za.ac.sun.plume.util.ExtractorConst.LONG
import za.ac.sun.plume.util.ExtractorConst.NULL
import za.ac.sun.plume.util.ExtractorConst.SHORT
import za.ac.sun.plume.util.ExtractorConst.VOID

class PlumeTypeSwitch : TypeSwitch() {

    override fun caseArrayType(type: ArrayType) {
        val typeVisitor = PlumeTypeSwitch()
        type.baseType.apply(typeVisitor)
        val sb: StringBuilder = StringBuilder(typeVisitor.result.toString())
        for (i in 0 until type.numDimensions) {
            sb.append("[]")
        }
        result = sb.toString()
    }

    override fun caseBooleanType(type: BooleanType) {
        result = BOOLEAN
    }

    override fun caseByteType(type: ByteType) {
        result = BYTE
    }

    override fun caseCharType(type: CharType) {
        result = CHAR
    }

    override fun caseDoubleType(type: DoubleType) {
        result = DOUBLE
    }

    override fun caseFloatType(type: FloatType) {
        result = FLOAT
    }

    override fun caseIntType(type: IntType) {
        result = INT
    }

    override fun caseLongType(type: LongType) {
        result = LONG
    }

    override fun caseNullType(type: NullType) {
        result = NULL
    }

    override fun caseRefType(type: RefType) {
        result = type.className
    }

    override fun caseVoidType(type: VoidType?) {
        result = VOID
    }

    override fun caseShortType(type: ShortType?) {
        result = SHORT
    }

    override fun defaultCase(type: Type) {
        result = type.toString()
    }
}
