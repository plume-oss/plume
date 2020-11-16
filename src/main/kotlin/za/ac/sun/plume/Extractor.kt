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
import soot.jimple.spark.SparkTransformer
import soot.jimple.toolkits.callgraph.CHATransformer
import soot.jimple.toolkits.callgraph.Edge
import soot.options.Options
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.domain.exceptions.PlumeCompileException
import za.ac.sun.plume.domain.files.*
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.MetaDataVertex
import za.ac.sun.plume.drivers.GremlinDriver
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.graph.ASTBuilder
import za.ac.sun.plume.graph.CFGBuilder
import za.ac.sun.plume.graph.CallGraphBuilder
import za.ac.sun.plume.graph.PDGBuilder
import za.ac.sun.plume.options.ExtractorOptions
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaFiles
import za.ac.sun.plume.util.ResourceCompilationUtil.compileJavaScriptFiles
import za.ac.sun.plume.util.ResourceCompilationUtil.compilePythonFiles
import za.ac.sun.plume.util.SootToPlumeUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
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
    private val astBuilder: ASTBuilder
    private val cfgBuilder: CFGBuilder
    private val pdgBuilder: PDGBuilder
    private val callGraphBuilder: CallGraphBuilder

    init {
        checkDriverConnection(driver)
        astBuilder = ASTBuilder(driver)
        cfgBuilder = CFGBuilder(driver)
        pdgBuilder = PDGBuilder(driver)
        callGraphBuilder = CallGraphBuilder(driver)
    }

    companion object {
        private val sootToPlume = mutableMapOf<Any, MutableList<PlumeVertex>>()

        /**
         * Associates the given Soot object to the given [PlumeVertex]
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
         * @param plumeVertex The [PlumeVertex] to associate to.
         */
        fun addSootToPlumeAssociation(sootObject: Any, plumeVertex: PlumeVertex) {
            if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = mutableListOf(plumeVertex)
            else sootToPlume[sootObject]?.add(plumeVertex)
        }

        /**
         * Associates the given Soot object to the given [PlumeVertex] at the given index.
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
         * @param plumeVertex The [PlumeVertex] to associate to.
         * @param index The index to place the associated [PlumeVertex] at.
         */
        fun addSootToPlumeAssociation(sootObject: Any, plumeVertex: PlumeVertex, index: Int) {
            if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = mutableListOf(plumeVertex)
            else sootToPlume[sootObject]?.add(index, plumeVertex)
        }

        /**
         * Associates the given Soot object to the given list of [PlumeVertex]s.
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
         * @param plumeVertices The list of [PlumeVertex]s to associate to.
         */
        fun addSootToPlumeAssociation(sootObject: Any, plumeVertices: MutableList<PlumeVertex>) {
            if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = plumeVertices
            else sootToPlume[sootObject]?.addAll(plumeVertices)
        }

        /**
         * Retrieves the list of [PlumeVertex] associations to the given Soot object.
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to get associations from.
         */
        fun getSootAssociation(sootObject: Any): List<PlumeVertex>? = sootToPlume[sootObject]
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
     * @param f The Java source/class file, or a directory of source/class files.
     * @throws PlumeCompileException If no suitable Java compiler is found given .java files.
     * @throws NullPointerException If the file does not exist.
     * @throws IOException This would throw if given .java files which fail to compile.
     */
    @Throws(PlumeCompileException::class, NullPointerException::class, IOException::class)
    fun load(f: File) {
        if (!f.exists()) {
            throw NullPointerException("File '${f.name}' does not exist!")
        } else if (f.isDirectory) {
            Files.walk(Paths.get(f.absolutePath)).use { walk ->
                walk.map { obj: Path -> obj.toString() }
                        .filter { f: String -> f.matches(Regex("^.*\\.(class|py|js|java)\$")) }
                        .map { pathname: String -> FileFactory(pathname) }
                        .collect(Collectors.toList())
                        .let { loadedFiles.addAll(it) }
            }
        } else if (f.isFile) {
            loadedFiles.add(FileFactory(f))
        }
    }

    /**
     * Will compile all supported source files loaded in the given set.
     *
     * @param files [File] pointers to source files.
     * @return A set of [File] pointers to the compiled class files.
     */
    private fun compileLoadedFiles(files: HashSet<File>): HashSet<File> {
        val splitFiles = mapOf<SupportedFile, MutableList<File>>(
                SupportedFile.JAVA to mutableListOf(),
                SupportedFile.JAVASCRIPT to mutableListOf(),
                SupportedFile.PYTHON to mutableListOf(),
                SupportedFile.JVM_CLASS to mutableListOf()
        )
        // Organize file in the map. Perform this sequentially if there are less than 100,000 files.
        files.stream().let { if (files.size >= 100000) it.parallel() else it.sequential() }
                .toList().stream().forEach {
                    when (it) {
                        is JavaFile -> splitFiles[SupportedFile.JAVA]?.add(it)
                        is PythonFile -> splitFiles[SupportedFile.PYTHON]?.add(it)
                        is JavaScriptFile -> splitFiles[SupportedFile.JAVASCRIPT]?.add(it)
                        is JVMClassFile -> splitFiles[SupportedFile.JVM_CLASS]?.add(it)
                    }
                }
        return splitFiles.keys.map {
            val filesToCompile = (splitFiles[it] ?: emptyList<File>()).toList()
            return@map when (it) {
                SupportedFile.JAVA ->
                    compileJavaFiles(filesToCompile)
                            .apply { if (this.isNotEmpty()) driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version"))) }
                SupportedFile.PYTHON ->
                    compilePythonFiles(filesToCompile)
                            .apply { if (this.isNotEmpty()) driver.addVertex(MetaDataVertex("Python", "2.7.2")) }
                SupportedFile.JAVASCRIPT ->
                    compileJavaScriptFiles(filesToCompile)
                            .apply { if (this.isNotEmpty()) driver.addVertex(MetaDataVertex("JavaScript", "170")) }
                SupportedFile.JVM_CLASS ->
                    filesToCompile
                            .apply { if (this.isNotEmpty()) driver.addVertex(MetaDataVertex("Java", System.getProperty("java.runtime.version"))) }
            }
        }.asSequence().flatten().toHashSet()
    }

    /**
     * Projects all loaded classes currently loaded.
     */
    fun project() {
        configureSoot()
        val compiledFiles = compileLoadedFiles(loadedFiles)
        val classStream = loadClassesIntoSoot(compiledFiles)
        when (ExtractorOptions.callGraphAlg) {
            ExtractorOptions.CallGraphAlg.CHA -> CHATransformer.v().transform()
            ExtractorOptions.CallGraphAlg.SPARK -> SparkTransformer.v().transform("", ExtractorOptions.sparkOpts)
            else -> Unit
        }
        // Load all methods to construct the CPG from and convert them to UnitGraph objects
        val graphs = classStream.asSequence()
                .map { it.methods.filter { mtd -> mtd.isConcrete }.toList() }.flatten()
                .let {
                    if (ExtractorOptions.callGraphAlg == ExtractorOptions.CallGraphAlg.NONE)
                        it else it.map(this::addExternallyReferencedMethods).flatten()
                }
                .distinct().toList().let { if (it.size >= 100000) it.parallelStream() else it.stream() }
                .filter { !it.isPhantom }.map { BriefUnitGraph(it.retrieveActiveBody()) }.toList()
        // Construct the CPGs for methods
        graphs.map(this::constructCPG)
                .toList().asSequence()
                .map(this::constructCallGraphEdges)
                .map { it.declaringClass }.distinct().toList()
                .forEach(this::constructStructure)
        // Connect methods to their type declarations and source files (if present)
        graphs.forEach { SootToPlumeUtil.connectMethodToTypeDecls(it.body.method, driver) }
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
        logger.debug("Building file, namespace, and type declaration for ${cls.name}")
        SootToPlumeUtil.buildClassStructure(cls, driver)
        SootToPlumeUtil.buildTypeDeclaration(cls, driver)
    }

    /**
     * Constructs the code-property graph from a method's [BriefUnitGraph].
     *
     * @param graph The [BriefUnitGraph] to construct the method head and body CPG from.
     * @return The given graph.
     */
    private fun constructCPG(graph: BriefUnitGraph): BriefUnitGraph {
        logger.debug("Projecting $classPath")
        // Build head
        SootToPlumeUtil.buildMethodHead(graph.body.method, driver)
        // Build body
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
        if (ExtractorOptions.callGraphAlg != ExtractorOptions.CallGraphAlg.NONE) callGraphBuilder.buildMethodBody(graph)
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
        // keep variable names
        PhaseOptions.v().setPhaseOption("jb", "use-original-names:true")
        // call graph options
        if (ExtractorOptions.callGraphAlg != ExtractorOptions.CallGraphAlg.NONE)
            Options.v().set_whole_program(true)
        if (ExtractorOptions.callGraphAlg == ExtractorOptions.CallGraphAlg.SPARK) {
            Options.v().setPhaseOption("cg", "enabled:true")
            Options.v().setPhaseOption("cg.spark", "enabled:true")
        }
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
        G.reset()
    }

}