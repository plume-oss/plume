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
package za.ac.sun.plume.visitors.ast

import org.apache.logging.log4j.LogManager
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import za.ac.sun.plume.controllers.ASTController
import za.ac.sun.plume.visitors.OpStackMethodVisitor

class ASTMethodVisitor(
        mv: MethodVisitor?,
        private val astController: ASTController
) : OpStackMethodVisitor(mv, astController), Opcodes {

    private val logger = LogManager.getLogger()

    override fun visitCode() {
        super.visitCode()
        astController.methodInfo.initializeMethod()
    }

    override fun visitLabel(label: Label) {
        super.visitLabel(label)
        astController.methodInfo.pushNewLabel(label)
    }

    override fun visitLineNumber(line: Int, start: Label) {
        super.visitLineNumber(line, start)
        astController.associateLineNumberWithLabel(line, start)
    }

    override fun visitEnd() {
        super.visitEnd()
        logger.debug(astController.toString())
    }

}