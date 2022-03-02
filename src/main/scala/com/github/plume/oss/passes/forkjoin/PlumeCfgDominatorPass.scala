package com.github.plume.oss.passes.forkjoin

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.forkjoin.PlumeForkJoinParallelCpgPass.forkJoinSerializeAndStore
import com.github.plume.oss.passes.parallel.PlumeParallelCpgPass.{
  parallelEnqueue,
  parallelWithWriter
}
import com.github.plume.oss.passes.parallel.PlumeParallelWriter
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.KeyPool
import io.shiftleft.semanticcpg.passes.controlflow.cfgdominator.CfgDominatorPass

class PlumeCfgDominatorPass(cpg: Cpg, keyPool: Option[KeyPool] = None)
    extends CfgDominatorPass(cpg)
    with PlumeCpgPassBase {

  def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    try {
      init()
      forkJoinSerializeAndStore(
        driver,
        name,
        cpg,
        baseLogger,
        generateParts(),
        (builder: DiffGraphBuilder, part: Method) => runOnPart(builder, part),
        keyPool
      )
    } finally {
      finish()
    }
  }

}
