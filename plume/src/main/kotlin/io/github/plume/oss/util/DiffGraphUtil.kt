package io.github.plume.oss.util

import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.NewNode
import io.shiftleft.codepropertygraph.generated.nodes.NewNodeBuilder
import io.shiftleft.passes.DiffGraph
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import overflowdb.Node
import kotlin.math.log

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
                    val src: NewNodeBuilder? = convertNode(change.src())
                    val dst: NewNodeBuilder? = convertNode(change.dst())
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

    fun convertNode(n: Any): NewNodeBuilder? =
        when (n) {
        is NewNode -> VertexMapper.mapToVertex(n)
        is Node -> VertexMapper.mapToVertex(n)
        else -> null
    }

}
