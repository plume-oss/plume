package io.github.plume.oss.util
import io.github.plume.oss.drivers.IDriver
import io.shiftleft.passes.DiffGraph
import io.shiftleft.passes.DiffGraph.Change
import org.apache.logging.log4j.LogManager

object JoernToPlumeUtil {

  private val logger = LogManager.getLogger(JoernToPlumeUtil.getClass)

  def accept(driver: IDriver, df: DiffGraph) {
    df.iterator.foreach {
      case Change.CreateNode(node) => {
        // TODO
        node.properties
      }
      case e: Change.CreateEdge  => {}
      case Change.RemoveNode(id) => {}
      case Change.RemoveEdge(e)  => {}
      case _                     => logger.warn("Unsupported DiffGraph operation $c.")
    }
  }

}
