package za.ac.sun.plume.domain.enums

/**
 * Types of edges.
 */
enum class EdgeLabels {
    /**
     * Syntax tree edge
     */
    AST,

    /**
     * Control flow edge
     */
    CFG,

    /**
     * Connection between a captured LOCAL and the corresponding CLOSURE_BINDING
     */
    CAPTURED_BY,

    /**
     * Type argument binding to a type parameter
     */
    BINDS_TO,

    /**
     * A reference to e.g. a LOCAL
     */
    REF,

    /**
     * The receiver of a method call which is either an object or a pointer
     */
    RECEIVER,

    /**
     * Edge from control structure node to the expression that holds the condition
     */
    CONDITION,

    /**
     * Relation between TYPE_DECL and BINDING node
     */
    BINDS,

    /**
     * Relation between a CALL and its arguments and RETURN and the returned expression
     */
    ARGUMENT,
}