package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import io.shiftleft.passes.{CpgPass, DiffGraph}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.LinkedBlockingQueue
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object PlumeConcurrentWriter {
  private val writerQueueCapacity = 4

  def concurrentCreateApply[T](
      producerQueueCapacity: Long,
      driver: IDriver,
      name: String,
      baseLogger: Logger,
      init: Unit => Unit,
      generateParts: Unit => Array[_ <: AstNode],
      cpg: Cpg,
      runOnPart: (DiffGraph.Builder, T) => Unit,
      finish: Unit => Unit
  ): Unit = {
    baseLogger.info(s"Start of enhancement: $name")
    val nanosStart = System.nanoTime()
    var nParts     = 0
    var nDiff      = 0

    init()
    val parts = generateParts()
    nParts = parts.length
    val partIter        = parts.iterator
    val completionQueue = mutable.ArrayDeque[Future[DiffGraph]]()
    val writer          = new PlumeConcurrentWriter(driver, cpg)
    val writerThread    = new Thread(writer)
    writerThread.setName("Writer")
    writerThread.start()
    try {
      try {
        var done = false
        while (!done) {
          if (completionQueue.size < producerQueueCapacity && partIter.hasNext) {
            val next = partIter.next()
            completionQueue.append(Future.apply {
              val builder = DiffGraph.newBuilder
              runOnPart(builder, next.asInstanceOf[T])
              builder.build()
            })
          } else if (completionQueue.nonEmpty) {
            val future = completionQueue.removeHead()
            val res    = Await.result(future, Duration.Inf)
            nDiff += res.size
            writer.queue.put(Some(res))
          } else {
            done = true
          }
        }
      } finally {
        try {
          writer.queue.put(None)
          writerThread.join()
        } finally {
          finish()
        }
      }
    } finally {
      val nanosStop = System.nanoTime()
      baseLogger.info(
        f"Enhancement $name completed in ${(nanosStop - nanosStart) * 1e-6}%.0f ms. ${nDiff}%d changes commited from ${nParts}%d parts."
      )
    }
  }
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
