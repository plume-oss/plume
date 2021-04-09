/*
 * Copyright 2021 Plume Authors
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
package io.github.plume.oss.passes.update

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.FileChange
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.store.PlumeStorage
import io.github.plume.oss.util.SootParserUtil
import io.github.plume.oss.util.SootToPlumeUtil
import io.shiftleft.codepropertygraph.generated.EdgeTypes.AST
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.HASH
import io.shiftleft.codepropertygraph.generated.NodeTypes.MODIFIER
import io.shiftleft.codepropertygraph.generated.nodes.NewFileBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewModifierBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootClass

/**
 * Checks a list of given classes and any class which is in the database but has changed will be removed. This removal
 * involves deleting the associated FILE, METHOD, TYPE_DECL and any V where (TYPE_DECL)-AST->(v).
 */
class MarkClassForRebuild(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MarkClassForRebuild::javaClass)
    private val cache = DriverCache(driver)

    /**
     * This pass will check and filter classes which need updates and needed to be built. Furthermore, classes which
     * need updates will have their modifiers rebuilt.
     *
     * @return a list of classes which need to be built or updated.
     */
    fun runPass(cs: Set<SootClass>): List<Pair<SootClass, FileChange>> {
        val cStateList = cs.map(::checkIfClassNeedsAnUpdate).toList()
        val csToUpdate = cStateList.filter { it.second == FileChange.UPDATE }.toList()
        csToUpdate.map { it.first }.forEach(::updateModifiers)
        if (csToUpdate.isNotEmpty()) logger.info("Number of classes to update is ${csToUpdate.size}")
        return cStateList.filterNot { it.second == FileChange.NOP }.toList()
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
        cache.tryGetFile(newCName)?.let { oldCNode: NewFileBuilder ->
            val currentCHash = PlumeStorage.getFileHash(c)
            logger.debug("Found an existing class with name ${c.name}")
            val maybeOldHash = oldCNode.build().properties().get(HASH)
            return when {
                maybeOldHash.isDefined -> {
                    if (maybeOldHash.get() != currentCHash) {
                        logger.debug("Class hashes differ, marking ${c.name} for rebuild.")
                        Pair(c, FileChange.UPDATE)
                    } else {
                        logger.trace("Classes are identical - no update necessary.")
                        Pair(c, FileChange.NOP)
                    }
                }
                else -> {
                    logger.trace("Class is external, not need to rebuild.")
                    Pair(c, FileChange.NOP)
                }
            }
        }
        logger.trace("No existing class for ${c.name} found.")
        return Pair(c, FileChange.NEW)
    }

    private fun updateModifiers(c: SootClass) {
        cache.tryGetTypeDecl(c.type.toQuotedString())?.let { typeDecl ->
            // Delete old ones
            driver.getNeighbours(typeDecl).use { g ->
                g.nodes(MODIFIER).forEach { driver.deleteVertex(it.id(), it.label()) }
            }
            // Create new ones
            linkModifiers(c, typeDecl)
        }
    }

    /*
     * TYPE_DECL -(AST)-> MODIFIER
     */
    private fun linkModifiers(c: SootClass, t: NewTypeDeclBuilder) {
        SootParserUtil.determineModifiers(c.modifiers)
            .mapIndexed { i, m -> NewModifierBuilder().modifierType(m).order(i + 1) }
            .forEach { m -> driver.addEdge(t, m, AST) }
    }

}
