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
package za.ac.sun.plume

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.objectweb.asm.ClassReader
import za.ac.sun.plume.controllers.ASTController
import za.ac.sun.plume.domain.meta.MetaDataCollector
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFile
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFiles
import za.ac.sun.plume.util.ResourceCompilationUtil.fetchClassFiles
import za.ac.sun.plume.visitors.ast.ASTClassVisitor
import za.ac.sun.plume.visitors.init.InitialClassVisitor
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.jar.JarFile

class Extractor(private val hook: IDriver) {
    private val logger: Logger = LogManager.getLogger(Extractor::class.java)
    private val loadedFiles: LinkedList<File> = LinkedList()

    /**
     * Loads a single Java class file or directory of class files into the cannon.
     *
     * @param file the Java source/class file, directory of source/class files, or a JAR file.
     * @throws NullPointerException if the file is null
     * @throws IOException          In the case of a directory given, this would throw if .java files fail to compile
     */
    @Throws(NullPointerException::class, IOException::class)
    fun load(file: File) {
        if (file.isDirectory) {
            // Any .java files will automatically be compiled
            compileJavaFiles(file)
            loadedFiles.addAll(fetchClassFiles(file))
        } else if (file.isFile) {
            when {
                file.name.endsWith(".java") -> {
                    compileJavaFile(file)
                    loadedFiles.add(File(file.absolutePath.replace(".java", ".class")))
                }
                file.name.endsWith(".jar") -> {
                    val jar = JarFile(file)
                    loadedFiles.addAll(fetchClassFiles(jar))
                }
                file.name.endsWith(".class") -> {
                    loadedFiles.add(file)
                }
            }
        } else if (!file.exists()) {
            throw NullPointerException("File '" + file.name + "' does not exist!")
        }
    }

    /**
     * Projects all loaded Java classes currently loaded.
     */
    fun project() {
        loadedFiles.forEach(Consumer { f: File -> this.project(f) })
        loadedFiles.clear()
    }

    /**
     * Attempts to project a file from the cannon.
     *
     * @param f the file to project.
     */
    private fun project(f: File) {
        try {
            // Allows us to accumulate information about classes beforehand
            val classMetaController = MetaDataCollector()
            // Allows us to build up our AST using the connection held by the hook
            val astController = ASTController(hook)
            FileInputStream(f).use { fis ->
                // Initialize services and controllers
                astController.clear().resetOrder()

                // First do an independent scan of the class
                val cr = ClassReader(fis)
                val rootVisitor = InitialClassVisitor(classMetaController)
                cr.accept(rootVisitor, 0)

                // Once initial data has been gathered, build the graph
                val astVisitor = ASTClassVisitor(classMetaController, astController)
                // ^ append new visitors here
                cr.accept(astVisitor, 0)
            }
        } catch (e: IOException) {
            logger.error("IOException encountered while visiting '" + f.name + "'.", e)
        }
    }

}