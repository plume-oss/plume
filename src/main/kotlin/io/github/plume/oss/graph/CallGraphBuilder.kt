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
package io.github.plume.oss.graph

import io.github.plume.oss.Extractor
import io.github.plume.oss.Extractor.Companion.getSootAssociation
import io.github.plume.oss.domain.enums.EdgeLabel
import io.github.plume.oss.domain.models.vertices.MethodVertex
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.util.SootToPlumeUtil.constructPhantom
import org.apache.logging.log4j.LogManager
import soot.Scene
import soot.Unit
import soot.jimple.IdentityStmt
import soot.jimple.toolkits.callgraph.Edge
import soot.toolkits.graph.BriefUnitGraph

/**
 * The [IGraphBuilder] that constructs the interprocedural call edges.
 *
 * @param driver The driver to build the call edges with.
 */
class CallGraphBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(CallGraphBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun buildMethodBody(graph: BriefUnitGraph) {
        val mtd = graph.body.method
        logger.debug("Building call graph edges for ${mtd.declaration}")
        // If this was an updated method, connect call graphs
        getSootAssociation(mtd)?.filterIsInstance<MethodVertex>()?.first()?.let { reconnectPriorCallGraphEdges(it) }
        this.graph = graph
        // Connect all units to their successors
        this.graph.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
    }

    private fun projectUnit(unit: Unit) {
        val cg = Scene.v().callGraph
        val edges = cg.edgesOutOf(unit) as Iterator<Edge>
        edges.forEach { e: Edge ->
            getSootAssociation(unit)?.firstOrNull()?.let { srcPlumeVertex ->
                val tgtPlumeVertex = getSootAssociation(e.tgt.method())?.firstOrNull()
                    ?: constructPhantom(e.tgt.method(), driver)
                runCatching {
                    driver.addEdge(srcPlumeVertex, tgtPlumeVertex, EdgeLabel.CALL)
                }.onFailure { e -> logger.warn(e.message) }
            }
        }
    }

    private fun reconnectPriorCallGraphEdges(mtdV: MethodVertex) {
        Extractor.getIncomingCallGraphEdges(mtdV)?.let { incomingVs ->
            if (incomingVs.isNotEmpty()) {
                logger.debug("Saved call graph edges found - reconnecting incoming call graph edges")
                incomingVs.forEach { inV ->
                    runCatching {
                        driver.addEdge(inV, mtdV, EdgeLabel.CALL)
                    }.onFailure { e -> logger.warn(e.message) }
                }
            } else {
                logger.debug("No previous call graph edges were found")
            }
        }
    }

}