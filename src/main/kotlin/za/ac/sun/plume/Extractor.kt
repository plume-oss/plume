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
import soot.PhaseOptions
import soot.Scene
import soot.SootClass
import soot.SootMethod
import soot.jimple.toolkits.callgraph.CHATransformer
import soot.jimple.toolkits.callgraph.Edge
import soot.options.Options
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.exceptions.PlumeCompileException
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.FileVertex
import za.ac.sun.plume.domain.models.vertices.MetaDataVertex
import za.ac.sun.plume.domain.models.vertices.MethodVertex
import za.ac.sun.plume.domain.models.vertices.TypeDeclVertex
import za.ac.sun.plume.drivers.GremlinDriver
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.graph.ASTBuilder
import za.ac.sun.plume.graph.CFGBuilder
import za.ac.sun.plume.graph.CallGraphBuilder
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
import kotlin.collections.HashSet
import kotlin.streams.toList


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
    private val callGraphBuilder: CallGraphBuilder

    init {
        checkDriverConnection(driver)
        configureSoot()
        astBuilder = ASTBuilder(driver, sootToPlume)
        cfgBuilder = CFGBuilder(driver, sootToPlume)
        pdgBuilder = PDGBuilder(driver, sootToPlume)
        callGraphBuilder = CallGraphBuilder(driver, sootToPlume)
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
            throw NullPointerException("File '${file.name}' does not exist!")
        }
    }

    /**
     * Projects all loaded classes currently loaded.
     */
    fun project() {
        val classStream = loadClassesIntoSoot(loadedFiles)
        CHATransformer.v().transform()
        // Load all methods to construct the CPG from and convert them to UnitGraph objects
        val graphs = classStream.asSequence()
                .map { it.methods.parallelStream().filter { mtd -> mtd.isConcrete }.toList() }.flatten()
                .map(this::addExternallyReferencedMethods).flatten()
                .distinct().toList()
                .parallelStream()
                .map { BriefUnitGraph(it.retrieveActiveBody()) }.toList()
        // Construct the CPGs for methods
        graphs.map(this::constructCPG)
                .toList().asSequence()
                .map(this::constructCallGraphEdges)
                .map { it.declaringClass }.distinct().toList()
                .forEach(this::constructStructure)
        // Connect methods to their type declarations and source files (if present)
        graphs.forEach(this::connectMethodToTypeDecls)
        clear()
    }

    /**
     * Searches for methods called outside of the application perspective. If they belong to classes loaded in Soot then
     * they are added to a list which is then returned including the given method.
     *
     * @param mtd The [SootMethod] from which the calls to methods will be collected.
     * @return The list of methods called including the given method.
     */
    private fun addExternallyReferencedMethods(mtd: SootMethod): List<SootMethod> {
        val cg = Scene.v().callGraph
        val edges = cg.edgesOutOf(mtd) as Iterator<Edge>
        return edges.asSequence().map { it.tgt.method() }.toMutableList().apply { this.add(mtd) }
    }

    /**
     * Constructs type, package, and source file information from the given class.
     *
     * @param cls The [SootClass] containing the information to build program structure information from.
     */
    private fun constructStructure(cls: SootClass) {
        astBuilder.buildClassStructure(cls)
        astBuilder.buildTypeDeclaration(cls)
    }

    /**
     * Connects the given method's [BriefUnitGraph] to its type declaration and source file (if present).
     *
     * @param graph The [BriefUnitGraph] to connect and extract type and source information from.
     */
    private fun connectMethodToTypeDecls(graph: BriefUnitGraph) {
        sootToPlume[graph.body.method.declaringClass]?.let { classVertices ->
            val typeDeclVertex = classVertices.first { it is TypeDeclVertex }
            val clsVertex = classVertices.first { it is FileVertex }
            val methodVertex = sootToPlume[graph.body.method]?.first { it is MethodVertex } as MethodVertex
            // Connect method to type declaration
            driver.addEdge(typeDeclVertex, methodVertex, EdgeLabel.AST)
            // Connect method to source file
            driver.addEdge(methodVertex, clsVertex, EdgeLabel.SOURCE_FILE)
        }
    }

    /**
     * Constructs the code-property graph from a method's [BriefUnitGraph].
     *
     * @param graph The [BriefUnitGraph] to construct the method head and body CPG from.
     * @return The given graph.
     */
    private fun constructCPG(graph: BriefUnitGraph): BriefUnitGraph {
        logger.debug("Projecting $classPath")
        astBuilder.buildMethodHead(graph)
        astBuilder.buildMethodBody(graph)
        cfgBuilder.buildMethodBody(graph)
        pdgBuilder.buildMethodBody(graph)
        return graph
    }

    /**
     * Once the method bodies are constructed, this function then connects calls to the called methods if present.
     *
     * @param graph The [BriefUnitGraph] from which calls are checked and connected to their referred methods.
     * @return The method from the given graph.
     */
    private fun constructCallGraphEdges(graph: BriefUnitGraph): SootMethod {
        callGraphBuilder.buildMethodBody(graph)
        return graph.body.method
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

    /**
     * Obtains the class path the way Soot expects the input.
     *
     * @param classFile The class file pointer.
     * @return The qualified class path with periods separating packages instead of slashes and no ".class" extension.
     */
    private fun getQualifiedClassPath(classFile: File): String = classFile.absolutePath
            .removePrefix(classPath.absolutePath + "/")
            .replace(File.separator, ".")
            .removeSuffix(".class")

    /**
     * Given a list of class names, load them into the Scene.
     *
     * @param classNames A set of class files.
     * @return the given class files as a list of [SootClass].
     */
    private fun loadClassesIntoSoot(classNames: HashSet<File>): List<SootClass> {
        classNames.map(this::getQualifiedClassPath).forEach(Scene.v()::addBasicClass)
        Scene.v().loadBasicClasses()
        return classNames.map(this::getQualifiedClassPath).map(Scene.v()::loadClassAndSupport).map { it.setApplicationClass(); it }
    }

    /**
     * Clears resources of file and graph pointers.
     */
    fun clear() {
        loadedFiles.clear()
        sootToPlume.clear()
    }

}