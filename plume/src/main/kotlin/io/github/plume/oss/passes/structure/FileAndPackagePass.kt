package io.github.plume.oss.passes.structure

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.github.plume.oss.util.ExtractorConst.UNKNOWN
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.NAMESPACE
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespace
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBuilder
import scala.Option
import soot.SootClass

/**
 * Builds all file and package information for classes.
 */
class FileAndPackagePass(private val driver: IDriver) : IProgramStructurePass {

    /**
     * This pass will build and link file and namespace information, i.e.
     *
     *     NAMESPACE_BLOCK -REF-> NAMESPACE
     *     FILE -AST-> NAMESPACE_BLOCK
     *     NAMESPACE_BLOCK -SOURCE_FILE-> FILE
     */
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        createDefaultVertices()
        val ns = cs.map { it.packageName }.distinct().map { NewNamespaceBuilder().name(it).order(-1) }.toList()
        return cs.map { c -> buildFileAndPackage(c, ns) }.toList()
    }

    private fun buildFileAndPackage(c: SootClass, ns: List<NewNamespaceBuilder>): SootClass {
        val nb = buildNamespaceBlock(c)
        val f = buildFile(c)
        // (NAMESPACE_BLOCK) -REF-> (NAMESPACE)
        ns.find { it.build().name() == nb.build().name() }
            ?.let { namespace -> driver.addEdge(nb, namespace, REF) }
        // (FILE) -AST-> (NAMESPACE_BLOCK)
        driver.addEdge(f, nb, AST)
        // (NAMESPACE_BLOCK) -SOURCE_FILE-> (FILE)
        driver.addEdge(nb, f, SOURCE_FILE)
        return c
    }

    private fun buildNamespaceBlock(c: SootClass): NewNamespaceBlockBuilder {
        val fileName = SootToPlumeUtil.sootClassToFileName(c)
        return NewNamespaceBlockBuilder()
            .filename(fileName)
            .order(1)
            .name(c.packageName)
            .fullName("$fileName:${c.packageName}")
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
        }
        if (driver.getVerticesByProperty(NAME, GLOBAL).isEmpty()) {
            val gNamespace = NewNamespaceBuilder().name(GLOBAL).order(-1)
            val gNamespaceBlock = NewNamespaceBlockBuilder().name(GLOBAL).fullName(GLOBAL).order(-1).filename("")
            driver.addEdge(gNamespaceBlock, gNamespace, REF)
        }
    }
}