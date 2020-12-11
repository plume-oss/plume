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
    const val ASSIGN = "ASSIGN"
    const val CAST = "CAST"
    const val ENTRYPOINT = "BODY"
    const val IF_ROOT = "IF"
    const val TABLE_SWITCH = "TABLE_SWITCH"
    const val LOOKUP_ROOT = "LOOKUP_SWITCH"
    const val TRUE_TARGET = "TRUE"
    const val FALSE_TARGET = "FALSE"

    // return text
    const val RETURN = "RETURN"

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
                "~" to "COMP",
                "<<" to "SHL",
                ">>" to "SHR",
                ">>>" to "USHR",
                "==" to "EQ",
                "<" to "LT",
                ">" to "GT",
                "!=" to "NEQ",
                "<=" to "LTE",
                ">=" to "GTE"
        )
    }
}