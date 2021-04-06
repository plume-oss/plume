package io.github.plume.oss.util

import net.jpountz.xxhash.StreamingXXHash32
import net.jpountz.xxhash.XXHashFactory
import soot.Body
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Provides methods to calculate the [xxHash](https://cyan4973.github.io/xxHash/) from given arguments.
 */
object HashUtil {

    private val factory: XXHashFactory by lazy { XXHashFactory.fastestInstance() }

    /**
     * Will ingest a file's contents and return the xxHash32 representation.
     *
     * @param f The file to hash.
     * @return The given file's xxHash32 representation
     */
    fun getFileHash(f: File) = getHashFromInputStream(FileInputStream(f))

    /**
     * Will ingest a Jimple method body and return the xxHash32 representation.
     *
     * @param body The method body to hash.
     * @return The given file's xxHash32 representation
     */
    fun getMethodHash(body: Body) = getHashFromInputStream(body.toString().byteInputStream())

    private fun getHashFromInputStream(stream: InputStream): Int {
        stream.use { inStream ->
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