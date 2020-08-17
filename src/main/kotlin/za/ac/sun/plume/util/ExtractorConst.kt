package za.ac.sun.plume.util

object ExtractorConst {
    val PRIMITIVES: List<String>
    val BINOPS: Map<String, String>

    // primitive types and literals
    const val BOOLEAN = "boolean"
    const val BYTE = "byte"
    const val CHAR = "char"
    const val DOUBLE = "double"
    const val FLOAT = "float"
    const val INT = "int"
    const val LONG = "long"
    const val NULL = "null"
    const val SHORT = "short"
    const val VOID = "void"

    // block bodies
    const val STORE = "STORE"
    const val CAST = "CAST"
    const val METHOD_BODY = "BODY"
    const val IF_ROOT = "IF"
    const val IF_BODY = "IF_BODY"
    const val ELSE_BODY = "ELSE_BODY"

    init {
        PRIMITIVES = listOf(BOOLEAN, BYTE, CHAR, DOUBLE, FLOAT, INT, LONG, NULL, SHORT, VOID)
        BINOPS = mapOf(
                "+" to "ADD",
                "-" to "SUB",
                "/" to "DIV",
                "*" to "MUL",
                "%" to "REM",
                "&" to "AND",
                "|" to "OR",
                "^" to "XOR",
                "~" to "COMPLIMENT",
                "<<" to "SHL",
                ">>" to "SHR",
                ">>>" to "USHR"
        )
    }
}