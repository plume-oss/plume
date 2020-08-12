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

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import za.ac.sun.plume.controllers.ASTController
import za.ac.sun.plume.domain.meta.ClassInfo
import za.ac.sun.plume.domain.meta.MetaDataCollector

class ASTClassVisitor(private val classMetaController: MetaDataCollector, private val astController: ASTController) : ClassVisitor(Opcodes.ASM5), Opcodes {

    private var classInfo: ClassInfo? = null

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<String>) {
        super.visit(version, access, name, signature, superName, interfaces)
        classInfo = classMetaController.getClass(name)
        astController.projectClassData(classInfo!!)
        // TODO: Could create MEMBER vertex from here to declare member classes
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        val methodInfo = classInfo!!.getMethod(name, descriptor, access)!!
        astController.pushNewMethod(methodInfo)
        return ASTMethodVisitor(mv, astController)
    }

}