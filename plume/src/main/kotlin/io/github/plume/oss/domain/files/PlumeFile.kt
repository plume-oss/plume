package io.github.plume.oss.domain.files

import java.io.File

/**
 * Generic wrapper for supported Plume source/class files.
 */
abstract class PlumeFile(pathname: String, val fileType: PlumeFileType) : File(pathname) {

    override fun hashCode() = if (this.exists())
        FileFactory.getFileHash(this)
    else super.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlumeFile) return false
        if (!super.equals(other)) return false
        return true
    }

}