package za.ac.sun.plume.domain.meta

import za.ac.sun.plume.controllers.MethodInfoController
import za.ac.sun.plume.util.ASMParserUtil

class ClassInfo(
        val className: String,
        val namespace: String,
        private val access: Int,
        val version: Int
) {
    private val classMethods = mutableListOf<MethodInfoController>()

    fun addMethod(methodName: String, methodSignature: String, access: Int, lineNumber: Int): MethodInfoController {
        val methodInfo = MethodInfoController(methodName, methodSignature, access, lineNumber)
        classMethods.add(methodInfo)
        return methodInfo
    }

    fun getMethod(methodName: String, methodSignature: String, access: Int): MethodInfoController? {
        val hashCode = 31 * (31 * methodName.hashCode() + methodSignature.hashCode()) + access.hashCode()
        return classMethods.find { methodInfo -> methodInfo.hashCode() ==  hashCode }
    }

    fun clear() {
        classMethods.clear()
    }

    override fun toString(): String {
        return "${ASMParserUtil.determineModifiers(access, className)} ${namespace}.${className}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassInfo

        if (className != other.className) return false
        if (namespace != other.namespace) return false

        return true
    }

    override fun hashCode(): Int {
        var result = className.hashCode()
        result = 31 * result + namespace.hashCode()
        return result
    }
}