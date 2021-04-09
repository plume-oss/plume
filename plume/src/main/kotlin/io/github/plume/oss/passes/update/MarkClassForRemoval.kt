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

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.store.DriverCache
import io.github.plume.oss.util.ExtractorConst.GLOBAL
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.*
import io.shiftleft.codepropertygraph.generated.nodes.Member
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.codepropertygraph.generated.nodes.NewTypeDeclBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import soot.SootClass

/**
 * A pass to remove any no longer present application classes and it's associated types.
 */
class MarkClassForRemoval(private val driver: IDriver) {

    private val logger: Logger = LogManager.getLogger(MarkClassForRemoval::javaClass)
    private val cache = DriverCache(driver)

    /**
     * Takes an incoming set of classes and removes those no longer in the database.
     *
     * @param cs Incoming application classes.
     */
    fun runPass(cs: Set<SootClass>) {
        val tds = driver.getPropertyFromVertices<String>(FULL_NAME, TYPE_DECL)
        val incomingTypes = cs.map { it.type.toQuotedString() }.toSet()
        tds.filterNot(incomingTypes::contains) // Get type names from database and get those not from loaded classes
            .mapNotNull(cache::tryGetTypeDecl) // Get the types from the database no longer in the given artifact
            .filter {
                !it.build().isExternal && it.build().astParentFullName() != GLOBAL
            } // Only remove application classes
            .forEach(::deleteTypeDeclAndNeighbours) // Delete these types
    }

    private fun deleteTypeDeclAndNeighbours(td: NewTypeDeclBuilder) {
        logger.trace("Removing ${td.build().fullName()}")
        driver.getNeighbours(td).use { g ->
            // Delete methods from TYPE_DECL
            g.nodes(METHOD).asSequence()
                .filterIsInstance<Method>()
                .map { it.fullName() }
                .forEach(driver::deleteMethod)
            // Delete members
            g.nodes(MEMBER).asSequence()
                .filterIsInstance<Member>()
                .forEach(::deleteField)
            // Delete FILE, NAMESPACE_BLOCK, TYPE, TYPE_DECL, MODIFIER
            g.nodes().asSequence()
                .filterNot { it.label() == METHOD || it.label() == MEMBER }
                .forEach { driver.deleteVertex(it.id(), it.label()) }
        }
    }

    private fun deleteField(m: Member) {
        driver.getNeighbours(VertexMapper.mapToVertex(m)).use { g ->
            g.nodes(MODIFIER).asSequence().forEach { driver.deleteVertex(it.id(), it.label()) }
        }
        driver.deleteVertex(m.id(), m.label())
    }
}