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
import soot.*
import soot.options.Options
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.graph.ASTBuilder
import za.ac.sun.plume.graph.CFGBuilder
import za.ac.sun.plume.graph.PDGBuilder
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFile
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFiles
import za.ac.sun.plume.util.ResourceCompilationUtil.fetchClassFiles
import java.io.File
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.jar.JarFile

/**
 * The main entrypoint of the extractor from which the CPG will be created.
 *
 * @param hook the [IDriver] with which the graph will be constructed with.
 * @param classPath the root of the source and class files to be analyzed.
 */
class Extractor(hook: IDriver, private val classPath: File) {
    private val logger: Logger = LogManager.getLogger(Extractor::javaClass)

    private val loadedFiles: LinkedList<File> = LinkedList()
    private val astBuilder: ASTBuilder
    private val cfgBuilder: CFGBuilder
    private val pdgBuilder: PDGBuilder

    init {
        configureSoot()
        astBuilder = ASTBuilder(hook)
        cfgBuilder = CFGBuilder(hook)
        pdgBuilder = PDGBuilder(hook)
    }

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
            throw NullPointerException("File '$file.name' does not exist!")
        }
    }

    /**
     * Projects all loaded Java classes currently loaded.
     */
    fun project() {
        loadClassesIntoSoot(loadedFiles)
        loadedFiles.forEach(Consumer { project(it) })
        loadedFiles.clear()
    }

    /**
     * Attempts to project a file from the cannon.
     *
     * @param f the file to project.
     */
    private fun project(f: File) {
        val classPath = getQualifiedClassPath(f)
        try {
            val cls = Scene.v().loadClassAndSupport(classPath)
            cls.setApplicationClass()
            logger.debug("Projecting $classPath")
            astBuilder.buildFileAndNamespace(cls)
            cls.methods.filter { it.isConcrete }.forEach {
                val unitGraph = BriefUnitGraph(it.retrieveActiveBody())
                val sootToPlume = mutableMapOf<Any, MutableList<PlumeVertex>>()
                astBuilder.build(it, unitGraph, sootToPlume)
                cfgBuilder.build(it, unitGraph, sootToPlume)
                pdgBuilder.build(it, unitGraph, sootToPlume)
            }
        } catch (e: Exception) {
            logger.error("IOException encountered while projecting $classPath", e)
        }
    }

    /**
     * Configure Soot options for CPG transformation.
     */
    private fun configureSoot() {
        // set application mode
        Options.v().set_app(true)
        // make sure classpath is configured correctly
        Options.v().set_soot_classpath(classPath.absolutePath)
        Options.v().set_prepend_classpath(true)
        // keep debugging info
        Options.v().set_keep_line_number(true)
        Options.v().set_keep_offset(true)
        // ignore library code
        Options.v().set_no_bodies_for_excluded(true)
        Options.v().set_allow_phantom_refs(true)
        // exclude java.lang packages
        val excluded: MutableList<String> = ArrayList()
        excluded.add("java.lang")
        Options.v().set_exclude(excluded)
        // keep variable names
        PhaseOptions.v().setPhaseOption("jb", "use-original-names:true")
    }

    private fun getQualifiedClassPath(classFile: File): String = classFile.absolutePath.removePrefix(classPath.absolutePath + "/").replace(File.separator, ".").removeSuffix(".class")

    /**
     * Given a list of class names, load them into the Scene.
     *
     * @param classNames a list of class names
     */
    private fun loadClassesIntoSoot(classNames: LinkedList<File>) {
        classNames.forEach { Scene.v().addBasicClass(getQualifiedClassPath(it)) }
        Scene.v().loadBasicClasses()
    }

}