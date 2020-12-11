package za.ac.sun.plume.domain.enums

/**
 * Types of vertices
 */
enum class VertexLabel {
    /**
     * Node to save meta data about the graph on its properties. Exactly one node of this type per graph.
     */
    META_DATA,

    /**
     * Node representing a source file. Often also the AST root.
     */
    FILE,

    /**
     * A method/function/procedure.
     */
    METHOD,

    /**
     * This node represents a formal parameter going towards the callee side.
     */
    METHOD_PARAMETER_IN,

    /**
     * A formal method return.
     */
    METHOD_RETURN,

    /**
     * A modifier, e.g., static, public, private.
     */
    MODIFIER,

    /**
     * A type which always has to reference a type declaration and may have type argument children if the referred to type declaration is a template.
     */
    TYPE,

    /**
     * A type declaration.
     */
    TYPE_DECL,

    /**
     * A binding of a METHOD into a TYPE_DECL.
     */
    BINDING,

    /**
     * Type parameter of TYPE_DECL or METHOD.
     */
    TYPE_PARAMETER,

    /**
     * Argument for a TYPE_PARAMETER that belongs to a TYPE. It binds another TYPE to a TYPE_PARAMETER.
     */
    TYPE_ARGUMENT,

    /**
     * Member of a class struct or union.
     */
    MEMBER,

    /**
     * A reference to a namespace.
     */
    NAMESPACE_BLOCK,

    /**
     * Literal/Constant.
     */
    LITERAL,

    /**
     * A (method)-call.
     */
    CALL,

    /**
     * A local variable.
     */
    LOCAL,

    /**
     * An arbitrary identifier/reference.
     */
    IDENTIFIER,

    /**
     * A node that represents which field is accessed in a <operator>.fieldAccess, in e.g. obj.field. The CODE part is
     * used for human display and matching to MEMBER nodes. The CANONICAL_NAME is used for dataflow tracking; typically
     * both coincide. However, suppose that two fields foo and bar are a C-style union; then CODE refers to whatever the
     * programmer wrote (obj.foo or obj.bar), but both share the same CANONICAL_NAME (e.g. GENERATED_foo_bar).
     */
    FIELD_IDENTIFIER,

    /**
     * A return instruction.
     */
    RETURN,

    /**
     * A structuring block in the AST.
     */
    BLOCK,

    /**
     * Initialization construct for arrays.
     */
    ARRAY_INITIALIZER,

    /**
     * Reference to a method instance.
     */
    METHOD_REF,

    /**
     * Reference to a type/class.
     */
    TYPE_REF,

    /**
     * A control structure such as if, while, or for.
     */
    CONTROL_STRUCTURE,

    /**
     * A jump target made explicit in the code using a label.
     */
    JUMP_TARGET,

    /**
     * A language-specific node.
     */
    UNKNOWN,
}