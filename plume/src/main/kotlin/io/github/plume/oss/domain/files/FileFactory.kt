/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * @return A [File] object if not one of the supported file types or a supported file type such as [JavaSourceFile].
     */
    @JvmStatic
    operator fun invoke(pathname: String): PlumeFile {
        return when {
            pathname.endsWith(".java") -> JavaSourceFile(pathname)
            pathname.endsWith(".class") -> JavaClassFile(pathname)
            else -> UnsupportedFile(pathname)
        }
    }

    /**
     * Creates a [File] given the pathname.
     *
     * @param f A generic [File] pointer for the file to cast.
     * @return A [File] object if not one of the supported file types or a supported file type such as [JavaSourceFile].
     */
    @JvmStatic
    operator fun invoke(f: File): PlumeFile {
        return when {
            f.name.endsWith(".java") -> JavaSourceFile(f.absolutePath)
            f.name.endsWith(".class") -> JavaClassFile(f.absolutePath)
            else -> UnsupportedFile(f.absolutePath)
        }
    }

}

/**
 * Class wrapper for Java source files.
 */
class JavaSourceFile internal constructor(pathname: String) : PlumeFile(pathname, PlumeFileType.JAVA_SOURCE)

/**
 * Class wrapper for JVM class files.
 */
class JavaClassFile internal constructor(pathname: String) : PlumeFile(pathname, PlumeFileType.JAVA_CLASS)

/**
 * Class wrapper for unsupported files.
 */
class UnsupportedFile internal constructor (pathname: String) : PlumeFile(pathname, PlumeFileType.UNSUPPORTED)

/**
 * The file types ingested by Plume's [Extractor].
 */
enum class PlumeFileType {
    /**
     * Java is a class-based, object-oriented programming language that is designed to have as few implementation
     * dependencies as possible.
     */
    JAVA_SOURCE,

    /**
     * Java bytecode is the instruction set of the Java virtual machine.
     */
    JAVA_CLASS,

    /**
     * Any file that is not supporte.
     */
    UNSUPPORTED
}