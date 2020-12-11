package za.ac.sun.plume.domain.exceptions

/**
 * Thrown to indicate that Plume was unable to find a suitable compiler to compile the given source code.
 *
 * @param message a custom message to substitute the exception.
 */
class PlumeCompileException(message: String) : RuntimeException(message)