package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.concurrent.PlumeConcurrentCpgPass.producerQueueCapacity
import com.github.plume.oss.passes.concurrent.PlumeConcurrentWriter.concurrentCreateApply
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.DiffGraph
import io.shiftleft.semanticcpg.passes.controlflow.CfgCreationPass

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object PlumeCfgCreationPass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

class PlumeCfgCreationPass(cpg: Cpg) extends CfgCreationPass(cpg) with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeCfgCreationPass.producerQueueCapacity
    concurrentCreateApply[Method](
      producerQueueCapacity,
      driver,
      name,
      baseLogger,
      _ => init(),
      _ => generateParts(),
      cpg,
      (x: DiffGraph.Builder, y: Method) => runOnPart(x, y),
      _ => finish()
    )
  }

}
