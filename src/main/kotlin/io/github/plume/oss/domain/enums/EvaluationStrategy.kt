package io.github.plume.oss.domain.enums

/**
 * Evaluation strategy for function parameters and return values.
 */
enum class EvaluationStrategy {
    /**
     * A parameter or return of a function is passed by reference which means an address is used behind the scenes
     */
    BY_REFERENCE,

    /**
     * Only applicable to object parameter or return values. The pointer to the object is passed by value but the object
     * itself is not copied and changes to it are thus propagated out of the method context
     */
    BY_SHARING,

    /**
     * A parameter or return of a function passed by value which means a flat copy is used
     */
    BY_VALUE,
}