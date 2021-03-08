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
package io.github.plume.oss.passes.graph

import io.github.plume.oss.Extractor.Companion.getSootAssociation
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IUnitGraphPass
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CALL
import io.shiftleft.codepropertygraph.generated.NodeTypes.METHOD
import io.shiftleft.codepropertygraph.generated.nodes.NewCallBuilder
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import org.apache.logging.log4j.LogManager
import soot.Scene
import soot.Unit
import soot.jimple.AssignStmt
import soot.jimple.IdentityStmt
import soot.jimple.InvokeStmt
import soot.jimple.toolkits.callgraph.Edge
import soot.toolkits.graph.BriefUnitGraph

/**
 * The [IUnitGraphPass] that constructs the interprocedural call edges.
 *
 * @param driver The driver to build the call edges with.
 */
class CGPass(private val driver: IDriver) : IUnitGraphPass {
    private val logger = LogManager.getLogger(CGPass::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun runPass(graph: BriefUnitGraph): BriefUnitGraph {
        val mtd = graph.body.method
        logger.debug("Building call graph edges for ${mtd.declaration}")
        this.graph = graph
        // Connect all units to their successors
        this.graph.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
        return graph
    }

    private fun projectUnit(unit: Unit) {
        val cg = Scene.v().callGraph
        val edges = cg.edgesOutOf(unit) as Iterator<Edge>
        // Attempt to use Soot calculated call graph edges, these are usually quite precise
        edges.forEach { e: Edge ->
            // If Soot points to the assignment as the call source then this is most likely from the rightOp. Let's
            // hope this is not the source of a bug
            val srcUnit = if (unit is AssignStmt) unit.rightOp else unit
            getSootAssociation(srcUnit)
                ?.filterIsInstance<NewCallBuilder>()
                ?.firstOrNull()
                ?.let { srcPlumeVertex ->
                    getSootAssociation(e.tgt.method())
                        ?.firstOrNull()?.let { tgtPlumeVertex ->
                            runCatching {
                                driver.addEdge(srcPlumeVertex, tgtPlumeVertex, CALL)
                            }.onFailure { e -> logger.warn(e.message) }
                        }
                }
        }
        // If call graph analysis fails because there is no main method, we will need to figure out call edges ourselves
        // We can do this by looking if our call unit does not have any outgoing CALL edges.
        when (unit) {
            is AssignStmt -> getSootAssociation(unit.rightOp)?.filterIsInstance<NewCallBuilder>()?.firstOrNull()
            is InvokeStmt -> getSootAssociation(unit.invokeExpr)?.filterIsInstance<NewCallBuilder>()?.firstOrNull()
            else -> null
        }?.let { callV ->
            driver.getNeighbours(callV).use { g ->
                // If there is no outgoing call edge from this call, then we should attempt to find it's target method
                if (g.node(callV.id())?.outE(CALL)?.hasNext() != true) {
                    val v = callV.build()
                    if (!g.nodes(METHOD).hasNext() && v.methodFullName().length > 1) {
                        driver.getMethod(v.methodFullName()).use { mg ->
                            if (mg.nodes(METHOD).hasNext()) {
                                val mtdV = mg.nodes(METHOD).next()
                                // Since this method already exists, we don't need to build a new method, only provide
                                // an existing ID
                                driver.addEdge(callV, NewMethodBuilder().id(mtdV.id()), CALL)
                            }
                        }
                    }
                }
            }
        }
    }

}