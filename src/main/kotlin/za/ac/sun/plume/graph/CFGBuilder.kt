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
package za.ac.sun.plume.graph

import org.apache.logging.log4j.LogManager
import soot.Unit
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph
import za.ac.sun.plume.Extractor.Companion.getSootAssociation
import za.ac.sun.plume.domain.enums.EdgeLabel
import za.ac.sun.plume.domain.models.PlumeVertex
import za.ac.sun.plume.domain.models.vertices.*
import za.ac.sun.plume.drivers.IDriver
import za.ac.sun.plume.util.ExtractorConst.FALSE_TARGET
import za.ac.sun.plume.util.ExtractorConst.TRUE_TARGET

/**
 * The [IGraphBuilder] that constructs the dependence edges in the graph.
 *
 * @param driver The driver to build the CFG with.
 */
class CFGBuilder(private val driver: IDriver) : IGraphBuilder {
    private val logger = LogManager.getLogger(CFGBuilder::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun buildMethodBody(graph: BriefUnitGraph): MethodVertex {
        val mtd = graph.body.method
        logger.debug("Building CFG for ${mtd.declaration}")
        this.graph = graph
        // Connect entrypoint to the first CFG vertex
        this.graph.heads.forEach { head ->
            // Select appropriate successor to start CFG chain at
            var startingUnit = head
            while (startingUnit is IdentityStmt) startingUnit = graph.getSuccsOf(startingUnit).firstOrNull() ?: break
            startingUnit?.let {
                getSootAssociation(it)?.firstOrNull()?.let { succVert ->
                    val mtdV = getSootAssociation(mtd)
                    val bodyVertex = mtdV?.first { mtdVertices -> mtdVertices is BlockVertex }!!
                    mtdV.firstOrNull()?.let { mtdVertex -> driver.addEdge(mtdVertex, bodyVertex, EdgeLabel.CFG) }
                    driver.addEdge(
                            fromV = bodyVertex,
                            toV = succVert,
                            edge = EdgeLabel.CFG
                    )
                }
            }
        }
        // Connect all units to their successors
        this.graph.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
        return getSootAssociation(mtd)?.first { it is MethodVertex } as MethodVertex
    }

    private fun projectUnit(unit: Unit) {
        when (unit) {
            is GotoStmt -> projectUnit(unit.target)
            is IfStmt -> projectIfStatement(unit)
            is LookupSwitchStmt -> projectLookupSwitch(unit)
            is TableSwitchStmt -> projectTableSwitch(unit)
            is ReturnStmt -> projectReturnEdge(unit)
            is ReturnVoidStmt -> projectReturnEdge(unit)
            is ThisRef -> Unit
            is IdentityRef -> Unit
            else -> {
                val sourceUnit = if (unit is GotoStmt) unit.target else unit
                val sourceVertex = getSootAssociation(sourceUnit)?.firstOrNull()
                graph.getSuccsOf(sourceUnit).forEach {
                    val targetUnit = if (it is GotoStmt) it.target else it
                    if (sourceVertex != null) {
                        getSootAssociation(targetUnit)?.let { vList -> driver.addEdge(sourceVertex, vList.first(), EdgeLabel.CFG) }
                    }
                }
            }
        }
    }

    private fun projectTableSwitch(unit: TableSwitchStmt) {
        val switchVertices = getSootAssociation(unit)!!
        val switchVertex = switchVertices.first { it is ControlStructureVertex } as ControlStructureVertex
        // Handle default target jump
        projectSwitchDefault(unit, switchVertices, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) projectSwitchTarget(switchVertices, i, switchVertex, tgt)
        }
    }

    private fun projectLookupSwitch(unit: LookupSwitchStmt) {
        val lookupVertices = getSootAssociation(unit)!!
        val lookupVertex = lookupVertices.first { it is ControlStructureVertex } as ControlStructureVertex
        // Handle default target jump
        projectSwitchDefault(unit, lookupVertices, lookupVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            val lookupValue = unit.getLookupValue(i)
            if (unit.defaultTarget != tgt) projectSwitchTarget(lookupVertices, lookupValue, lookupVertex, tgt)
        }
    }

    private fun projectSwitchTarget(lookupVertices: List<PlumeVertex>, lookupValue: Int, lookupVertex: ControlStructureVertex, tgt: Unit) {
        val tgtV = lookupVertices.first { it is JumpTargetVertex && it.argumentIndex == lookupValue }
        driver.addEdge(lookupVertex, tgtV, EdgeLabel.CFG)
        getSootAssociation(tgt)?.let { vList ->
            driver.addEdge(tgtV, vList.first(), EdgeLabel.CFG)
        }
    }

    private fun projectSwitchDefault(unit: SwitchStmt, switchVertices: List<PlumeVertex>, switchVertex: ControlStructureVertex) {
        unit.defaultTarget.let { defaultUnit ->
            val tgtV = switchVertices.first { it is JumpTargetVertex && it.name == "DEFAULT" }
            driver.addEdge(switchVertex, tgtV, EdgeLabel.CFG)
            getSootAssociation(defaultUnit)?.let { vList ->
                driver.addEdge(tgtV, vList.first(), EdgeLabel.CFG)
            }
        }
    }

    private fun projectIfStatement(unit: IfStmt) {
        val ifVertices = getSootAssociation(unit)!!
        graph.getSuccsOf(unit).forEach {
            val srcVertex = if (it == unit.target) {
                ifVertices.first { vert -> vert is JumpTargetVertex && vert.name == FALSE_TARGET }
            } else {
                ifVertices.first { vert -> vert is JumpTargetVertex && vert.name == TRUE_TARGET }
            }
            val tgtVertices = if (it is GotoStmt) getSootAssociation(it.target)
            else getSootAssociation(it)
            tgtVertices?.let { vList ->
                driver.addEdge(ifVertices.first(), srcVertex, EdgeLabel.CFG)
                driver.addEdge(srcVertex, vList.first(), EdgeLabel.CFG)
            }
        }
    }

    private fun projectReturnEdge(unit: ReturnStmt) {
        getSootAssociation(unit)?.firstOrNull()?.let { src ->
            getSootAssociation(graph.body.method)?.filterIsInstance<MethodReturnVertex>()?.firstOrNull()?.let { tgt ->
                driver.addEdge(src, tgt, EdgeLabel.CFG)
            }
        }
    }

    private fun projectReturnEdge(unit: ReturnVoidStmt) {
        getSootAssociation(unit)?.firstOrNull()?.let { src ->
            getSootAssociation(graph.body.method)?.filterIsInstance<MethodReturnVertex>()?.firstOrNull()?.let { tgt ->
                driver.addEdge(src, tgt, EdgeLabel.CFG)
            }
        }
    }
}