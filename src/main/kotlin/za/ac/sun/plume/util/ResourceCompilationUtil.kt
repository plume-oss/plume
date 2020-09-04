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
package za.ac.sun.plume.util

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import za.ac.sun.plume.domain.exceptions.PlumeCompileException
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.stream.Collectors
import javax.tools.ToolProvider

object ResourceCompilationUtil {
    private val logger: Logger = LogManager.getLogger(ResourceCompilationUtil::javaClass)

    /**
     * Validates the given file as a directory that exists.
     *
     * @param f the file to validate
     * @throws IOException if the file is not a valid directory or does not exist.
     */
    @Throws(IOException::class)
    private fun validateFileAsDirectory(f: File) {
        // Validate path
        if (!f.isDirectory) throw IOException("The path must point to a valid directory!")
        if (!f.exists()) throw IOException("The path does not exist!")
    }

    /**
     * Given a path to a Java source file, programmatically compiles the source (.java) file.
     *
     * @param file the source file to compile
     */
    @JvmStatic
    fun compileJavaFile(file: File) {
        val javac = ToolProvider.getSystemJavaCompiler() ?: throw PlumeCompileException("Unable to find a Java compiler on the system!")
        if (javac.sourceVersions.none { it.ordinal >= 8 }) throw PlumeCompileException("Plume requires JDK version >= 8. Please install a suitable JDK and re-run the process.")
        val fileManager = javac.getStandardFileManager(null, null, null)
        javac.getTask(null, fileManager, null, listOf("-g"), null,
                fileManager.getJavaFileObjects(file)).call()
    }

    /**
     * Given a path to a directory, programmatically compile any .java files found in the directory.
     *
     * @param path the path to the directory
     * @throws IOException if the path is not a directory or does not exist
     */
    @JvmStatic
    @Throws(IOException::class)
    fun compileJavaFiles(path: File) {
        validateFileAsDirectory(path)
        // Dynamically compile Java test resources
        val javac = ToolProvider.getSystemJavaCompiler()
        val fileManager = javac.getStandardFileManager(null, null, null)
        val fileList = LinkedList<File>()
        Files.walk(Paths.get(path.absolutePath)).use { walk ->
            walk.map { obj: Path -> obj.toString() }
                    .filter { f: String -> f.endsWith(".java") }
                    .collect(Collectors.toList())
                    .forEach{ f: String -> fileList.add(File(f)) }
        }
        javac.getTask(null, fileManager, null, listOf("-g"), null,
                fileManager.getJavaFileObjectsFromFiles(fileList)).call()
    }

    /**
     * Given a path to a directory, programmatically delete any .class files found in the directory.
     *
     * @param path the path to the directory
     * @throws IOException if the path is not a directory or does not exist
     */
    @JvmStatic
    @Throws(IOException::class)
    fun deleteClassFiles(path: File) {
        validateFileAsDirectory(path)
        Files.walk(Paths.get(path.absolutePath)).use { walk ->
            walk.map { obj: Path -> obj.toString() }
                    .filter { f: String -> f.endsWith(".class") }
                    .collect(Collectors.toList())
                    .forEach{ f: String -> if (!File(f).delete()) logger.error("Unable to delete: $f") }
        }
    }

    /**
     * Returns a list of all the class files under a given directory recursively.
     *
     * @param path the path to the directory
     * @return a list of all .class files under the given directory
     * @throws IOException if the path is not a directory or does not exist
     */
    @Throws(IOException::class)
    @JvmStatic
    fun fetchClassFiles(path: File): List<File> {
        validateFileAsDirectory(path)
        Files.walk(Paths.get(path.absolutePath)).use { walk ->
            return walk.map { obj: Path -> obj.toString() }
                    .filter { f: String -> f.endsWith(".class") }
                    .map { pathname: String -> File(pathname) }
                    .collect(Collectors.toList())
        }
    }

    /**
     * Returns a list of all the class files inside of a JAR file.
     *
     * @param jar the JarFile
     * @return a list of all `.class` files under the given JAR file.
     */
    @JvmStatic
    fun fetchClassFiles(jar: JarFile): MutableList<File> {
        return jar.stream()
                .map { obj: JarEntry -> obj.toString() }
                .filter { f: String -> f.endsWith(".class") }
                .map { name: String? -> JarEntry(name) }
                .map { je: JarEntry -> extractJarEntry(jar, je) }
                .filter { f: File? -> !Objects.isNull(f) }
                .collect(Collectors.toList())
    }

    /**
     * Extracts the [JarFile] from a given [JarEntry] and writes it to a temporary file, which is returned
     * as a [File].
     *
     * @param jarFile the JAR to extract from
     * @param entry   the entry to extract
     * @return the temporary file if the extraction process was successful, `null` if otherwise.
     */
    private fun extractJarEntry(jarFile: JarFile, entry: JarEntry): File? {
        try {
            val tmp = File.createTempFile(entry.toString().substring(entry.toString().lastIndexOf('/') + 1), null)
            jarFile.getInputStream(entry).use { `in` ->
                BufferedOutputStream(FileOutputStream(tmp)).use { out ->
                    val buffer = ByteArray(2048)
                    var nBytes = `in`.read(buffer)
                    while (nBytes > 0) {
                        out.write(buffer, 0, nBytes)
                        nBytes = `in`.read(buffer)
                    }
                }
            }
            return tmp
        } catch (e: IOException) {
            logger.warn("Error while extracting '" + entry.name + "' from JAR.", e)
            return null
        }
    }
}