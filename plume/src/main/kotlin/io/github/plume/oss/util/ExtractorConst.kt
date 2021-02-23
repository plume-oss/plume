package io.github.plume.oss.util

object ExtractorConst {
    const val LANGUAGE_FRONTEND = "Plume"
    const val LANGUAGE_FRONTEND_VERSION = "0.1"
    val PRIMITIVES = listOf("boolean", "byte", "char", "double", "float", "int", "long", "null", "short", "void")
    val BIN_OPS: Map<String, String> = mapOf(
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
        ">=" to "GTE",
        "cmp" to "CMP"
    )

    // block bodies
    const val CAST = "CAST"
    const val ENTRYPOINT = "BODY"
    const val TRUE_TARGET = "TRUE"
    const val FALSE_TARGET = "FALSE"
}