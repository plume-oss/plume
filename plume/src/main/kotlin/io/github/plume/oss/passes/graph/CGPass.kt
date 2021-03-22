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

import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IUnitGraphPass
import io.github.plume.oss.store.PlumeStorage
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CALL
import io.shiftleft.codepropertygraph.generated.NodeKeyNames.FULL_NAME
import io.shiftleft.codepropertygraph.generated.NodeTypes.METHOD
import io.shiftleft.codepropertygraph.generated.nodes.NewMethodBuilder
import org.apache.logging.log4j.LogManager
import soot.Scene
import soot.Unit
import soot.jimple.AssignStmt
import soot.jimple.IdentityStmt
import soot.jimple.InvokeExpr
import soot.jimple.InvokeStmt
import soot.jimple.toolkits.callgraph.Edge
import soot.toolkits.graph.BriefUnitGraph

/**
 * The [IUnitGraphPass] that constructs the interprocedural call edges.
 *
 * @param g The driver to build the call edges with.
 * @param driver The driver to build the call edges with.
 */
class CGPass(private val g: BriefUnitGraph, private val driver: IDriver) : IUnitGraphPass {

    private val logger = LogManager.getLogger(CGPass::javaClass)
    private val builder = DeltaGraph.Builder()

    override fun runPass(): DeltaGraph {
        val mtd = g.body.method
        logger.debug("Building call graph edges for ${mtd.declaringClass.name}:${mtd.name}")
        // If this was an updated method, connect call graphs
        PlumeStorage.getMethodStore(mtd).filterIsInstance<NewMethodBuilder>()
            .firstOrNull()?.let { reconnectPriorCallGraphEdges(it) }
        // Connect all calls to their methods
        this.g.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
        return builder.build()
    }

    private fun projectUnit(unit: Unit) {
        val cg = Scene.v().callGraph
        val edges = cg.edgesOutOf(unit) as Iterator<Edge>
        // If Soot points to the assignment as the call source then this is most likely from the rightOp.
        val srcUnit = if (unit is AssignStmt) unit.rightOp else unit
        when (srcUnit) {
            is InvokeExpr -> PlumeStorage.getCall(srcUnit)
            is InvokeStmt -> PlumeStorage.getCall(srcUnit.invokeExpr)
            else -> null
        }?.let { callV ->
            var foundAndConnectedCallTgt = false
            edges.forEach { e: Edge ->
                PlumeStorage.getMethodStore(e.tgt.method())
                    .filterIsInstance<NewMethodBuilder>()
                    .firstOrNull()?.let { tgtPlumeVertex ->
                        builder.addEdge(callV, tgtPlumeVertex, CALL)
                        foundAndConnectedCallTgt = true
                    }
            }
            // If call graph analysis fails because there is no main method, we will need to figure out call edges ourselves
            // We can do this by looking if our call unit does not have any outgoing CALL edges.
            // If there is no outgoing call edge from this call, then we should attempt to find it's target method
            if (!foundAndConnectedCallTgt) {
                val v = callV.build()
                if (v.methodFullName().length > 1) {
                    getMethodHead(v.methodFullName())?.let { mtdV -> builder.addEdge(callV, mtdV, CALL) }
                }
            }
        }
    }

    private fun getMethodHead(fullName: String): NewMethodBuilder? =
        PlumeStorage.getMethod(fullName)
            ?: (driver.getVerticesByProperty(FULL_NAME, fullName, METHOD).firstOrNull() as NewMethodBuilder?)
                ?.apply { PlumeStorage.addMethod(this) }

    private fun reconnectPriorCallGraphEdges(mtdV: NewMethodBuilder) {
        val mtd = mtdV.build()
        PlumeStorage.getCallsIn(mtd.fullName()).let { incomingVs ->
            if (incomingVs.isNotEmpty()) {
                logger.debug("Saved call graph edges found - reconnecting incoming call graph edges")
                incomingVs.forEach { inV -> builder.addEdge(inV, mtdV, CALL) }
            } else {
                logger.debug("No previous call graph edges were found")
            }
        }
    }
}