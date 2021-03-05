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
package io.github.plume.oss

import io.github.plume.oss.domain.exceptions.PlumeCompileException
import io.github.plume.oss.domain.files.FileFactory
import io.github.plume.oss.domain.files.JVMClassFile
import io.github.plume.oss.domain.files.PlumeFile
import io.github.plume.oss.domain.files.UnsupportedFile
import io.github.plume.oss.drivers.GremlinDriver
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.drivers.Neo4jDriver
import io.github.plume.oss.drivers.OverflowDbDriver
import io.github.plume.oss.metrics.ExtractorTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.github.plume.oss.options.ExtractorOptions
import io.github.plume.oss.passes.method.ASTPass
import io.github.plume.oss.passes.method.CFGPass
import io.github.plume.oss.passes.method.CGPass
import io.github.plume.oss.passes.method.PDGPass
import io.github.plume.oss.passes.structure.FileAndPackagePass
import io.github.plume.oss.passes.structure.MarkForRebuildPass
import io.github.plume.oss.passes.structure.TypeDeclPass
import io.github.plume.oss.util.DiffGraphUtil
import io.github.plume.oss.util.ExtractorConst.LANGUAGE_FRONTEND
import io.github.plume.oss.util.ExtractorConst.plumeVersion
import io.github.plume.oss.util.ResourceCompilationUtil
import io.github.plume.oss.util.ResourceCompilationUtil.COMP_DIR
import io.github.plume.oss.util.SootToPlumeUtil
import io.github.plume.oss.util.SootToPlumeUtil.buildTypeDeclaration
import io.github.plume.oss.util.SootToPlumeUtil.obtainModifiersFromTypeDeclVert
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.passes.ParallelCpgPass
import io.shiftleft.semanticcpg.passes.containsedges.ContainsEdgePass
import io.shiftleft.semanticcpg.passes.languagespecific.fuzzyc.TypeDeclStubCreator
import io.shiftleft.semanticcpg.passes.linking.linker.Linker
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import scala.jdk.CollectionConverters
import soot.*
import soot.jimple.spark.SparkTransformer
import soot.jimple.toolkits.callgraph.CHATransformer
import soot.jimple.toolkits.callgraph.Edge
import soot.options.Options
import soot.toolkits.graph.BriefUnitGraph
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import java.util.zip.ZipFile
import kotlin.streams.asSequence

/**
 * The main entrypoint of the extractor from which the CPG will be created.
 *
 * @param driver the [IDriver] with which the graph will be constructed with.
 */
class Extractor(val driver: IDriver) {
    private val logger: Logger = LogManager.getLogger(Extractor::javaClass)

    private val loadedFiles: HashSet<PlumeFile> = HashSet()
    private val callGraphBuilder: CGPass

    init {
        File(COMP_DIR).let { f -> if (f.exists()) f.deleteRecursively(); f.deleteOnExit() }
        checkDriverConnection(driver)
        callGraphBuilder = CGPass(driver)
        PlumeTimer.reset()
    }

    /**
     * The companion object of this class holds the state of the current extraction
     */
    companion object {
        private val sootToPlume = mutableMapOf<Any, MutableList<NewNodeBuilder>>()
        private val classToFileHash = mutableMapOf<SootClass, String>()
        private val savedCallGraphEdges = mutableMapOf<String, MutableList<NewCallBuilder>>()

        /**
         * Associates the given Soot object to the given [NewNode].
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
         * @param node The [NewNode] to associate to.
         * @param index The index to place the associated [NewNode] at.
         */
        fun addSootToPlumeAssociation(sootObject: Any, node: NewNodeBuilder, index: Int = -1) {
            if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = mutableListOf(node)
            else if (index <= -1) sootToPlume[sootObject]?.add(node)
            else sootToPlume[sootObject]?.add(index, node)
        }

        /**
         * Associates the given Soot object to the given list of [NewNode]s.
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to associate from.
         * @param nodes The list of [NewNode]s to associate to.
         * @param index The index to place the associated [PlumeVertex](s) at.
         */
        fun addSootToPlumeAssociation(sootObject: Any, nodes: MutableList<NewNodeBuilder>, index: Int = -1) {
            if (!sootToPlume.containsKey(sootObject)) sootToPlume[sootObject] = nodes
            else if (index <= -1) sootToPlume[sootObject]?.addAll(nodes)
            else sootToPlume[sootObject]?.addAll(index, nodes)
        }

        /**
         * Retrieves the list of [NewNode] associations to the given Soot object.
         *
         * @param sootObject The object from a Soot [BriefUnitGraph] to get associations from.
         */
        fun getSootAssociation(sootObject: Any): List<NewNodeBuilder>? = sootToPlume[sootObject]

        /**
         * Associates the given [SootClass] with its source file's hash.
         *
         * @param cls The [SootClass] to associate.
         * @param hash The hash for the file's contents.
         */
        fun putNewFileHashPair(cls: SootClass, hash: String) {
            classToFileHash[cls] = hash
        }

        /**
         * Retrieves the original file's hash from the given [SootClass].
         *
         * @param cls The representative [SootClass].
         */
        fun getFileHashPair(cls: SootClass) = classToFileHash[cls]
    }

    /**
     * Make sure that all drivers that require a connection are connected.
     *
     * @param driver The driver to check the connection of.
     */
    private fun checkDriverConnection(driver: IDriver) {
        when (driver) {
            is GremlinDriver -> if (!driver.connected) driver.connect()
            is OverflowDbDriver -> if (!driver.connected) driver.connect()
            is Neo4jDriver -> if (!driver.connected) driver.connect()
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
    fun load(f: File): Extractor {
        PlumeTimer.startTimerOn(ExtractorTimeKey.LOADING_AND_COMPILING)
        File(COMP_DIR).let { c -> if (!c.exists()) c.mkdirs() }
        if (!f.exists()) {
            throw NullPointerException("File '${f.name}' does not exist!")
        } else if (f.isDirectory) {
            Files.walk(Paths.get(f.absolutePath)).use { walk ->
                walk.map { obj: Path -> obj.toString() }
                    .map { FileFactory.invoke(it) }
                    .filter { it !is UnsupportedFile }
                    .collect(Collectors.toList())
                    .let { loadedFiles.addAll(it) }
            }
        } else if (f.isFile) {
            if (f.name.endsWith(".jar")) {
                ResourceCompilationUtil.unzipArchive(ZipFile(f)).forEach { loadedFiles.add(FileFactory(it)) }
            } else {
                loadedFiles.add(FileFactory(f))
            }
        }
        PlumeTimer.stopTimerOn(ExtractorTimeKey.LOADING_AND_COMPILING)
        return this
    }

    private fun addMetaDataInfo() {
        val maybeMetaData = driver.getMetaData()
        if (maybeMetaData != null) {
            val metaData = maybeMetaData.build()
            if (metaData.language() != LANGUAGE_FRONTEND || metaData.version() != plumeVersion) {
                driver.deleteVertex(maybeMetaData.id(), META_DATA)
                driver.addVertex(NewMetaDataBuilder().language(LANGUAGE_FRONTEND).version(plumeVersion))
            }
        } else {
            driver.addVertex(NewMetaDataBuilder().language(LANGUAGE_FRONTEND).version(plumeVersion))
        }
    }

    /**
     * Projects all loaded classes to a base CPG.
     */
    fun project(): Extractor {
        /*
            Load and compile files then feed them into Soot
         */
        val classStream = mutableListOf<SootClass>()
        PlumeTimer.measure(ExtractorTimeKey.LOADING_AND_COMPILING) {
            configureSoot()
            val compiledFiles = ResourceCompilationUtil.compileLoadedFiles(loadedFiles)
            if (compiledFiles.isNotEmpty()) addMetaDataInfo()
            loadClassesIntoSoot(compiledFiles).toCollection(classStream)
        }
        if (classStream.isEmpty()) return this
        /*
            Build program structure and remove sub-graphs which need to be rebuilt
         */
        val classesToBuild = mutableListOf<SootClass>()
        PlumeTimer.measure(ExtractorTimeKey.BASE_CPG_BUILDING) {
            pipeline(
                MarkForRebuildPass(driver)::runPass,
                FileAndPackagePass(driver)::runPass,
                TypeDeclPass(driver)::runPass
            ).invoke(classStream).toCollection(classesToBuild)
        }
        /*
            Build Soot Unit graphs
         */
        val sootUnitGraphs = mutableListOf<BriefUnitGraph>()
        PlumeTimer.measure(ExtractorTimeKey.UNIT_GRAPH_BUILDING) {
            constructUnitGraphs(classesToBuild).toCollection(sootUnitGraphs)
        }
        /*
            Build external type and method stubs
         */
        PlumeTimer.measure(ExtractorTimeKey.BASE_CPG_BUILDING) {
            val allTypes =
                classStream.asSequence().map { it.fields }.flatten().map { it.type } + sootUnitGraphs.asSequence()
                    .map { it.body.locals + it.body.parameterLocals }.flatten().map { it.type }
            createExternalTypes(classStream, allTypes)
            // Construct the CPGs for methods
            pipeline(
                ::baseCPGPass,
                ::constructCallGraphEdges,
            ).invoke(sootUnitGraphs)
        }
        clear()
        return this
    }

    /**
     * Adds additional data calculated from the graph using passes from [io.shiftleft.semanticcpg.passes] and
     * [io.shiftleft.dataflowengineoss.passes]. This is constructed from the base CPG and requires [Extractor.project]
     * to be called beforehand.
     */
    fun postProject(): Extractor {
        driver.getProgramTypeData().use { g ->
            PlumeTimer.measure(ExtractorTimeKey.SCPG_PASSES) {
                val cpg = Cpg.apply(g)
                listOf(
                    TypeDeclStubCreator(cpg),
                    Linker(cpg),
                ).map { it.run() }
                    .map(CollectionConverters::IteratorHasAsJava)
                    .flatMap { it.asJava().asSequence() }
                    .forEach { DiffGraphUtil.processDiffGraph(driver, it) }
            }
        }
        driver.getMethodNames().forEach { mName ->
            driver.getMethod(mName).use { g ->
                PlumeTimer.measure(ExtractorTimeKey.SCPG_PASSES) {
                    val cpg = Cpg.apply(g)
                    val methods = g.nodes(METHOD).asSequence().toList()
                    runParallelPass(methods.filterIsInstance<AstNode>(), ContainsEdgePass(cpg))
                    runParallelPass(methods.filterIsInstance<Method>(), ReachingDefPass(cpg))
                }
            }
        }
        return this
    }

    private fun <T> runParallelPass(parts: List<T>, pass: ParallelCpgPass<T>) {
        parts.map(pass::runOnPart)
            .map(CollectionConverters::IteratorHasAsJava)
            .flatMap { it.asJava().asSequence() }
            .forEach { DiffGraphUtil.processDiffGraph(driver, it) }
    }

    private fun <T> pipeline(vararg functions: (T) -> T): (T) -> T =
        { functions.fold(it) { output, stage -> stage.invoke(output) } }

    /**
     * Creates [TypeDecl] from external [soot.Type]s. This also links the [TypeDecl]s to their modifiers and the
     * unknown file vertex.
     *
     * @param classStream The stream of application [SootClass] to separate external classes from.
     * @param typeStream The stream of all [soot.Type]s.
     */
    private fun createExternalTypes(classStream: List<SootClass>, typeStream: Sequence<soot.Type>) {
        driver.getProgramStructure().use { g ->
            typeStream.distinct()
                .filter { t -> !classStream.any { it.name == t.toString() } }
                .filter {
                    !g.nodes(TYPE_DECL).asSequence().any { n -> n.property(FULL_NAME) == it.toString() }
                }
                .map { t -> buildTypeDeclaration(t).apply { driver.addVertex(this) } }
                .forEach { t ->
                    // Connect external type decls to the unknown file vert
                    getSootAssociation(io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN())?.let {
                        it.firstOrNull()?.let { f -> driver.addEdge(t, f, SOURCE_FILE) }
                    }
                    // Connect type decls to their modifiers
                    obtainModifiersFromTypeDeclVert(t).forEachIndexed { i, m ->
                        driver.addEdge(t, NewModifierBuilder().modifierType(m).order(i + 1), AST)
                    }
                }
        }
    }

    /**
     * Load all methods to construct the CPG from and convert them to [BriefUnitGraph] objects.
     *
     * @param classStream A stream of [SootClass] to construct [BriefUnitGraph] from.
     * @return a list of [BriefUnitGraph] objects.
     */
    private fun constructUnitGraphs(classStream: List<SootClass>) = classStream.asSequence()
        .map { it.methods.filter { mtd -> mtd.isConcrete }.toList() }.flatten()
        .distinct().toList().let { if (it.size >= 100000) it.parallelStream() else it.stream() }
        .filter { !it.isPhantom }.map { m ->
            runCatching { BriefUnitGraph(m.retrieveActiveBody()) }
                .onFailure { logger.warn("Unable to get method body for method ${m.name}.") }
                .getOrNull()
        }.asSequence().filterNotNull().toList()

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
        return edges.asSequence()
            .map { e ->
                runCatching { e.tgt.method() }
                    .onFailure { logger.warn("Unable to get method for externally referenced method ${e.tgt}.") }
                    .getOrNull()
            }
            .filterNotNull().toMutableList().apply { this.add(mtd) }
    }

    /**
     * Constructs the code-property graph from a method's [BriefUnitGraph].
     *
     * @param gs The [BriefUnitGraph] to construct the method head and body CPG from.
     * @return The given graph.
     */
    private fun baseCPGPass(gs: List<BriefUnitGraph>): List<BriefUnitGraph> {
        val astPass = ASTPass(driver)
        val cfgPass = CFGPass(driver)
        val pdgPass = PDGPass(driver)
        gs.forEach { g ->
            logger.debug("Projecting ${g.body.method}")
            // Build head
            SootToPlumeUtil.buildMethodHead(g.body.method, driver)
            // Build body
            pipeline(
                astPass::runPass,
                cfgPass::runPass,
                pdgPass::runPass,
            ).invoke(g)
        }
        return gs
    }


    /**
     * Once the method bodies are constructed, this function then connects calls to the called methods if present.
     *
     * @param gs The [BriefUnitGraph] from which calls are checked and connected to their referred methods.
     * @return The method from the given graph.
     */
    private fun constructCallGraphEdges(gs: List<BriefUnitGraph>): List<BriefUnitGraph> {
        if (ExtractorOptions.callGraphAlg != ExtractorOptions.CallGraphAlg.NONE) gs.forEach(callGraphBuilder::runPass)
        return gs
    }

    /**
     * Configure Soot options for CPG transformation.
     */
    private fun configureSoot() {
        // set application mode
        Options.v().set_app(true)
        // make sure classpath is configured correctly
        Options.v().set_soot_classpath(COMP_DIR)
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
        .removePrefix(COMP_DIR + File.separator)
        .replace(File.separator, ".")
        .removeSuffix(".class")

    /**
     * Given a list of class names, load them into the Scene.
     *
     * @param classNames A set of class files.
     * @return the given class files as a list of [SootClass].
     */
    private fun loadClassesIntoSoot(classNames: HashSet<JVMClassFile>): List<SootClass> {
        if (classNames.isEmpty()) return emptyList()
        classNames.map(this::getQualifiedClassPath).forEach(Scene.v()::addBasicClass)
        Scene.v().loadBasicClasses()
        Scene.v().loadDynamicClasses()
        val cs = classNames.map { Pair(it, getQualifiedClassPath(it)) }
            .map { Pair(it.first, Scene.v().loadClassAndSupport(it.second)) }
            .map { clsPair: Pair<File, SootClass> ->
                val f = clsPair.first
                val c = clsPair.second
                c.setApplicationClass(); putNewFileHashPair(c, f.hashCode().toString())
                c
            }
        when (ExtractorOptions.callGraphAlg) {
            ExtractorOptions.CallGraphAlg.CHA -> CHATransformer.v().transform()
            ExtractorOptions.CallGraphAlg.SPARK -> SparkTransformer.v().transform("", ExtractorOptions.sparkOpts)
            else -> Unit
        }
        return cs
    }

    /**
     * Clears resources of file and graph pointers.
     */
    private fun clear() {
        loadedFiles.clear()
        classToFileHash.clear()
        sootToPlume.clear()
        savedCallGraphEdges.clear()
        File(COMP_DIR).deleteRecursively()
        G.reset()
        G.v().resetSpark()
    }

}