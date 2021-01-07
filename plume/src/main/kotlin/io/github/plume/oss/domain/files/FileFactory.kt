package io.github.plume.oss.domain.files

import net.jpountz.xxhash.StreamingXXHash32
import net.jpountz.xxhash.XXHashFactory
import io.github.plume.oss.Extractor
import java.io.File
import java.io.FileInputStream

/**
 * The factory responsible for obtaining the desired [File] wrapped by its programming language wrapper determined by
 * the file extension.
 */
object FileFactory {
    /**
     * Creates a [File] given the pathname.
     *
     * @param pathname The path at which the file resides.
     * @return A [File] object if not one of the supported file types or a supported file type such as [JavaFile].
     */
    @JvmStatic
    operator fun invoke(pathname: String): PlumeFile {
        return when {
            pathname.endsWith(".java") -> JavaFile(pathname)
            pathname.endsWith(".class") -> JVMClassFile(pathname)
            else -> UnsupportedFile(pathname)
        }
    }

    /**
     * Creates a [File] given the pathname.
     *
     * @param f A generic [File] pointer for the file to cast.
     * @return A [File] object if not one of the supported file types or a supported file type such as [JavaFile].
     */
    @JvmStatic
    operator fun invoke(f: File): PlumeFile {
        return when {
            f.name.endsWith(".java") -> JavaFile(f.absolutePath)
            f.name.endsWith(".class") -> JVMClassFile(f.absolutePath)
            else -> UnsupportedFile(f.absolutePath)
        }
    }

    /**
     * Will ingest a file's contents and return the xxHash32 representation. See
     * [xxHash](https://cyan4973.github.io/xxHash/) for more information.
     *
     * @param f The file to hash.
     * @return The given file's xxHash32 representation
     */
    fun getFileHash(f: File): Int {
        val factory = XXHashFactory.fastestInstance()
        FileInputStream(f).use { inStream ->
            val seed = -0x68b84d74
            val hash32: StreamingXXHash32 = factory.newStreamingHash32(seed)
            val buf = ByteArray(8192)
            while (true) {
                val read = inStream.read(buf)
                if (read == -1) {
                    break
                }
                hash32.update(buf, 0, read)
            }
            return hash32.value
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
     * Java bytecode is the instruction set of the Java virtual machine.
     */
    JVM_CLASS,
}