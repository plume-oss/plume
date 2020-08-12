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
package za.ac.sun.plume.visitors.init

import org.apache.logging.log4j.LogManager
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import za.ac.sun.plume.domain.meta.ClassInfo
import za.ac.sun.plume.domain.meta.MetaDataCollector

class InitialClassVisitor(private val classMetaController: MetaDataCollector) : ClassVisitor(Opcodes.ASM5), Opcodes {

    private val logger = LogManager.getLogger()
    private var classInfo: ClassInfo? = null

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>) {
        super.visit(version, access, name, signature, superName, interfaces)
        classInfo = classMetaController.putClass(name, access, version)
        logger.debug("")
        logger.debug(classInfo.toString() + " extends " + superName + " {")
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        val methodInfo = classInfo!!.addMethod(name, descriptor, access, -1)
        logger.debug("")
        logger.debug("\t $methodInfo {")
        return InitialMethodVisitor(mv, methodInfo)
    }

    override fun visitEnd() {
        logger.debug("")
        logger.debug("}")
        super.visitEnd()
    }

}