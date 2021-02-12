package io.github.plume.oss.util
import io.github.plume.oss.domain.mappers.VertexMapper
import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.generated.nodes.{CpgNode, NewNode, NewNodeBuilder}
import io.shiftleft.passes.DiffGraph
import io.shiftleft.passes.DiffGraph.Change
import io.shiftleft.proto.cpg.Cpg.CpgStruct
import io.shiftleft.proto.cpg.Cpg.CpgStruct.Node
import org.apache.logging.log4j.LogManager

import scala.jdk.CollectionConverters

object JoernToPlumeUtil {

  private val logger = LogManager.getLogger(JoernToPlumeUtil.getClass)

  def scalaMapToVertex(properties: Map[String, Any]): NewNodeBuilder = {
    val m = CollectionConverters.MapHasAsJava(properties.view.mapValues(f => f.asInstanceOf[AnyRef]).toMap).asJava
    VertexMapper.INSTANCE.mapToVertex(m)
  }

  def nodeToMap(n: NewNode): Map[String, Any] = n.properties + Map("label" -> n.label)

  def decomposeEdge(e: Change.CreateEdge): (NewNodeBuilder, NewNodeBuilder, String) = {
    val src =
      if (e.sourceNodeKind == Change.NodeKind.New)
        scalaMapToVertex(nodeToMap(e.src.asInstanceOf[NewNode]))
      else
        cpgNodeToNewNodeBuilder(e.src)
    val dst =
      if (e.destinationNodeKind == Change.NodeKind.New)
        scalaMapToVertex(nodeToMap(e.dst.asInstanceOf[NewNode]))
      else
        cpgNodeToNewNodeBuilder(e.dst)
    (src, dst, e.label)
  }

  def decomposeEdge(e: Change.RemoveEdge): (NewNodeBuilder, NewNodeBuilder, String) = {
    val srcMap = e.edge.outNode().propertyMap(); srcMap.put("label", e.edge.outNode().label())
    val src = VertexMapper.INSTANCE.mapToVertex(srcMap)
    val dstMap = e.edge.inNode().propertyMap(); dstMap.put("label", e.edge.inNode().label())
    val dst = VertexMapper.INSTANCE.mapToVertex(dstMap)
    (src, dst, e.edge.label())
  }

  def cpgNodeToNewNodeBuilder(cpgNode: CpgNode): NewNodeBuilder = {
    val n = cpgNode.asInstanceOf[Node]
    val m = n.getPropertyList.toArray
      .map(x => {
        val p = x.asInstanceOf[CpgStruct.Node.Property]
        p.getName.name() -> n.getProperty(p.getName.getNumber)
      })
      .toMap
    scalaMapToVertex(m + Map("label" -> cpgNode.label))
  }

  def accept(driver: IDriver, df: DiffGraph) {
    df.iterator.foreach {
      case Change.CreateNode(node) => driver.addVertex(scalaMapToVertex(nodeToMap(node)))
      case Change.RemoveNode(id)   => driver.deleteVertex(id, null)
      case e: Change.CreateEdge =>
        val (src, dst, edge) = decomposeEdge(e)
        driver.addEdge(src, dst, edge)
      case e: Change.RemoveEdge =>
        val (src, dst, edge) = decomposeEdge(e)
        driver.deleteEdge(src, dst, edge)
      case c => logger.warn(s"Unsupported DiffGraph operation $c.")
    }
  }

}
