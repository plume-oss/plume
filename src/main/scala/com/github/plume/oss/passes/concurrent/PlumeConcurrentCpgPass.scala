package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.concurrent.PlumeConcurrentCpgPass.concurrentCreateApply
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.AstNode
import io.shiftleft.passes.{ConcurrentWriterCpgPass, DiffGraph}
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class PlumeConcurrentCpgPass[T <: AstNode](cpg: Cpg)
    extends ConcurrentWriterCpgPass[T](cpg) {

  override def generateParts(): Array[_ <: AstNode] = Array[AstNode](null)

  def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeConcurrentCpgPass.producerQueueCapacity
    concurrentCreateApply[T](
      producerQueueCapacity,
      driver,
      name,
      baseLogger,
      _ => init(),
      _ => generateParts(),
      cpg,
      (x: DiffGraph.Builder, y: T) => runOnPart(x, y),
      _ => finish()
    )
  }

  override def runOnPart(builder: DiffGraph.Builder, part: T): Unit
}

object PlumeConcurrentCpgPass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()

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
