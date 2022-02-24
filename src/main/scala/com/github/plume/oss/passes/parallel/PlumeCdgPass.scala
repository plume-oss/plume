package com.github.plume.oss.passes.parallel

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.parallel.PlumeParallelCpgPass.parallelWithWriter
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.{KeyPool, ParallelIteratorExecutor}
import io.shiftleft.semanticcpg.passes.controlflow.codepencegraph.CdgPass

class PlumeCdgPass(cpg: Cpg, keyPools: Option[Iterator[KeyPool]] = None)
    extends CdgPass(cpg)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withWriter(driver) { writer =>
      enqueueInParallel(writer)
    }
  }

  private def withWriter[X](driver: IDriver)(f: PlumeParallelWriter => Unit): Unit =
    parallelWithWriter(driver, f, cpg, baseLogger)

  private def enqueueInParallel(writer: PlumeParallelWriter): Unit = {
    withStartEndTimesLogged {
      try {
        init()
        val it = new ParallelIteratorExecutor(itWithKeyPools()).map { case (part, keyPool) =>
          runOnPart(part).foreach(diffGraph => writer.enqueue(Some(diffGraph), keyPool))
        }
        consume(it)
      } catch {
        case exception: Exception =>
          baseLogger.warn(s"Exception in parallel CPG pass $name:", exception)
      }
    }
  }

  private def itWithKeyPools(): Iterator[(Method, Option[KeyPool])] = {
    if (keyPools.isEmpty) {
      partIterator.map(p => (p, None))
    } else {
      val pools = keyPools.get
      partIterator.map { p =>
        (
          p,
          pools.nextOption() match {
            case Some(pool) => Some(pool)
            case None =>
              baseLogger.warn("Not enough key pools provided. Ids may not be constant across runs")
              None
          }
        )
      }
    }
  }

  private def consume(it: Iterator[_]): Unit = {
    while (it.hasNext) {
      it.next()
    }
  }
}
