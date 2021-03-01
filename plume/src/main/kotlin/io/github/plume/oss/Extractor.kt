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
import io.github.plume.oss.domain.files.*
import io.github.plume.oss.domain.mappers.VertexMapper.mapToVertex
import io.github.plume.oss.drivers.*
import io.github.plume.oss.graph.ASTBuilder
import io.github.plume.oss.graph.CFGBuilder
import io.github.plume.oss.graph.CallGraphBuilder
import io.github.plume.oss.graph.PDGBuilder
import io.github.plume.oss.options.ExtractorOptions
import io.github.plume.oss.util.DiffGraphUtil
import io.github.plume.oss.util.ExtractorConst.LANGUAGE_FRONTEND
import io.github.plume.oss.util.ExtractorConst.plumeVersion
import io.github.plume.oss.util.ResourceCompilationUtil.COMP_DIR
import io.github.plume.oss.util.ResourceCompilationUtil.compileJavaFiles
import io.github.plume.oss.util.ResourceCompilationUtil.moveClassFiles
import io.github.plume.oss.util.SootParserUtil.determineModifiers
import io.github.plume.oss.util.SootToPlumeUtil
import io.github.plume.oss.util.SootToPlumeUtil.buildClassStructure
import io.github.plume.oss.util.SootToPlumeUtil.buildTypeDeclaration
import io.github.plume.oss.util.SootToPlumeUtil.obtainModifiersFromTypeDeclVert
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.EdgeTypes.SOURCE_FILE
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.semanticcpg.passes.languagespecific.fuzzyc.TypeDeclStubCreator
import io.shiftleft.semanticcpg.passes.linking.linker.Linker
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Graph
import overflowdb.Node
import scala.Option
import scala.jdk.CollectionConverters
import soot.*
import soot.jimple.*
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
import kotlin.streams.toList
import io.shiftleft.codepropertygraph.generated.nodes.File as ODBFile

/**
 * The main entrypoint of the extractor from which the CPG will be created.
 *
 * @param driver the [IDriver] with which the graph will be constructed with.
 */
class Extractor(val driver: IDriver) {
    private val logger: Logger = LogManager.getLogger(Extractor::javaClass)

    private val loadedFiles: HashSet<PlumeFile> = HashSet()
    private val astBuilder: ASTBuilder
    private val cfgBuilder: CFGBuilder
    private val pdgBuilder: PDGBuilder
    private val callGraphBuilder: CallGraphBuilder
    private lateinit var programStructure: Graph

    init {
        File(COMP_DIR).let { f -> if (f.exists()) f.deleteRecursively(); f.deleteOnExit() }
        checkDriverConnection(driver)
        astBuilder = ASTBuilder(driver)
        cfgBuilder = CFGBuilder(driver)
        pdgBuilder = PDGBuilder(driver)
        callGraphBuilder = CallGraphBuilder(driver)
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

        /**
         * Saves call graph edges to the [NewMethod] from the [NewCall].
         *
         * @param fullName The method full name.
         * @param call The source [NewCall].
         */
        fun saveCallGraphEdge(fullName: String, call: NewCallBuilder) {
            if (!savedCallGraphEdges.containsKey(fullName)) savedCallGraphEdges[fullName] = mutableListOf(call)
            else savedCallGraphEdges[fullName]?.add(call)
        }

        /**
         * Retrieves all the incoming [NewCall]s from the given [NewMethod].
         *
         * @param fullName The method full name.
         */
        fun getIncomingCallGraphEdges(fullName: String) = savedCallGraphEdges[fullName]
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
                unzipArchive(ZipFile(f)).forEach { loadedFiles.add(FileFactory(it)) }
            } else {
                loadedFiles.add(FileFactory(f))
            }
        }
        return this
    }

    private fun unzipArchive(zf: ZipFile) = sequence {
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
    private fun compileLoadedFiles(files: HashSet<PlumeFile>): HashSet<JVMClassFile> {
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
        if (splitFiles.keys.contains(SupportedFile.JAVA) || splitFiles.keys.contains(SupportedFile.JVM_CLASS)) {
            addMetaDataInfo()
        }
        return splitFiles.keys.map {
            val filesToCompile = (splitFiles[it] ?: emptyList<JVMClassFile>()).toList()
            return@map when (it) {
                SupportedFile.JAVA -> compileJavaFiles(filesToCompile)
                SupportedFile.JVM_CLASS -> moveClassFiles(filesToCompile.map { f -> f as JVMClassFile }.toList())
            }
        }.asSequence().flatten().toHashSet()
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
        configureSoot()
        val compiledFiles = compileLoadedFiles(loadedFiles)
        val classStream = loadClassesIntoSoot(compiledFiles)
        when (ExtractorOptions.callGraphAlg) {
            ExtractorOptions.CallGraphAlg.CHA -> CHATransformer.v().transform()
            ExtractorOptions.CallGraphAlg.SPARK -> SparkTransformer.v().transform("", ExtractorOptions.sparkOpts)
            else -> Unit
        }
        // Initialize program structure graph and scan for an existing CPG
        programStructure = driver.getProgramStructure()
        classStream.forEach(this::analyseExistingCPGs)
        // Update program structure after sub-graphs which will change are discarded
        programStructure.close()
        programStructure = driver.getProgramStructure()
        // Setup defaults
        setUpDefaultStructure()
        // Load all methods to construct the CPG from and convert them to UnitGraph objects
        val graphs = constructUnitGraphs(classStream)
        // Build external types from fields and locals
        createExternalTypes(
            classStream = classStream,
            typeStream = classStream.asSequence().map { it.fields }.flatten().map { it.type }
        )
        createExternalTypes(
            classStream = classStream,
            typeStream = graphs.asSequence().map { it.body.locals + it.body.parameterLocals }.flatten().map { it.type }
        )
        // Construct the CPGs for methods
        graphs.map(this::constructCPG)
            .toList().asSequence()
            .map(this::constructCallGraphEdges)
            .map { it.declaringClass }.distinct().toList()
            .forEach(this::constructStructure)
        // Connect methods to their type declarations and source files (if present)
        graphs.forEach { SootToPlumeUtil.connectMethodToTypeDecls(it.body.method, driver) }
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
            val cpg = Cpg.apply(g)
            listOf(
                TypeDeclStubCreator(cpg),
                Linker(cpg),
            ).map { it.run() }
                .map(CollectionConverters::IteratorHasAsJava)
                .flatMap { it.asJava().asSequence() }
                .forEach { DiffGraphUtil.processDiffGraph(driver, it) }
        }
        driver.getMethodNames().forEach { mName ->
            driver.getMethod(mName).use { g ->
                val cpg = Cpg.apply(g)
                val reachingDefPass = ReachingDefPass(cpg)
                g.nodes(METHOD).asSequence().filterIsInstance<Method>()
                    .map(reachingDefPass::runOnPart)
                    .map(CollectionConverters::IteratorHasAsJava)
                    .flatMap { it.asJava().asSequence() }
                    .forEach { DiffGraphUtil.processDiffGraph(driver, it) }
            }
        }
        return this
    }

    /**
     * Creates [TypeDecl] from external [soot.Type]s. This also links the [TypeDecl]s to their modifiers and the
     * unknown file vertex.
     *
     * @param classStream The stream of application [SootClass] to separate external classes from.
     * @param typeStream The stream of all [soot.Type]s.
     */
    private fun createExternalTypes(classStream: List<SootClass>, typeStream: Sequence<soot.Type>) {
        typeStream.distinct()
            .filter { t -> !classStream.any { it.name == t.toString() } }
            .filter {
                !programStructure.nodes(TYPE_DECL).asSequence().any { n -> n.property(FULL_NAME) == it.toString() }
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

    /**
     * Load all methods to construct the CPG from and convert them to [BriefUnitGraph] objects.
     *
     * @param classStream A stream of [SootClass] to construct [BriefUnitGraph] from.
     * @return a list of [BriefUnitGraph] objects.
     */
    private fun constructUnitGraphs(classStream: List<SootClass>) = classStream.asSequence()
        .map { it.methods.filter { mtd -> mtd.isConcrete }.toList() }.flatten()
        .let {
            if (ExtractorOptions.callGraphAlg == ExtractorOptions.CallGraphAlg.NONE)
                it else it.map(this::addExternallyReferencedMethods).flatten()
        }
        .distinct().toList().let { if (it.size >= 100000) it.parallelStream() else it.stream() }
        .filter { !it.isPhantom }.map { m ->
            runCatching { BriefUnitGraph(m.retrieveActiveBody()) }
                .onFailure { logger.warn("Unable to get method body for method ${m.name}.") }
                .getOrNull()
        }.asSequence().filterNotNull().toList()

    /**
     * Sets up default vertices for placeholders like unknown files.
     */
    private fun setUpDefaultStructure() {
        val unknown = io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN()
        if (programStructure.nodes(FILE).asSequence<Node>().none { f: Node -> f.property(NAME) == unknown }) {
            val unknownFile = NewFileBuilder().name(unknown).order(0).hash(Option.apply(unknown))
            driver.addVertex(unknownFile)
            val fileNode = unknownFile.build()
            programStructure.addNode(unknownFile.id(), fileNode.label()).let { n ->
                fileNode.properties().foreach { e -> n.setProperty(e._1, e._2) }
            }
            addSootToPlumeAssociation(unknown, unknownFile)
        }
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
        return edges.asSequence()
            .map { e ->
                runCatching { e.tgt.method() }
                    .onFailure { logger.warn("Unable to get method for externally referenced method ${e.tgt}.") }
                    .getOrNull()
            }
            .filterNotNull().toMutableList().apply { this.add(mtd) }
    }

    /**
     * Constructs type, package, and source file information from the given class.
     *
     * @param cls The [SootClass] containing the information to build program structure information from.
     */
    private fun constructStructure(cls: SootClass) {
        if (programStructure.nodes(FILE).asSequence()
                .none { it.property(NAME) == SootToPlumeUtil.sootClassToFileName(cls) }
        ) {
            logger.debug("Building file, namespace, and type declaration for ${cls.name}")
            val file = buildClassStructure(cls, driver)
            val typeDecl = buildTypeDeclaration(cls.type, false)
            determineModifiers(cls.modifiers)
                .mapIndexed { i, m -> NewModifierBuilder().modifierType(m).order(i + 1) }
                .forEach { driver.addEdge(typeDecl, it, AST) }
            cls.fields.forEachIndexed { i, field ->
                SootToPlumeUtil.projectMember(field, i + 1).let { memberVertex ->
                    driver.addEdge(typeDecl, memberVertex, AST)
                    addSootToPlumeAssociation(field, memberVertex)
                }
            }
            driver.addEdge(typeDecl, file, SOURCE_FILE)
            addSootToPlumeAssociation(cls, typeDecl)
        }
    }

    /**
     * Constructs the code-property graph from a method's [BriefUnitGraph].
     *
     * @param graph The [BriefUnitGraph] to construct the method head and body CPG from.
     * @return The given graph.
     */
    private fun constructCPG(graph: BriefUnitGraph): BriefUnitGraph {
        // If file does not exists then rebuild, else update
        val cls = graph.body.method.declaringClass
        val files = programStructure.nodes { it == ODBFile.Label() }.asSequence()
        if (files.none { it.property(NAME) == SootToPlumeUtil.sootClassToFileName(cls) }) {
            logger.debug("Projecting ${graph.body.method}")
            // Build head
            SootToPlumeUtil.buildMethodHead(graph.body.method, driver)
            // Build body
            astBuilder.buildMethodBody(graph)
            cfgBuilder.buildMethodBody(graph)
            pdgBuilder.buildMethodBody(graph)
        } else {
            logger.debug("${graph.body.method} source file found in CPG, no need to build")
        }
        return graph
    }

    private fun analyseExistingCPGs(cls: SootClass) {
        val currentFileHash = getFileHashPair(cls)
        val files = programStructure.nodes { it == FILE }.asSequence()
        logger.debug("Looking for existing file vertex for ${cls.name} from given file hash $currentFileHash")
        files.firstOrNull { it.property(NAME) == SootToPlumeUtil.sootClassToFileName(cls) }?.let { fileV ->
            if (fileV.property(HASH) != currentFileHash) {
                logger.debug("Existing class was found and file hashes do not match, marking for rebuild.")
                // Rebuild
                driver.getNeighbours(mapToVertex(fileV)).use { neighbours ->
                    neighbours.nodes { it == Method.Label() }.forEach { mtdV: Node ->
                        val mtd1 = (mapToVertex(mtdV) as NewMethodBuilder).build()
                        logger.debug(
                            "Deleting method and saving incoming call graph edges for " +
                                    "${mtd1.fullName()} ${mtd1.signature()}"
                        )
                        driver.getMethod(mtd1.fullName(), false).use { g ->
                            g.nodes { it == Method.Label() }.asSequence().firstOrNull()?.let { mtdV: Node ->
                                val mtd2 = mapToVertex(mtdV) as NewMethodBuilder
                                val builtMtd2 = mtd2.build()
                                driver.getNeighbours(mtd2).use { ns ->
                                    if (ns.V(mtdV.id()).hasNext()) {
                                        ns.V(mtdV.id()).next().`in`(CALL).asSequence()
                                            .filterIsInstance<Call>()
                                            .forEach {
                                                saveCallGraphEdge(
                                                    builtMtd2.fullName(),
                                                    mapToVertex(it) as NewCallBuilder
                                                )
                                            }
                                    }
                                }
                            }
                        }
                        driver.deleteMethod(mtd1.fullName())
                    }
                }
                logger.debug("Deleting $fileV")
                driver.deleteVertex(fileV.id(), fileV.label())
                // Delete TypeDecls
                programStructure.nodes { it == TYPE_DECL }.asSequence()
                    .filter { it.property(FULL_NAME) == cls.type.toQuotedString() }
                    .forEach { typeDecl ->
                        logger.debug("Deleting $typeDecl")
                        driver.getNeighbours(NewTypeDeclBuilder().id(typeDecl.id())).use { g ->
                            g.nodes(typeDecl.id()).next().out(AST)
                                .forEach { logger.debug("Deleting $it"); driver.deleteVertex(it.id(), it.label()) }
                        }
                        driver.deleteVertex(typeDecl.id(), TYPE_DECL)
                    }
            } else {
                logger.debug("Existing class was found and file hashes match, no need to rebuild.")
            }
        }
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
        classNames.map(this::getQualifiedClassPath).forEach(Scene.v()::addBasicClass)
        Scene.v().loadBasicClasses()
        Scene.v().loadDynamicClasses()
        return classNames.map { Pair(it, getQualifiedClassPath(it)) }
            .map { Pair(it.first, Scene.v().loadClassAndSupport(it.second)) }
            .map { clsPair: Pair<File, SootClass> ->
                val f = clsPair.first
                val cls = clsPair.second
                cls.setApplicationClass(); putNewFileHashPair(cls, f.hashCode().toString())
                cls
            }
    }

    /**
     * Clears resources of file and graph pointers.
     */
    private fun clear() {
        loadedFiles.clear()
        classToFileHash.clear()
        sootToPlume.clear()
        savedCallGraphEdges.clear()
        programStructure.close()
        File(COMP_DIR).deleteRecursively()
        G.reset()
        G.v().resetSpark()
    }

}