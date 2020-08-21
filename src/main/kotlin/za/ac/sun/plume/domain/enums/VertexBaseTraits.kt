package za.ac.sun.plume.domain.enums

/**
 * Common base traits for nodes.
 */
enum class VertexBaseTraits {
    /**
     * Any node that can exist in an abstract syntax tree
     */
    AST_NODE,

    /**
     * Any node that can occur as part of a control flow graph
     */
    CFG_NODE,

    /**
     * Declare a variable by specifying its data type and name
     */
    DECLARATION,

    /**
     * Formal input parameters, locals, and identifiers
     */
    LOCAL_LIKE,

    /**
     * Any node that can occur in a data flow
     */
    TRACKING_POINT,

    /**
     * Expression as a specialisation of tracking point
     */
    EXPRESSION,

    /**
     * Any node that can exist in a method
     */
    WITHIN_METHOD,

    /**
     * Call representation
     */
    CALL_REPR
}