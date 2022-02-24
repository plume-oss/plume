package com.github.plume.oss.passes.parallel

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.parallel.PlumeParallelCpgPass.{parallelEnqueue, parallelWithWriter}
import io.joern.dataflowengineoss.passes.reachingdef.ReachingDefPass
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.KeyPool
import io.shiftleft.semanticcpg.language._

class PlumeReachingDefPass(
    cpg: Cpg,
    keyPools: Option[Iterator[KeyPool]] = None,
    unchangedTypes: Set[String] = Set.empty[String]
) extends ReachingDefPass(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withWriter(driver) { writer =>
      enqueueInParallel(writer)
    }
  }

  override def partIterator: Iterator[Method] = cpg.method.internal.iterator.filterNot { m =>
    val typeFullName = m.fullName.substring(0, m.fullName.lastIndexOf('.'))
    unchangedTypes.contains(typeFullName)
  }

  private def withWriter[X](driver: IDriver)(f: PlumeParallelWriter => Unit): Unit =
    parallelWithWriter[X](driver, f, cpg, baseLogger)

  private def enqueueInParallel(writer: PlumeParallelWriter): Unit =
    withStartEndTimesLogged {
      init()
      parallelEnqueue[Method](
        baseLogger,
        name,
        writer,
        (part: Method) => runOnPart(part),
        keyPools,
        partIterator
      )
    }

}
