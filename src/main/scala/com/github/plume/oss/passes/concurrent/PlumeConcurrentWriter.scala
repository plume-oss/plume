package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{CpgPass, DiffGraph}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.LinkedBlockingQueue

object PlumeConcurrentWriter {
  private val writerQueueCapacity = 4
}
class PlumeConcurrentWriter(
    driver: IDriver,
    cpg: Cpg,
    baseLogger: Logger = LoggerFactory.getLogger(classOf[CpgPass])
) extends Runnable {

  val queue: LinkedBlockingQueue[Option[DiffGraph]] =
    new LinkedBlockingQueue[Option[DiffGraph]](PlumeConcurrentWriter.writerQueueCapacity)

  override def run(): Unit = {
    try {
      var terminate = false
      while (!terminate) {
        queue.take() match {
          case None =>
            baseLogger.debug("Shutting down WriterThread")
            terminate = true
          case Some(diffGraph) =>
            val appliedDiffGraph =
              DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, None)
            driver.bulkTx(appliedDiffGraph)
        }
      }
    } catch {
      case exception: InterruptedException => baseLogger.warn("Interrupted WriterThread", exception)
    }
  }
}
