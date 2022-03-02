package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.concurrent.PlumeConcurrentCpgPass.concurrentCreateApply
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.semanticcpg.passes.controlflow.CfgCreationPass

object PlumeCfgCreationPass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

class PlumeCfgCreationPass(cpg: Cpg) extends CfgCreationPass(cpg) with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeCfgCreationPass.producerQueueCapacity
    try {
      init()
      concurrentCreateApply[Method](
        producerQueueCapacity,
        driver,
        name,
        baseLogger,
        generateParts(),
        cpg,
        (x: DiffGraphBuilder, y: Method) => runOnPart(x, y),
        None
      )
    } finally {
      finish()
    }
  }

}
