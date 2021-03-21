package io.github.plume.oss.passes.structure

import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IProgramStructurePass
import io.github.plume.oss.store.LocalCache
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.*
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.*
import io.shiftleft.codepropertygraph.generated.NodeTypes.FILE
import io.shiftleft.codepropertygraph.generated.NodeTypes.TYPE_DECL
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Node
import soot.SootClass

/**
 * Checks a list of given classes and any class which is in the database but has changed will be removed. This removal
 * involves deleting the associated FILE, METHOD, TYPE_DECL and any V where (TYPE_DECL)-AST->(v).
 */
class MarkForRebuildPass(private val driver: IDriver) : IProgramStructurePass {

    private val logger: Logger = LogManager.getLogger(MarkForRebuildPass::javaClass)
    private val cache = DriverCache(driver)

    private enum class FileChange { UPDATE, NEW, NOP }

    /**
     * This pass will check and remove any classes that require an update from the database.
     *
     * @return a list of classes which need to be built.
     */
    override fun runPass(cs: List<SootClass>): List<SootClass> {
        val cStateList = cs.map(::checkIfClassNeedsAnUpdate).toList()
        cStateList.filter { it.second == FileChange.UPDATE }.forEach { dropClassFromGraph(it.first) }
        return cStateList.filterNot { it.second == FileChange.NOP }.map { it.first }.toList()
    }

    /**
     * Checks the given class against the given graph. If the file from the class' hash matches a file in the graph but
     * the hashes differ, this will mark for rebuild.
     *
     * @param c The class to check.
     * @return (c, UPDATE) if the class needs to be rebuilt, (c, NEW) if the class is unseen, and (c, NOP) if the given
     * class and the one in the database are the same.
     */
    private fun checkIfClassNeedsAnUpdate(c: SootClass): Pair<SootClass, FileChange> {
        val newCName = SootToPlumeUtil.sootClassToFileName(c)
        cache.tryGetFile(newCName)?.let { oldCNode: NewNodeBuilder ->
            val currentCHash = PlumeStorage.getFileHash(c)
            logger.info("Found an existing class with name ${c.name}...")
            return if (oldCNode.build().properties().get(HASH).get() != currentCHash) {
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

    private fun dropClassFromGraph(c: SootClass) {
        val newCName = SootToPlumeUtil.sootClassToFileName(c)
        driver.getVerticesByProperty(NAME, newCName, FILE).filterIsInstance<NewFileBuilder>().firstOrNull()
            ?.let { oldCNode: NewFileBuilder ->
                driver.getNeighbours(oldCNode).use { fNeighbours ->
                    val nodes = fNeighbours.nodes().asSequence().toList()
                    dropTypeDecl(nodes, c.type.toQuotedString())
                    dropFile(oldCNode)
                }
            }
    }

    private fun dropMethod(m: Method) {
        saveCallEdges(m)
        driver.deleteMethod(m.fullName())
    }

    private fun saveCallEdges(m: Method) {
        driver.getMethod(m.fullName(), false).use { g ->
            g.nodes { it == Method.Label() }.asSequence().firstOrNull()?.let { m1: Node ->
                val m2 = VertexMapper.mapToVertex(m1) as NewMethodBuilder
                val m2Build = m2.build()
                driver.getNeighbours(m2).use { ns ->
                    if (ns.V(m1.id()).hasNext()) {
                        ns.V(m1.id()).next().`in`(CALL).asSequence()
                            .filterIsInstance<Call>()
                            .forEach {
                                PlumeStorage.storeCallEdge(
                                    m2Build.fullName(),
                                    VertexMapper.mapToVertex(it) as NewCallBuilder
                                )
                            }
                    }
                }
            }
        }
    }

    private fun dropFile(c: NewFileBuilder) {
        val fileName = c.build().name()
        logger.debug("Deleting $FILE for $fileName")
        LocalCache.removeFile(fileName)
        driver.deleteVertex(c.id(), FILE)
    }

    private fun dropTypeDecl(ns: List<Node>, typeFullName: String) {
        ns.filterIsInstance<TypeDecl>()
            .filter { it.property(FULL_NAME) == typeFullName }
            .forEach { typeDecl ->
                logger.debug("Deleting $TYPE_DECL $typeFullName")
                LocalCache.removeTypeDecl(typeFullName)
                LocalCache.removeType(typeFullName)
                driver.getNeighbours(VertexMapper.mapToVertex(typeDecl)).use { n ->
                    n.nodes(typeDecl.id()).next()
                        .out(AST)
                        .forEach {
                            logger.debug("Deleting (${it.label()}: ${it.id()})")
                            if (it is Method) dropMethod(it)
                            else driver.deleteVertex(it.id(), it.label())
                        }
                    n.nodes(typeDecl.id()).next()
                        .`in`(REF).asSequence().distinct()
                        .forEach {
                            logger.debug("Deleting (${it.label()}: ${it.id()})")
                            driver.deleteVertex(it.id(), it.label())
                        }
                }
                driver.deleteVertex(typeDecl.id(), TYPE_DECL)
            }
    }
}
