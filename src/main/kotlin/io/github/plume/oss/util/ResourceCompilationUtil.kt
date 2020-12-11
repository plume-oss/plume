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

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.python.util.JycompileAntTask
import io.github.plume.oss.domain.exceptions.PlumeCompileException
import io.github.plume.oss.domain.files.JVMClassFile
import io.github.plume.oss.domain.files.PlumeFile
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.tools.JavaCompiler
import javax.tools.ToolProvider
import kotlin.streams.toList
import org.mozilla.javascript.tools.jsc.Main as JSC

object ResourceCompilationUtil {
    private val logger: Logger = LogManager.getLogger(ResourceCompilationUtil::javaClass)

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
    @JvmStatic
    fun compileJavaFiles(files: List<PlumeFile>): List<JVMClassFile> {
        if (files.isEmpty()) return emptyList()
        val javac = getJavaCompiler()
        val fileManager = javac.getStandardFileManager(null, null, null)
        javac.getTask(null, fileManager, null, listOf("-g"), null,
                fileManager.getJavaFileObjectsFromFiles(files)).call()
        return files.map { JVMClassFile(it.absolutePath.replace(".java", ".class")) }.toList()
    }

    /**
     * Given paths to a Python source files, programmatically compiles the source (.py) files.
     *
     * @param files the source files to compile.
     * @throws PlumeCompileException if there is no suitable Java compiler found.
     */
    @JvmStatic
    fun compilePythonFiles(files: List<PlumeFile>): List<JVMClassFile> {
        if (files.isEmpty()) return emptyList()
        val jythonc = JycompileAntTask()
        getJavaCompiler()
        // These needs to be compiled per directory level
        val dirMap = mutableMapOf<String, MutableList<PlumeFile>>()
        files.forEach {
            val dir = it.absolutePath.removeSuffix("/${it.name}")
            if (dirMap[dir].isNullOrEmpty()) dirMap[dir] = mutableListOf(it)
            else dirMap[dir]?.add(it)
        }
        dirMap.forEach {
            jythonc.destdir = File(it.key)
            jythonc.process(it.value.toSet())
        }
        return files.map { JVMClassFile(it.absolutePath.replace(".py", "\$py.class")) }.toList()
    }

    /**
     * Given paths to a JavaScript source files, programmatically compiles the source (.js) files.
     *
     * @param files the source files to compile.
     * @throws PlumeCompileException if there is no suitable Java compiler found.
     */
    @JvmStatic
    fun compileJavaScriptFiles(files: List<PlumeFile>): List<JVMClassFile> {
        if (files.isEmpty()) return emptyList()
        val jsc = JSC()
        getJavaCompiler()
        jsc.processOptions(arrayOf("-version", "170", "-g"))
        jsc.processSource(files.parallelStream().map { it.absolutePath }.toList().toTypedArray())
        return files.map { JVMClassFile(it.absolutePath.replace(".js", ".class")) }.toList()
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
    @JvmStatic
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