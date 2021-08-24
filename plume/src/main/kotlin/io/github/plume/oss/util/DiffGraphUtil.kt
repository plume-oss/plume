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
package io.github.plume.oss.util

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.domain.model.DeltaGraph
import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import io.shiftleft.passes.DiffGraph
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Node

/**
 * Utility class to process [DiffGraph] changes.
 */
object DiffGraphUtil {

    private val logger: Logger = LogManager.getLogger(DiffGraphUtil::class.java)

    /**
     * Processes [DiffGraph] operations and applies them to the given [IDriver].
     *
     * @param driver The driver to which the changes are to be applied.
     * @param df The [DiffGraph] from which to extract the changes from.
     */
    fun processDiffGraph(driver: IDriver, df: DiffGraph) {
        df.iterator().foreach { change: DiffGraph.Change ->
            when (change) {
                is io.shiftleft.passes.`DiffGraph$Change$CreateNode` -> {
                    val n = convertNode(change.node())
                    if (n != null) driver.addVertex(n)
                    else logger.warn("Could not convert ${change.node()} from $change.")
                }
                is io.shiftleft.passes.`DiffGraph$Change$RemoveNode` ->
                    driver.deleteVertex(change.nodeId())
                is io.shiftleft.passes.`DiffGraph$Change$RemoveEdge` -> {
                    val edge = change.edge()
                    driver.deleteEdge(
                        src = VertexMapper.mapToVertex(edge.outNode() as Node),
                        tgt = VertexMapper.mapToVertex(edge.inNode() as Node),
                        edge = edge.label()
                    )
                }
                is io.shiftleft.passes.`DiffGraph$Change$CreateEdge` -> {
                    val src: NewNodeBuilder<out NewNode>? = convertNode(change.src())
                    val dst: NewNodeBuilder<out NewNode>? = convertNode(change.dst())
                    if (src == null) logger.warn("Could not convert ${change.src()} from $change.")
                    if (dst == null) logger.warn("Could not convert ${change.dst()} from $change.")
                    if (src != null && dst != null) driver.addEdge(src, dst, change.label())
                }
                is io.shiftleft.passes.`DiffGraph$Change$SetNodeProperty` -> {
                    val node = change.node()
                    driver.updateVertexProperty(node.id(), node.label(), change.key(), change.value())
                }
                else -> logger.warn("Unsupported DiffGraph operation ${change.javaClass} encountered.")
            }
        }
    }

    fun toDeltaGraph(df: DiffGraph): DeltaGraph {
        val builder = DeltaGraph.Builder()
        df.iterator().foreach { change: DiffGraph.Change ->
            when (change) {
                is io.shiftleft.passes.`DiffGraph$Change$CreateNode` -> {
                    val n = convertNode(change.node())
                    if (n != null) builder.addVertex(n)
                    else logger.warn("Could not convert ${change.node()} from $change.")
                }
                is io.shiftleft.passes.`DiffGraph$Change$RemoveEdge` -> {
                    val edge = change.edge()
                    builder.deleteEdge(
                            src = VertexMapper.mapToVertex(edge.outNode() as Node),
                            tgt = VertexMapper.mapToVertex(edge.inNode() as Node),
                            e = edge.label()
                    )
                }
                is io.shiftleft.passes.`DiffGraph$Change$CreateEdge` -> {
                    val src: NewNodeBuilder<out NewNode>? = convertNode(change.src())
                    val dst: NewNodeBuilder<out NewNode>? = convertNode(change.dst())
                    if (src == null) logger.warn("Could not convert ${change.src()} from $change.")
                    if (dst == null) logger.warn("Could not convert ${change.dst()} from $change.")
                    if (src != null && dst != null) builder.addEdge(src, dst, change.label())
                    else null
                }
                else -> Unit
            }
        }
        return builder.build()
    }

    private fun convertNode(n: Any): NewNodeBuilder<out NewNode>? =
        when (n) {
        is NewNode -> VertexMapper.mapToVertex(n)
        is Node -> VertexMapper.mapToVertex(n)
        else -> null
    }

}
