package com.github.plume.oss.passes.parallel

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.parallel.PlumeParallelCpgPass.{parallelEnqueue, parallelWithWriter}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.KeyPool
import io.shiftleft.semanticcpg.passes.controlflow.cfgdominator.CfgDominatorPass

class PlumeCfgDominatorPass(cpg: Cpg, keyPools: Option[Iterator[KeyPool]] = None)
    extends CfgDominatorPass(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withWriter(driver) { writer =>
      enqueueInParallel(writer)
    }
  }

  private def withWriter[X](driver: IDriver)(f: PlumeParallelWriter => Unit): Unit =
    parallelWithWriter(driver, f, cpg, baseLogger)

  private def enqueueInParallel(writer: PlumeParallelWriter): Unit =
    withStartEndTimesLogged {
      parallelEnqueue[Method](
        baseLogger,
        name,
        _ => init(),
        writer,
        (part: Method) => runOnPart(part),
        keyPools,
        partIterator
      )
    }

}
