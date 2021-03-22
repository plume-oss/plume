package io.github.plume.oss.passes.structure

import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.store.LocalCache
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.NAMESPACE
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

    private val namespaceCache = mutableMapOf<String, NewNamespaceBuilder>()
    private val cache = DriverCache(driver)

    /**
     * This pass will build and link file and namespace information, i.e.
     *
     *     NAMESPACE_BLOCK -REF-> NAMESPACE
     *     FILE -AST-> NAMESPACE_BLOCK
     *     NAMESPACE_BLOCK -SOURCE_FILE-> FILE
     */
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        createDefaultVertices()
        val ns = cs.distinctBy { it.packageName }.map(::getNamespace).toList()
        return cs.map { c -> buildFileAndPackage(c, ns) }.toList()
    }

    private fun buildFileAndPackage(c: SootClass, ns: List<NewNodeBuilder>): SootClass {
        val nb = cache.getOrMakeNamespaceBlock(c)
        val f = cache.getOrMakeFile(c)
        // (NAMESPACE_BLOCK) -REF-> (NAMESPACE)
        ns.find { it.build().properties().get(NAME).get() == c.packageName }
            ?.let { namespace -> driver.addEdge(nb, namespace, REF) }
        // (FILE) -AST-> (NAMESPACE_BLOCK)
        driver.addEdge(f, nb, AST)
        // (NAMESPACE_BLOCK) -SOURCE_FILE-> (FILE)
        driver.addEdge(nb, f, SOURCE_FILE)
        return c
    }

    private fun getNamespace(c: SootClass): NewNamespaceBuilder = namespaceCache[c.packageName]
        ?: (driver.getVerticesByProperty(NAME, c.packageName, NAMESPACE).firstOrNull() as NewNamespaceBuilder?
            ?: NewNamespaceBuilder().name(c.packageName).order(-1)).apply { namespaceCache[c.packageName] = this }

    private fun createDefaultVertices() {
        if (cache.tryGetFile(UNKNOWN) == null) {
            val unknownFile = NewFileBuilder().name(UNKNOWN).order(0).hash(Option.apply(UNKNOWN))
            driver.addVertex(unknownFile)
            LocalCache.addFile(unknownFile)
        }
        if (cache.tryGetNamespaceBlock(GLOBAL) == null) {
            val gNamespace = NewNamespaceBuilder().name(GLOBAL).order(0)
            val gNamespaceBlock = NewNamespaceBlockBuilder().name(GLOBAL).fullName(GLOBAL).order(0).filename("")
            driver.addEdge(gNamespaceBlock, gNamespace, REF)
            LocalCache.addNamespaceBlock(gNamespaceBlock)
            namespaceCache[GLOBAL] = gNamespace
        }
    }
}