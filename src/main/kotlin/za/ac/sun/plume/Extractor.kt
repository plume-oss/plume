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
import za.ac.sun.plume.domain.exceptions.PlumeCompileException
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.MetaDataVertex
import za.ac.sun.plume.drivers.GremlinDriver
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.drivers.JanusGraphDriver
import za.ac.sun.plume.drivers.TinkerGraphDriver
import za.ac.sun.plume.graph.ASTBuilder
import za.ac.sun.plume.graph.CFGBuilder
import za.ac.sun.plume.graph.PDGBuilder
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFile
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFiles
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaScriptFile
import za.ac.sun.plume.util.ResourceCompilationUtil.compilePythonFile
import za.ac.sun.plume.util.ResourceCompilationUtil.fetchClassFiles
import java.io.File
import java.io.IOException
import java.util.*
import java.util.jar.JarFile

/**
 * The main entrypoint of the extractor from which the CPG will be created.
 *
 * @param driver the [IDriver] with which the graph will be constructed with.
 * @param classPath the root of the source and class files to be analyzed.
 */
class Extractor(private val driver: IDriver, private val classPath: File) {
    private val logger: Logger = LogManager.getLogger(Extractor::javaClass)

    private val loadedFiles: HashSet<File> = HashSet()
    private val sootToPlume = mutableMapOf<Any, MutableList<PlumeVertex>>()
    private val astBuilder: ASTBuilder
    private val cfgBuilder: CFGBuilder
    private val pdgBuilder: PDGBuilder

    init {
        checkDriverConnection(driver)
        configureSoot()
        astBuilder = ASTBuilder(driver, sootToPlume)
        cfgBuilder = CFGBuilder(driver, sootToPlume)
        pdgBuilder = PDGBuilder(driver, sootToPlume)
    }

    /**
     * Make sure that all drivers that require a connection are connected.
     *
     * @param driver The driver to check the connection of.
     */
    private fun checkDriverConnection(driver: IDriver) {
        when (driver) {
            is GremlinDriver -> if (!driver.connected) driver.connect()
        }
    }

    /**
     * Loads a single Java class file or directory of class files into the cannon.
     *
     * @param file The Java source/class file, or a directory of source/class files.
     * @throws PlumeCompileException If no suitable Java compiler is found given .java files.
     * @throws NullPointerException If the file does not exist.
     * @throws IOException This would throw if given .java files which fail to compile.
     */
    @Throws(PlumeCompileException::class, NullPointerException::class, IOException::class)
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
                    driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version")))
                }
                file.name.endsWith(".py") -> {
                    compilePythonFile(file)
                    loadedFiles.add(File(file.absolutePath.replace(".py", "\$py.class")))
                    driver.addVertex(MetaDataVertex("Python", "2.7.2"))
                }
                file.name.endsWith(".js") -> {
                    compileJavaScriptFile(file)
                    loadedFiles.add(File(file.absolutePath.replace(".js", ".class")))
                    driver.addVertex(MetaDataVertex("JavaScript", "170"))
                }
                file.name.endsWith(".jar") -> {
                    val jar = JarFile(file)
                    loadedFiles.addAll(fetchClassFiles(jar))
                    driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version")))
                }
                file.name.endsWith(".class") -> {
                    loadedFiles.add(file)
                    driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version")))
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
        // Add metadata if not present
        loadedFiles.forEach { project(it) }
        loadedFiles.clear()
    }

    /**
     * Attempts to project a file from the cannon.
     *
     * @param f The file to project.
     */
    private fun project(f: File) {
        val classPath = getQualifiedClassPath(f)
        val cls = Scene.v().loadClassAndSupport(classPath)
        cls.setApplicationClass()
        logger.debug("Projecting $classPath")
        astBuilder.buildProgramStructure(cls)
        cls.methods.filter { it.isConcrete }.forEach {
            val unitGraph = BriefUnitGraph(it.retrieveActiveBody())
            astBuilder.build(it, unitGraph)
            cfgBuilder.build(it, unitGraph)
            pdgBuilder.build(it, unitGraph)
        }
        // Clear this class' references and allow for GC removal
        sootToPlume.clear()
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
        // set whole program mode
        Options.v().set_whole_program(true)
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
     * @param classNames A set of class files.
     */
    private fun loadClassesIntoSoot(classNames: HashSet<File>) {
        classNames.forEach { Scene.v().addBasicClass(getQualifiedClassPath(it)) }
        Scene.v().loadBasicClasses()
    }

}