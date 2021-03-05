package io.github.plume.oss.passes

import io.github.plume.oss.Extractor
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.FILE
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBlockBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewNamespaceBuilder
import scala.Option
import soot.SootClass

/**
 * Builds all the namespace blocks from the list of classes.
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
        createUnknownIfNotExists()
        val ns = cs.map { it.packageName }.distinct().map { NewNamespaceBuilder().name(it).order(-1) }.toList()
        return cs.map { c -> buildFileAndPackage(c, ns) }.toList()
    }

    private fun createUnknownIfNotExists() {
        val unknown = io.shiftleft.semanticcpg.language.types.structure.File.UNKNOWN()
        driver.getProgramStructure().use { g ->
            if (g.nodes(FILE).asSequence().none { f -> f.property(NAME) == unknown }) {
                val unknownFile = NewFileBuilder().name(unknown).order(0).hash(Option.apply(unknown))
                driver.addVertex(unknownFile)
                val fileNode = unknownFile.build()
                g.addNode(unknownFile.id(), fileNode.label()).let { n ->
                    fileNode.properties().foreach { e -> n.setProperty(e._1, e._2) }
                }
            }
        }
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
}