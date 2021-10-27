package io.github.plume.oss.passes.concurrent

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.PlumeCpgPassBase
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.DiffGraph
import io.shiftleft.semanticcpg.passes.CfgCreationPass

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object PlumeCfgCreationPass {
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

class PlumeCfgCreationPass(cpg: Cpg) extends CfgCreationPass(cpg) with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeCfgCreationPass.producerQueueCapacity
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
              runOnPart(builder, next)
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
