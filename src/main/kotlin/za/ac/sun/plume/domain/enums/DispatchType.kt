package za.ac.sun.plume.domain.enums

/**
 * The dispatch type of a call, which is either static or dynamic.
 */
enum class DispatchType {
    /**
     * For statically dispatched calls the call target is known before program execution
     */
    STATIC_DISPATCH,

    /**
     * For dynamically dispatched calls the target is determined during runtime
     */
    DYNAMIC_DISPATCH,
}