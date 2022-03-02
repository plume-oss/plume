package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.concurrent.PlumeConcurrentCpgPass.nDiffT
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{CpgPass, KeyPool}
import org.slf4j.{Logger, LoggerFactory, MDC}
import overflowdb.BatchedUpdate.DiffGraph

import java.util.concurrent.LinkedBlockingQueue

object PlumeConcurrentWriter {
  private val writerQueueCapacity = 4
}
class PlumeConcurrentWriter(
    driver: IDriver,
    cpg: Cpg,
    baseLogger: Logger = LoggerFactory.getLogger(classOf[CpgPass]),
    keyPool: Option[KeyPool] = None,
    mdc: java.util.Map[String, String]
) extends Runnable {

  val queue: LinkedBlockingQueue[Option[DiffGraph]] =
    new LinkedBlockingQueue[Option[DiffGraph]](PlumeConcurrentWriter.writerQueueCapacity)

  @volatile var raisedException: Exception = null

  override def run(): Unit = {
    try {
      nDiffT = 0
      MDC.setContextMap(mdc)
      var terminate = false
      while (!terminate) {
        queue.take() match {
          case None =>
            baseLogger.debug("Shutting down WriterThread")
            terminate = true
          case Some(diffGraph) =>
            val appliedDiffGraph = overflowdb.BatchedUpdate
              .applyDiff(cpg.graph, diffGraph, keyPool.orNull, null)

            nDiffT += appliedDiffGraph
              .transitiveModifications()
            driver.bulkTx(appliedDiffGraph)
        }
      }
    } catch {
      case exception: InterruptedException => baseLogger.warn("Interrupted WriterThread", exception)
      case exc: Exception =>
        raisedException = exc
        queue.clear()
        throw exc
    }
  }
}
