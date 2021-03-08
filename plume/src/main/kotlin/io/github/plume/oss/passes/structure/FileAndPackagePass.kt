package io.github.plume.oss.passes.structure

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import scala.Option
import soot.SootClass

/**
 * Builds all file and package information for classes.
 */
class FileAndPackagePass(private val driver: IDriver) : IProgramStructurePass {

    private val nodeCache = mutableSetOf<NewNodeBuilder>()

    /**
     * This pass will build and link file and namespace information, i.e.
     *
     *     NAMESPACE_BLOCK -REF-> NAMESPACE
     *     FILE -AST-> NAMESPACE_BLOCK
     *     NAMESPACE_BLOCK -SOURCE_FILE-> FILE
     */
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        createDefaultVertices()
        val ns = cs.distinctBy { it.packageName }
            .map { c ->
                driver.getVerticesByProperty(NAME, c.packageName, NAMESPACE).firstOrNull()
                    ?: NewNamespaceBuilder().name(c.packageName).order(-1)
            }
            .map { nodeCache.add(it); it }
            .toList()
        return cs.map { c -> buildFileAndPackage(c, ns) }.toList()
    }

    private fun buildFileAndPackage(c: SootClass, ns: List<NewNodeBuilder>): SootClass {
        val nb = getNamespaceBlock(c)
        val f = getFile(c)
        // (NAMESPACE_BLOCK) -REF-> (NAMESPACE)
        ns.find { it.build().properties().get(NAME).get() == c.packageName }
            ?.let { namespace -> driver.addEdge(nb, namespace, REF) }
        // (FILE) -AST-> (NAMESPACE_BLOCK)
        driver.addEdge(f, nb, AST)
        // (NAMESPACE_BLOCK) -SOURCE_FILE-> (FILE)
        driver.addEdge(nb, f, SOURCE_FILE)
        // Cache additions
        nodeCache.addAll(listOf(nb, f))
        return c
    }

    /**
     * This will first see if there is a NAMESPACE_BLOCK in the cache, if not then will look in the graph,
     * if not then will build a new vertex.
     */
    private fun getNamespaceBlock(c: SootClass): NewNodeBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        nodeCache.filterIsInstance<NewNamespaceBlockBuilder>()
            .find { it.build().properties().get(FULL_NAME).get() == "$fileName:${c.packageName}" }
            ?.let { return it }
        return driver.getVerticesByProperty(
            propertyKey = FULL_NAME,
            propertyValue = "$fileName:${c.packageName}",
            label = NAMESPACE_BLOCK
        ).firstOrNull() ?: buildNamespaceBlock(c)
    }

    private fun buildNamespaceBlock(c: SootClass): NewNamespaceBlockBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        return NewNamespaceBlockBuilder()
            .filename(fileName)
            .order(1)
            .name(c.packageName)
            .fullName("$fileName:${c.packageName}")
    }

    /**
     * This will first see if there is a FILE in the cache, if not then will look in the graph,
     * if not then will build a new vertex.
     */
    private fun getFile(c: SootClass): NewNodeBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        nodeCache.filterIsInstance<NewFileBuilder>()
            .find { it.build().properties().get(NAME).get() == fileName }
            ?.let { return it }
        return driver.getVerticesByProperty(
            propertyKey = NAME,
            propertyValue = fileName,
            label = FILE
        ).firstOrNull() ?: buildFile(c)
    }

    private fun buildFile(c: SootClass): NewFileBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        val fileHash = Extractor.getFileHashPair(c)
        return NewFileBuilder()
            .name(fileName)
            .order(1)
            .hash(Option.apply(fileHash))
    }

    private fun createDefaultVertices() {
        if (driver.getVerticesByProperty(NAME, UNKNOWN, FILE).isEmpty()) {
            val unknownFile = NewFileBuilder().name(UNKNOWN).order(-1).hash(Option.apply(UNKNOWN))
            driver.addVertex(unknownFile)
            nodeCache.add(unknownFile)
        }
        if (driver.getVerticesByProperty(NAME, GLOBAL).isEmpty()) {
            val gNamespace = NewNamespaceBuilder().name(GLOBAL).order(-1)
            val gNamespaceBlock = NewNamespaceBlockBuilder().name(GLOBAL).fullName(GLOBAL).order(-1).filename("")
            driver.addEdge(gNamespaceBlock, gNamespace, REF)
            nodeCache.addAll(listOf(gNamespace, gNamespaceBlock))
        }
    }
}