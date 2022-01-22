package com.github.plume.oss.passes.parallel

import com.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{CpgPass, DiffGraph, KeyPool}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.LinkedBlockingQueue

private class PlumeParallelWriter(
    driver: IDriver,
    cpg: Cpg,
    baseLogger: Logger = LoggerFactory.getLogger(classOf[CpgPass])
) extends Runnable {

  final case class DiffGraphAndKeyPool(diffGraph: Option[DiffGraph], keyPool: Option[KeyPool])

  private val queue = new LinkedBlockingQueue[DiffGraphAndKeyPool]

  def enqueue(diffGraph: Option[DiffGraph], keyPool: Option[KeyPool]): Unit = {
    queue.put(DiffGraphAndKeyPool(diffGraph, keyPool))
  }

  override def run(): Unit = {
    try {
      var terminate = false
      while (!terminate) {
        queue.take() match {
          case DiffGraphAndKeyPool(Some(diffGraph), keyPool) =>
            val appliedDiffGraph = DiffGraph.Applier.applyDiff(diffGraph, cpg, keyPool = keyPool)
            // Reflect changes in driver
            driver.bulkTx(appliedDiffGraph)
          case DiffGraphAndKeyPool(None, _) =>
            baseLogger.debug("Shutting down WriterThread")
            terminate = true
          case _ =>
        }
      }
    } catch {
      case exception: InterruptedException =>
        baseLogger.warn("Interrupted WriterThread", exception)
    }
  }
}
