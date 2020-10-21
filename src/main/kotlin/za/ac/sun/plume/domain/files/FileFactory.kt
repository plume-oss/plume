package za.ac.sun.plume.domain.files

import za.ac.sun.plume.Extractor
import java.io.File

/**
 * The factory responsible for obtaining the desired [File] wrapped by its programming language wrapper determined by
 * the file extension.
 */
object FileFactory {
    /**
     * Creates a [File] given the pathname.
     *
     * @param pathname The path at which the file resides.
     * @return A [File] object if not one of the supported file types or a supported file type such as [JavaFile] or
     * [PythonFile].
     */
    @JvmStatic
    operator fun invoke(pathname: String): File {
        return when {
            pathname.endsWith(".java") -> JavaFile(pathname)
            pathname.endsWith(".py") -> PythonFile(pathname)
            pathname.endsWith(".js") -> JavaScriptFile(pathname)
            pathname.endsWith(".class") -> JVMClassFile(pathname)
            else -> File(pathname)
        }
    }

    /**
     * Creates a [File] given the pathname.
     *
     * @param f A generic [File] pointer for the file to cast.
     * @return A [File] object if not one of the supported file types or a supported file type such as [JavaFile] or
     * [PythonFile].
     */
    @JvmStatic
    operator fun invoke(f: File): File {
        return when {
            f.name.endsWith(".java") -> JavaFile(f.absolutePath)
            f.name.endsWith(".py") -> PythonFile(f.absolutePath)
            f.name.endsWith(".js") -> JavaScriptFile(f.absolutePath)
            f.name.endsWith(".class") -> JVMClassFile(f.absolutePath)
            else -> f
        }
    }
}

/**
 * The file types supported by Plume's [Extractor].
 */
enum class SupportedFile {
    /**
     * Java is a class-based, object-oriented programming language that is designed to have as few implementation
     * dependencies as possible.
     */
    JAVA,

    /**
     * Python is an interpreted, high-level and general-purpose programming language. Python's design philosophy
     * emphasizes code readability with its notable use of significant whitespace.
     */
    PYTHON,

    /**
     * JavaScript is a programming language that conforms to the ECMAScript specification. JavaScript is high-level,
     * often just-in-time compiled, and multi-paradigm. It has curly-bracket syntax, dynamic typing, prototype-based
     * object-orientation, and first-class functions.
     */
    JAVASCRIPT,

    /**
     * Java bytecode is the instruction set of the Java virtual machine.
     */
    JVM_CLASS,
}