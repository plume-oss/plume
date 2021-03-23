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
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.drivers.GremlinDriver
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.drivers.Neo4jDriver
import io.github.plume.oss.drivers.OverflowDbDriver
import io.github.plume.oss.metrics.ExtractorTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.github.plume.oss.options.ExtractorOptions
import io.github.plume.oss.passes.DataFlowPass
import io.github.plume.oss.passes.graph.BaseCPGPass
import io.github.plume.oss.passes.graph.CGPass
import io.github.plume.oss.passes.method.MethodStubPass
import io.github.plume.oss.passes.structure.ExternalTypePass
import io.github.plume.oss.passes.structure.FileAndPackagePass
import io.github.plume.oss.passes.structure.MarkForRebuildPass
import io.github.plume.oss.passes.structure.TypePass
import io.github.plume.oss.passes.type.GlobalTypePass
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.ExtractorConst.LANGUAGE_FRONTEND
import io.github.plume.oss.util.ExtractorConst.plumeVersion
import io.github.plume.oss.util.ResourceCompilationUtil
import io.github.plume.oss.util.ResourceCompilationUtil.COMP_DIR
import io.github.plume.oss.util.ResourceCompilationUtil.TEMP_DIR
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.INHERITS_FROM
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes.META_DATA
import io.shiftleft.codepropertygraph.generated.NodeTypes.METHOD
import io.shiftleft.codepropertygraph.generated.nodes.NewMetaDataBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.*
import soot.jimple.spark.SparkTransformer
import soot.jimple.toolkits.callgraph.CHATransformer
import soot.options.Options
import soot.toolkits.graph.BriefUnitGraph
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
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
    private val cache = DriverCache(driver)

    init {
        File(COMP_DIR).let { f -> if (f.exists()) f.deleteRecursively(); f.deleteOnExit() }
        checkDriverConnection(driver)
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
        PlumeTimer.startTimerOn(ExtractorTimeKey.COMPILING_AND_UNPACKING)
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
        PlumeTimer.stopTimerOn(ExtractorTimeKey.COMPILING_AND_UNPACKING)
        return this
    }

    private fun upsertMetaData() {
        val maybeMetaData = driver.getMetaData()
        if (maybeMetaData != null) {
            val metaData = maybeMetaData.build()
            if (metaData.language() != LANGUAGE_FRONTEND)
                driver.updateVertexProperty(maybeMetaData.id(), META_DATA, LANGUAGE, LANGUAGE_FRONTEND)
            if (metaData.version() != plumeVersion)
                driver.updateVertexProperty(maybeMetaData.id(), META_DATA, VERSION, plumeVersion)
        } else {
            driver.addVertex(NewMetaDataBuilder().language(LANGUAGE_FRONTEND).version(plumeVersion))
        }
    }

    /**
     * Projects all loaded classes to the graph database.
     */
    fun project(): Extractor {
        /*
            Load and compile files then feed them into Soot
         */
        if (loadedFiles.isEmpty()) return apply { logger.info("No files loaded, returning") }
        val (nCs, nSs, nUs) = loadedFileGroupCount()
        logger.info("Preparing $nCs class and $nSs source file(s). Ignoring $nUs unsupported file(s).")
        val cs = mutableListOf<SootClass>()
        val compiledFiles = mutableSetOf<JavaClassFile>()
        PlumeTimer.measure(ExtractorTimeKey.COMPILING_AND_UNPACKING) {
            ResourceCompilationUtil.compileLoadedFiles(loadedFiles).toCollection(compiledFiles)
            if (compiledFiles.isNotEmpty()) upsertMetaData()
        }
        if (compiledFiles.isEmpty()) return this
        logger.info("Loading all classes into Soot")
        PlumeTimer.measure(ExtractorTimeKey.SOOT) {
            configureSoot()
            loadClassesIntoSoot(compiledFiles).toCollection(cs)
        }
        compiledFiles.clear() // Done using compiledFiles
        /*
            Build program structure and remove sub-graphs which need to be rebuilt
         */
        logger.info("Building internal program structure and type information")
        val csToBuild = mutableListOf<SootClass>()
        PlumeTimer.measure(ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING) {
            // First read the existing TYPE, TYPE_DECL, and FILEs from the driver and load it into the cache
            pipeline(
                MarkForRebuildPass(driver)::runPass,
                FileAndPackagePass(driver)::runPass,
                TypePass(driver)::runPass,
            ).invoke(cs).toCollection(csToBuild)
        }
        cs.clear() // Done using cs
        /*
            Build Soot Unit graphs and extract types
         */
        logger.info("Building UnitGraphs")
        val sootUnitGraphs = mutableListOf<BriefUnitGraph>()
        PlumeTimer.measure(ExtractorTimeKey.SOOT) { constructUnitGraphs(csToBuild).toCollection(sootUnitGraphs) }
        /*
            Obtain all referenced types from fields, returns, and locals
         */
        val ts = mutableListOf<Type>()
        PlumeTimer.measure(ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING) {
            val fieldsAndRets = csToBuild.map { c -> c.fields.map { it.type } + c.methods.map { it.returnType } }
                .flatten().toSet()
            val locals = sootUnitGraphs.map { it.body.locals + it.body.parameterLocals }
                .flatten().map { it.type }.toSet()
            val returns = sootUnitGraphs.map { it.body.method.returnType }.toSet()
            (fieldsAndRets + locals + returns).distinct().toCollection(ts)
        }
        /*
            Build primitive type information
         */
        logger.info("Building primitive type information")
        logger.debug("All referenced types: ${ts.groupBy { it.javaClass }.mapValues { it.value.size }}}")
        PlumeTimer.measure(ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING) {
            pipeline(
                GlobalTypePass(driver)::runPass
            ).invoke(ts)
        }
        /*
            Obtain inheritance information
        */
        logger.info("Obtaining class hierarchy")
        val parentToChildCs: MutableList<Pair<SootClass, Set<SootClass>>> = mutableListOf()
        PlumeTimer.measure(ExtractorTimeKey.SOOT) {
            val fh = FastHierarchy()
            Scene.v().classes.asSequence()
                .map { Pair(it, fh.getSubclassesOf(it)) }
                .map { Pair(it.first, csToBuild.intersect(it.second)) }
                .filter { it.second.isNotEmpty() }.toCollection(parentToChildCs)
        }
        /*
            Build external type and method stubs
         */
        logger.info("Building external program structure and type information")
        PlumeTimer.measure(ExtractorTimeKey.BASE_CPG_BUILDING) {
            val referencedTypes = ts.filterNot { it is PrimType }
                .map { if (it is ArrayType) it.baseType else it } + parentToChildCs.map { it.first.type }
            val filteredExtTypes = referencedTypes.minus(csToBuild.map { it.type }).filterIsInstance<RefType>().toList()
            pipeline(
                FileAndPackagePass(driver)::runPass,
                ExternalTypePass(driver)::runPass,
            ).invoke(filteredExtTypes.map { it.sootClass })
        }
        /*
            Build inheritance edges, i.e.
            TYPE_DECL -INHERITS_FROM-> TYPE
        */
        PlumeTimer.measure(ExtractorTimeKey.PROGRAM_STRUCTURE_BUILDING) {
            parentToChildCs.forEach { (c, children) ->
                cache.tryGetType(c.type.toQuotedString())?.let { t ->
                    children.intersect(csToBuild)
                        .mapNotNull { child -> cache.tryGetTypeDecl(child.type.toQuotedString()) }
                        .forEach { td -> driver.addEdge(td, t, INHERITS_FROM) }
                }
            }
        }
        csToBuild.clear() // Done using csToBuild
        /*
            Construct the CPGs for methods
         */
        PlumeTimer.measure(ExtractorTimeKey.BASE_CPG_BUILDING) { buildMethods(parentToChildCs, sootUnitGraphs) }
        // Clear all Soot resources and storage
        this.clear()
        /*
            Method body level analysis - only done on new/updated methods
         */
        logger.info("Running data flow passes")
        PlumeTimer.measure(ExtractorTimeKey.DATA_FLOW_PASS) { DataFlowPass(driver).runPass() }
        PlumeStorage.methodCpgs.clear()
        return this
    }

    private fun buildMethods(
        parentToChildCs: MutableList<Pair<SootClass, Set<SootClass>>>,
        sootUnitGraphs: MutableList<BriefUnitGraph>
    ) {
        val allMs: List<SootMethod> = (parentToChildCs.flatMap { it.second + it.first }.flatMap { it.methods }
                + sootUnitGraphs.map { it.body.method }).distinct().toList()
        val existingMs = driver.getPropertyFromVertices<String>(FULL_NAME, METHOD).toSet()
        // Create method heads while avoiding duplication
        val headsToBuild = allMs.filterNot { sm ->
            val (fullName, _, _) = SootToPlumeUtil.methodToStrings(sm)
            existingMs.contains(fullName)
        }
        // Create method bodies while avoiding duplication
        val bodiesToBuild = sootUnitGraphs.filterNot { sm ->
            val (fullName, _, _) = SootToPlumeUtil.methodToStrings(sm.body.method)
            existingMs.contains(fullName)
        }.toList()
        val chunkSize = ExtractorOptions.methodChunkSize
        val nThreads = (bodiesToBuild.size / chunkSize)
            .coerceAtLeast(1)
            .coerceAtMost(Runtime.getRuntime().availableProcessors())
        val channel = Channel<DeltaGraph>()
        try {
            logger.info("Spawning $nThreads thread(s).")
            // Create jobs in chunks and submit these jobs to a thread pool
            val threadPool = Executors.newFixedThreadPool(nThreads)
            try {
                logger.info("Building ${headsToBuild.size} method heads")
                runBlocking {
                    // Producer: Build method bodies and channel them through as delta graphs
                    launch {
                        headsToBuild.chunked(chunkSize)
                            .map { chunk -> Runnable { buildMethodHeads(chunk, channel) } }
                            .forEach(threadPool::submit)
                    }
                    // Consumer: Receive delta graphs and write changes to the driver in serial
                    runInsideProgressBar("Method Heads", headsToBuild.size.toLong()) { pb ->
                        runBlocking {
                            repeat(headsToBuild.size) { channel.receive().apply(driver); pb?.step() }
                        }
                    }
                    logger.info("All ${headsToBuild.size} method heads have been applied to the driver")

                }
                logger.info("Building ${bodiesToBuild.size} method bodies")
                runBlocking {
                    // Producer: Build method bodies and channel them through as delta graphs
                    launch {
                        bodiesToBuild.chunked(chunkSize)
                            .map { chunk -> Runnable { buildBaseCPGs(chunk, channel) } }
                            .forEach(threadPool::submit)
                    }
                    // Consumer: Receive delta graphs and write changes to the driver in serial
                    runInsideProgressBar("Method Bodies", bodiesToBuild.size.toLong()) { pb ->
                        runBlocking {
                            repeat(bodiesToBuild.size) { channel.receive().apply(driver); pb?.step() }
                        }
                    }
                    logger.info("All ${bodiesToBuild.size} method bodies have been applied to the driver")
                }
            } finally {
                threadPool.shutdown()
            }
            // Connect call edges. This is not done in parallel but asynchronously
            logger.info("Constructing call graph edges")
            runBlocking {
                if (ExtractorOptions.callGraphAlg != ExtractorOptions.CallGraphAlg.NONE) {
                    // Producer: Build method calls and channel them through as delta graphs
                    launch {
                        bodiesToBuild.chunked(chunkSize).forEach { chunk -> buildCallGraph(chunk, channel) }
                    }
                    // Consumer: Receive delta graphs and write changes to the driver in serial
                    runBlocking {
                        repeat(bodiesToBuild.size) { channel.receive().apply(driver) }
                    }
                    logger.info("All ${bodiesToBuild.size} method calls have been applied to the driver")
                }
            }
        } finally {
            channel.close()
        }
    }

    private fun runInsideProgressBar(pbName: String, barMax: Long, f: (pb: ProgressBar?) -> Unit) {
        val level = logger.level
        if (level >= Level.INFO) {
            ProgressBar(
                pbName,
                barMax,
                1000,
                System.out,
                ProgressBarStyle.ASCII,
                "",
                1,
                false,
                null,
                ChronoUnit.SECONDS,
                0L,
                Duration.ZERO
            ).use { pb -> f(pb) }
        } else {
            f(null)
        }
    }

    private fun loadedFileGroupCount(): Triple<Int?, Int?, Int?> {
        val groupedFs = loadedFiles.groupBy { it.fileType }.mapValues { it.value.size }.toMap()
        val nCs = if (groupedFs.containsKey(PlumeFileType.JAVA_CLASS)) groupedFs[PlumeFileType.JAVA_CLASS] else 0
        val nSs = if (groupedFs.containsKey(PlumeFileType.JAVA_SOURCE)) groupedFs[PlumeFileType.JAVA_SOURCE] else 0
        val nUs = if (groupedFs.containsKey(PlumeFileType.UNSUPPORTED)) groupedFs[PlumeFileType.UNSUPPORTED] else 0
        return Triple(nCs, nSs, nUs)
    }

    private fun <T> pipeline(vararg functions: (T) -> T): (T) -> T =
        { functions.fold(it) { output, stage -> stage.invoke(output) } }

    /**
     * Constructs the method heads concurrently.
     *
     * @param ms The list of method heads as [SootMethod]s.
     */
    private fun buildMethodHeads(ms: List<SootMethod>, channel: Channel<DeltaGraph>) = runBlocking {
        ms.forEach { m -> launch { channel.send(MethodStubPass(m).runPass()) } }
    }

    /**
     * Constructs the method bodies concurrently.
     *
     * @param gs The list of method bodies as [BriefUnitGraph]s.
     */
    private fun buildBaseCPGs(gs: List<BriefUnitGraph>, channel: Channel<DeltaGraph>) = runBlocking {
        gs.forEach { g ->
            launch {
                val dg = BaseCPGPass(g).runPass()
                channel.send(dg)
                val (fullName, _, _) = SootToPlumeUtil.methodToStrings(g.body.method)
                PlumeStorage.methodCpgs[fullName] = dg
            }
        }
    }

    /**
     * Constructs the CALL edges concurrently.
     *
     * @param gs The list of method bodies as [BriefUnitGraph]s.
     */
    private fun buildCallGraph(gs: List<BriefUnitGraph>, channel: Channel<DeltaGraph>) = runBlocking {
        gs.forEach { g -> launch { channel.send(CGPass(g, driver).runPass()) } }
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
        // Soot output to Plume temp folder
        Options.v().set_output_dir("$TEMP_DIR${File.separator}/sootOutput")
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
    private fun loadClassesIntoSoot(classNames: Set<JavaClassFile>): List<SootClass> {
        if (classNames.isEmpty()) return emptyList()
        classNames.map(this::getQualifiedClassPath).forEach(Scene.v()::addBasicClass)
        Scene.v().loadBasicClasses()
        Scene.v().loadDynamicClasses()
        val cs = classNames.map { Pair(it, getQualifiedClassPath(it)) }
            .map { Pair(it.first, Scene.v().loadClassAndSupport(it.second)) }
            .map { clsPair: Pair<File, SootClass> ->
                clsPair.second.apply {
                    val f = clsPair.first
                    this.setApplicationClass()
                    PlumeStorage.storeFileHash(this, f.hashCode().toString())
                }
            }
        when (ExtractorOptions.callGraphAlg) {
            ExtractorOptions.CallGraphAlg.CHA -> CHATransformer.v().transform()
            ExtractorOptions.CallGraphAlg.SPARK -> SparkTransformer.v().transform("", ExtractorOptions.sparkOpts)
            else -> Unit
        }
        return cs
    }

    /**
     * Clears build resources, loaded files, and resets Soot.
     */
    private fun clear() {
        PlumeStorage.clear()
        loadedFiles.clear()
        File(COMP_DIR).deleteRecursively()
        G.reset()
    }

}