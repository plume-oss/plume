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
import io.github.plume.oss.domain.files.JavaFile
import io.github.plume.oss.domain.files.PlumeFile
import io.github.plume.oss.domain.files.SupportedFile
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_CODE
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipFile
import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.StandardLocation
import javax.tools.ToolProvider
import kotlin.streams.toList


object ResourceCompilationUtil {
    private val logger = LogManager.getLogger(ResourceCompilationUtil::class.java)

    val TEMP_DIR = "${System.getProperty("java.io.tmpdir")}${File.separator}plume"
    val COMP_DIR = "$TEMP_DIR${File.separator}build"

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
            for (jfo in fileManager.list(
                StandardLocation.CLASS_OUTPUT,
                "", Collections.singleton(JavaFileObject.Kind.CLASS), true
            )) {
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

    fun unzipArchive(zf: ZipFile) = sequence {
        zf.use { zip ->
            // Copy zipped files across
            zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val destFile = File(COMP_DIR + File.separator + entry.name)
                val dirName = destFile.absolutePath.substringBeforeLast('/')
                // Create directory path
                File(dirName).mkdirs()
                runCatching {
                    destFile.createNewFile()
                }.onSuccess {
                    zip.getInputStream(entry)
                        .use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
                }.onFailure {
                    logger.warn("Encountered an error while extracting entry ${entry.name} from archive ${zf.name}.")
                }
                yield(destFile)
            }
        }
    }

    /**
     * Will compile all supported source files loaded in the given set.
     *
     * @param files [PlumeFile] pointers to source files.
     * @return A set of [PlumeFile] pointers to the compiled class files.
     */
    fun compileLoadedFiles(files: HashSet<PlumeFile>): HashSet<JVMClassFile> {
        val splitFiles = mapOf<SupportedFile, MutableList<PlumeFile>>(
            SupportedFile.JAVA to mutableListOf(),
            SupportedFile.JVM_CLASS to mutableListOf()
        )
        // Organize file in the map. Perform this sequentially if there are less than 100,000 files.
        files.stream().let { if (files.size >= 100000) it.parallel() else it.sequential() }
            .toList().stream().forEach {
                when (it) {
                    is JavaFile -> splitFiles[SupportedFile.JAVA]?.add(it)
                    is JVMClassFile -> splitFiles[SupportedFile.JVM_CLASS]?.add(it)
                }
            }
        return splitFiles.keys.map {
            val filesToCompile = (splitFiles[it] ?: emptyList<JVMClassFile>()).toList()
            return@map when (it) {
                SupportedFile.JAVA -> compileJavaFiles(filesToCompile)
                SupportedFile.JVM_CLASS -> moveClassFiles(filesToCompile.map { f -> f as JVMClassFile }.toList())
            }
        }.asSequence().flatten().toHashSet()
    }
}