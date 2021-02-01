/*
 * Copyright 2020 David Baker Effendi
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
package io.github.plume.oss.util

import io.github.plume.oss.domain.exceptions.PlumeCompileException
import io.github.plume.oss.domain.files.JVMClassFile
import io.github.plume.oss.domain.files.PlumeFile
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.stream.Collectors
import javax.tools.JavaCompiler
import javax.tools.ToolProvider
import javax.tools.JavaFileObject

import javax.tools.StandardLocation


object ResourceCompilationUtil {
    private val logger: Logger = LogManager.getLogger(ResourceCompilationUtil::javaClass)
    val COMP_DIR = "${System.getProperty("java.io.tmpdir")}${File.separator}plume"

    /**
     * Validates the given file as a directory that exists.
     *
     * @param f the file to validate.
     * @throws IOException if the file is not a valid directory or does not exist.
     */
    @Throws(IOException::class)
    private fun validateFileAsDirectory(f: File) {
        // Validate path
        if (!f.isDirectory) throw IOException("The path must point to a valid directory!")
        if (!f.exists()) throw IOException("The path does not exist!")
    }

    /**
     * Given paths to a Java source files, programmatically compiles the source (.java) files.
     *
     * @param files the source files to compile.
     * @throws PlumeCompileException if there is no suitable Java compiler found.
     */
    fun compileJavaFiles(files: List<PlumeFile>): List<JVMClassFile> {
        if (files.isEmpty()) return emptyList()
        val javac = getJavaCompiler()
        val fileManager = javac.getStandardFileManager(null, null, null)
        javac.getTask(
            null,
            fileManager,
            null,
            listOf("-g", "-d", COMP_DIR),
            null,
            fileManager.getJavaFileObjectsFromFiles(files)
        ).call()
        return sequence {
            for (jfo in fileManager.list(StandardLocation.CLASS_OUTPUT,
                "", Collections.singleton(JavaFileObject.Kind.CLASS), true)) {
                yield(JVMClassFile(jfo.name))
            }
        }.toList()
    }

    /**
     * Inspects class files and moves them to the temp directory based on their package path.
     *
     * @param files the class files to move.
     */
    fun moveClassFiles(files: List<JVMClassFile>): List<JVMClassFile> {
        lateinit var destPath: String

        class ClassPathVisitor : ClassVisitor(Opcodes.ASM8) {
            override fun visit(
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String?,
                interfaces: Array<out String>?
            ) {
                destPath = COMP_DIR + File.separator + name + ".class"
            }
        }

        return files.map { f ->
            FileInputStream(f).use { fis ->
                val cr = ClassReader(fis)
                val rootVisitor = ClassPathVisitor()
                cr.accept(rootVisitor, SKIP_CODE)
            }
            val dstFile = JVMClassFile(destPath)
            dstFile.mkdirs()
            Files.copy(f.toPath(), dstFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            dstFile
        }.toList()
    }

    /**
     * Obtains an appropriate JDK to compile source to class files.
     *
     * @throws PlumeCompileException if no suitable JDK is found.
     * @return a [JavaCompiler] with a suitable version.
     */
    private fun getJavaCompiler(): JavaCompiler {
        val javac = ToolProvider.getSystemJavaCompiler()
            ?: throw PlumeCompileException("Unable to find a Java compiler on the system!")
        if (javac.sourceVersions.none { it.ordinal >= 8 }) throw PlumeCompileException("Plume requires JDK version >= 8. Please install a suitable JDK and re-run the process.")
        return javac
    }

    /**
     * Given a path to a directory, programmatically delete any .class files found in the directory.
     *
     * @param path the path to the directory.
     * @throws IOException if the path is not a directory or does not exist.
     */
    @Throws(IOException::class)
    fun deleteClassFiles(path: File) {
        validateFileAsDirectory(path)
        Files.walk(Paths.get(path.absolutePath)).use { walk ->
            walk.map { obj: Path -> obj.toString() }
                .filter { f: String -> f.endsWith(".class") }
                .collect(Collectors.toList())
                .forEach { f: String -> if (!File(f).delete()) logger.error("Unable to delete: $f") }
        }
    }
}