package za.ac.sun.plume.domain.meta

class MetaDataCollector {

    private val allClasses = HashSet<ClassInfo>()

    fun putClass(fullName: String, access: Int, version: Int): ClassInfo {
        val classInfo = ClassInfo(obtainClassName(fullName), obtainNamespace(fullName), access, version)
        allClasses.add(classInfo)
        return classInfo
    }

    fun getClass(fullName: String): ClassInfo? {
        var hashCode = obtainClassName(fullName).hashCode()
        hashCode = 31 * hashCode + obtainNamespace(fullName).hashCode()
        return allClasses.firstOrNull { it.hashCode() == hashCode }
    }

    private fun obtainClassName(fullName: String): String {
        return if (fullName.lastIndexOf('/') != -1) {
            fullName.substring(fullName.lastIndexOf('/') + 1)
        } else {
            fullName
        }
    }

    private fun obtainNamespace(fullName: String): String {
        return if (fullName.lastIndexOf('/') != -1) {
            fullName.substring(0, fullName.lastIndexOf('/')).replace("/".toRegex(), ".")
        } else {
            ""
        }
    }

}