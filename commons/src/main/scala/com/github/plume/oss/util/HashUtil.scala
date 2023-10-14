package com.github.plume.oss.util

import net.jpountz.xxhash.{StreamingXXHash32, XXHashFactory}

import java.io.{File, FileInputStream, InputStream}
import scala.util.Using

/** Provides methods to calculate the [[https://cyan4973.github.io/xxHash/ xxHash]] from given arguments.
  */
object HashUtil {

  private val factory: XXHashFactory = XXHashFactory.fastestInstance()

  /** Will ingest a file's contents and return the xxHash32 representation.
    *
    * @param f The file to hash.
    * @return The given file's xxHash32 representation
    */
  def getFileHash(f: File): String = getHashFromInputStream(new FileInputStream(f)).toString

  /** Will ingest an input stream and return the xxHash32 representation.
    *
    * @param stream The stream to hash.
    * @return The given stream's xxHash32 representation
    */
  def getHashFromInputStream(stream: InputStream): Int = {
    Using.resource(stream) { inStream =>
      val seed                      = -0x68b84d74
      val hash32: StreamingXXHash32 = factory.newStreamingHash32(seed)
      val buf                       = new Array[Byte](8192)
      Iterator
        .continually(inStream.read(buf))
        .takeWhile(_ != -1)
        .foreach(hash32.update(buf, 0, _))
      return hash32.getValue
    }
  }

}
