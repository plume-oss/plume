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
package io.github.plume.oss.passes.method

import io.github.plume.oss.Extractor.Companion.getSootAssociation
import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.IMethodPass
import io.github.plume.oss.util.ExtractorConst.FALSE_TARGET
import io.github.plume.oss.util.ExtractorConst.TRUE_TARGET
import io.shiftleft.codepropertygraph.generated.EdgeTypes.CFG
import io.shiftleft.codepropertygraph.generated.nodes.*
import org.apache.logging.log4j.LogManager
import soot.Unit
import soot.jimple.*
import soot.toolkits.graph.BriefUnitGraph

/**
 * The [IMethodPass] that constructs the dependence edges in the graph.
 *
 * @param driver The driver to build the CFG with.
 */
class CFGPass(private val driver: IDriver) : IMethodPass {
    private val logger = LogManager.getLogger(CFGPass::javaClass)
    private lateinit var graph: BriefUnitGraph

    override fun runPass(graph: BriefUnitGraph): BriefUnitGraph {
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
                    val bodyVertex = mtdV?.first { mtdVertices -> mtdVertices is NewBlockBuilder }!!
                    mtdV.firstOrNull()?.let { mtdVertex -> driver.addEdge(mtdVertex, bodyVertex, CFG) }
                    runCatching {
                        driver.addEdge(bodyVertex, succVert, CFG)
                    }.onFailure { e -> logger.warn(e.message) }
                }
            }
        }
        // Connect all units to their successors
        this.graph.body.units.filterNot { it is IdentityStmt }.forEach(this::projectUnit)
        return graph
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
                        getSootAssociation(targetUnit)?.let { vList ->
                            runCatching {
                                driver.addEdge(sourceVertex, vList.first(), CFG)
                            }.onFailure { e -> logger.warn(e.message) }
                        }
                    }
                }
            }
        }
    }

    private fun projectTableSwitch(unit: TableSwitchStmt) {
        val switchVertices = getSootAssociation(unit)!!
        val switchVertex = switchVertices.first { it is NewControlStructureBuilder } as NewControlStructureBuilder
        // Handle default target jump
        projectSwitchDefault(unit, switchVertices, switchVertex)
        // Handle case jumps
        unit.targets.forEachIndexed { i, tgt ->
            if (unit.defaultTarget != tgt) projectSwitchTarget(switchVertices, i, switchVertex, tgt)
        }
    }

    private fun projectLookupSwitch(unit: LookupSwitchStmt) {
        val lookupVertices = getSootAssociation(unit)!!
        val lookupVertex = lookupVertices.first { it is NewControlStructureBuilder } as NewControlStructureBuilder
        // Handle default target jump
        projectSwitchDefault(unit, lookupVertices, lookupVertex)
        // Handle case jumps
        for (i in 0 until unit.targetCount) {
            val tgt = unit.getTarget(i)
            val lookupValue = unit.getLookupValue(i)
            if (unit.defaultTarget != tgt) projectSwitchTarget(lookupVertices, lookupValue, lookupVertex, tgt)
        }
    }

    private fun projectSwitchTarget(
        lookupVertices: List<NewNodeBuilder>,
        lookupValue: Int,
        lookupVertex: NewControlStructureBuilder,
        tgt: Unit
    ) {
        val tgtV = lookupVertices.first { it is NewJumpTargetBuilder && it.build().argumentIndex() == lookupValue }
        projectTargetPath(lookupVertex, tgtV, tgt)
    }

    private fun projectSwitchDefault(
        unit: SwitchStmt,
        switchVertices: List<NewNodeBuilder>,
        switchVertex: NewControlStructureBuilder
    ) {
        unit.defaultTarget.let { defaultUnit ->
            val tgtV = switchVertices.first { it is NewJumpTargetBuilder && it.build().name() == "DEFAULT" }
            projectTargetPath(switchVertex, tgtV, defaultUnit)
        }
    }

    private fun projectTargetPath(
        lookupVertex: NewControlStructureBuilder,
        tgtV: NewNodeBuilder,
        tgt: Unit
    ) {
        runCatching {
            driver.addEdge(lookupVertex, tgtV, CFG)
        }.onFailure { e -> logger.warn(e.message) }
        getSootAssociation(tgt)?.let { vList ->
            runCatching {
                driver.addEdge(tgtV, vList.first(), CFG)
            }.onFailure { e -> logger.warn(e.message) }
        }
    }

    private fun projectIfStatement(unit: IfStmt) {
        val ifVertices = getSootAssociation(unit)!!
        graph.getSuccsOf(unit).forEach {
            val srcVertex = if (it == unit.target) {
                ifVertices.first { vert -> vert is NewJumpTargetBuilder && vert.build().name() == FALSE_TARGET }
            } else {
                ifVertices.first { vert -> vert is NewJumpTargetBuilder && vert.build().name() == TRUE_TARGET }
            }
            val tgtVertices = if (it is GotoStmt) getSootAssociation(it.target)
            else getSootAssociation(it)
            tgtVertices?.let { vList ->
                runCatching {
                    driver.addEdge(ifVertices.first(), srcVertex, CFG)
                }.onFailure { e -> logger.warn(e.message) }
                runCatching {
                    driver.addEdge(srcVertex, vList.first(), CFG)
                }.onFailure { e -> logger.warn(e.message) }
            }
        }
    }

    private fun projectReturnEdge(unit: Stmt) {
        getSootAssociation(unit)?.firstOrNull()?.let { src ->
            getSootAssociation(graph.body.method)?.filterIsInstance<NewMethodReturnBuilder>()?.firstOrNull()
                ?.let { tgt ->
                    runCatching {
                        driver.addEdge(src, tgt, CFG)
                    }.onFailure { e -> logger.warn(e.message) }
                }
        }
    }
}