package io.github.plume.oss.passes

import io.github.plume.oss.Extractor
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.metrics.ExtractorTimeKey
import io.github.plume.oss.metrics.PlumeTimer
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes
import io.shiftleft.codepropertygraph.generated.NodeKeyNames
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.TYPE_DECL
import io.shiftleft.codepropertygraph.generated.nodes.Method
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Graph
import overflowdb.Node
import soot.SootClass

/**
 * Checks a list of given classes and any class which is in the database but has changed will be removed. This removal
 * involves deleting the associated FILE, METHOD, TYPE_DECL and any V where (TYPE_DECL)-AST->(v).
 */
class MarkForRebuildPass(private val driver: IDriver) : IProgramStructurePass {
    private val logger: Logger = LogManager.getLogger(MarkForRebuildPass::javaClass)

    private enum class FileChange { UPDATE, NEW, NOP }

    /**
     * This pass will check and remove any classes that require an update from the database.
     *
     * @return a list of classes which need to be built.
     */
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        driver.getProgramStructure().use { g ->
            val cStateList = cs.map { c -> checkIfClassNeedsAnUpdate(c, g) }.toList()
            cStateList.filter { it.second == FileChange.UPDATE }.forEach { dropClassFromGraph(it.first, g) }
            return cStateList.filterNot { it.second == FileChange.NOP }.map { it.first }.toList()
        }
    }

    /**
     * Checks the given class against the given graph. If the file from the class' hash matches a file in the graph but
     * the hashes differ, this will mark for rebuild.
     *
     * @param c The class to check.
     * @param g The graph to check against.
     * @return (c, UPDATE) if the class needs to be rebuilt, (c, NEW) if the class is unseen, and (c, NOP) if the given
     * class and the one in the database are the same.
     */
    private fun checkIfClassNeedsAnUpdate(c: SootClass, g: Graph): Pair<SootClass, FileChange> {
        val newCName = SootToPlumeUtil.sootClassToFileName(c)
        g.nodes(FILE).asSequence().find { it.property(NAME) == newCName }?.let { oldCNode: Node ->
            val currentCHash = Extractor.getFileHashPair(c)
            logger.info("Found an existing class with name ${c.name}...")
            return if (oldCNode.property(NodeKeyNames.HASH) != currentCHash) {
                logger.info("Class hashes differ, marking ${c.name} for rebuild.")
                Pair(c, FileChange.UPDATE)
            } else {
                logger.info("Classes are identical - no update necessary.")
                Pair(c, FileChange.NOP)
            }
        }
        logger.debug("No existing class for ${c.name} found.")
        return Pair(c, FileChange.NEW)
    }

    private fun dropClassFromGraph(c: SootClass, g: Graph) {
        val newCName = SootToPlumeUtil.sootClassToFileName(c)
        g.nodes(FILE).asSequence().find { it.property(NAME) == newCName }?.let { oldCNode: Node ->
            driver.getNeighbours(VertexMapper.mapToVertex(oldCNode)).use { neighbours ->
                val nodes = neighbours.nodes().asSequence().toList()
                dropMethods(nodes)
                dropFile(oldCNode)
                dropTypeDecl(c, g)
            }
        }
    }

    private fun dropMethods(ns: List<Node>) =
        ns.filterIsInstance<Method>().forEach {
            logger.debug("Deleting method for ${it.fullName()}")
            driver.deleteMethod(it.fullName())
        }

    private fun dropFile(c: Node) {
        logger.debug("Deleting $FILE for ${c.property(FULL_NAME)}")
        driver.deleteVertex(c.id(), FILE)
    }

    private fun dropTypeDecl(c: SootClass, g: Graph) = g.nodes(TYPE_DECL).asSequence()
        .filter { it.property(FULL_NAME) == c.type.toQuotedString() }
        .forEach { typeDecl ->
            logger.debug("Deleting $TYPE_DECL ${typeDecl.property(FULL_NAME)}")
            driver.getNeighbours(VertexMapper.mapToVertex(typeDecl)).use { n ->
                n.nodes(typeDecl.id()).next()
                    .out(EdgeTypes.AST)
                    .forEach {
                        logger.debug("Deleting (${it.label()}: ${it.id()})")
                        driver.deleteVertex(it.id(), it.label())
                    }
            }
            driver.deleteVertex(typeDecl.id(), TYPE_DECL)
        }
}
