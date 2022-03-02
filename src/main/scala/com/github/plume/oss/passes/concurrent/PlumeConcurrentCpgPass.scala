package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.concurrent.PlumeConcurrentCpgPass.concurrentCreateApply
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import io.shiftleft.passes.{ConcurrentWriterCpgPass, KeyPool}
import io.shiftleft.utils.ExecutionContextProvider
import org.slf4j.{Logger, MDC}
import overflowdb.BatchedUpdate.{DiffGraph, DiffGraphBuilder}

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class PlumeConcurrentCpgPass[T <: AstNode](cpg: Cpg, keyPool: Option[KeyPool])
    extends ConcurrentWriterCpgPass[T](cpg) {

  override def generateParts(): Array[_ <: AstNode] = Array[AstNode](null)

  def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeConcurrentCpgPass.producerQueueCapacity
    try {
      init()
      concurrentCreateApply[T](
        producerQueueCapacity,
        driver,
        name,
        baseLogger,
        generateParts(),
        cpg,
        (x: DiffGraphBuilder, y: T) => runOnPart(x, y),
        keyPool,
        (newDiff: Int) => {
          nDiffT = newDiff
          nDiffT
        }
      )
    } finally {
      finish()
    }
  }

  override def runOnPart(builder: DiffGraphBuilder, part: T): Unit
}

object PlumeConcurrentCpgPass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()

  def concurrentCreateApply[T](
      producerQueueCapacity: Long,
      driver: IDriver,
      name: String,
      baseLogger: Logger,
      parts: Array[_ <: AstNode],
      cpg: Cpg,
      runOnPart: (DiffGraphBuilder, T) => Unit,
      keyPool: Option[KeyPool],
      setDiff: Int => Int
  ): Unit = {
    baseLogger.info(s"Start of enhancement: $name")
    val nanosStart = System.nanoTime()
    var nParts     = 0
    var nDiff      = setDiff(0)
    // init is called outside of this method
    nParts = parts.length
    val partIter        = parts.iterator
    val completionQueue = mutable.ArrayDeque[Future[DiffGraph]]()
    val writer =
      new PlumeConcurrentWriter(driver, cpg, baseLogger, keyPool, MDC.getCopyOfContextMap, setDiff)
    val writerThread = new Thread(writer)
    writerThread.setName("Writer")
    writerThread.start()
    implicit val ec: ExecutionContext = ExecutionContextProvider.getExecutionContext
    try {
      try {
        var done = false
        while (!done && writer.raisedException == null) {
          if (writer.raisedException != null)
            throw writer.raisedException

          if (completionQueue.size < producerQueueCapacity && partIter.hasNext) {
            val next = partIter.next()
            completionQueue.append(Future.apply {
              val builder = new DiffGraphBuilder
              runOnPart(builder, next.asInstanceOf[T])
              builder.build()
            })
          } else if (completionQueue.nonEmpty) {
            val future = completionQueue.removeHead()
            val res    = Await.result(future, Duration.Inf)
            nDiff = setDiff(nDiff + res.size)
            writer.queue.put(Some(res))
          } else {
            done = true
          }
        }
      } finally {
        try {
          if (writer.raisedException == null) writer.queue.put(None)
          writerThread.join()
          if (writer.raisedException != null)
            throw new RuntimeException("Failure in diffgraph application", writer.raisedException)
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
