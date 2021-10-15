package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.codepropertygraph.generated.nodes.Method
import io.shiftleft.passes.DiffGraph
import io.shiftleft.semanticcpg.passes.CfgCreationPass
import io.shiftleft.semanticcpg.passes.cfgcreation.CfgCreator

import java.util.concurrent.LinkedBlockingQueue
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object PlumeCfgCreationPass {
  private val writerQueueCapacity = 4
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

class PlumeCfgCreationPass(cpg: Cpg) extends CfgCreationPass(cpg) {

  def createAndApply(driver: IDriver): Unit = {
//    createAndApply() // Apply to reference graph first
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    import PlumeCfgCreationPass.producerQueueCapacity
    baseLogger.info(s"Start of enhancement: $name")
    val nanosStart = System.nanoTime()
    var nParts = 0
    var nDiff = 0

    init()
    val parts = generateParts()
    nParts = parts.length
    val partIter = parts.iterator
    val completionQueue = mutable.ArrayDeque[Future[DiffGraph]]()
    val writer = new Writer(driver)
    val writerThread = new Thread(writer)
    writerThread.setName("Writer")
    writerThread.start()
    try {
      try {
        // The idea is that we have a ringbuffer completionQueue that contains the workunits that are currently in-flight.
        // We add futures to the end of the ringbuffer, and take futures from the front.
        // then we await the future from the front, and add it to the writer-queue.
        // the end result is that we get deterministic output (esp. deterministic order of changes), while having up to one
        // writer-thread and up to producerQueueCapacity many threads in-flight.
        // as opposed to ParallelCpgPass, there is no race between diffgraph-generators to enqueue into the writer -- everything
        // is nice and ordered. Downside is that a very slow part may gum up the works (i.e. the completionQueue fills up and threads go idle)
        var done = false
        while (!done) {
          if (completionQueue.size < producerQueueCapacity && partIter.hasNext) {
            val next = partIter.next()
            //todo: Verify that we get FIFO scheduling; otherwise, do something about it.
            //if this e.g. used LIFO with 4 cores and 18 size of ringbuffer, then 3 cores may idle while we block on the front item.
            completionQueue.append(Future.apply {
              val builder = DiffGraph.newBuilder
              runOnPart(builder, next)
              builder.build()
            })
          } else if (completionQueue.nonEmpty) {
            val future = completionQueue.removeHead()
            val res = Await.result(future, Duration.Inf)
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
      // the nested finally is somewhat ugly -- but we promised to clean up with finish(), we want to include finish()
      // in the reported timings, and we must have our final log message if finish() throws
      val nanosStop = System.nanoTime()
      baseLogger.info(
        f"Enhancement $name completed in ${(nanosStop - nanosStart) * 1e-6}%.0f ms. ${nDiff}%d changes commited from ${nParts}%d parts.")
    }
  }

  private class Writer(driver: IDriver) extends Runnable {

    val queue = new LinkedBlockingQueue[Option[DiffGraph]](PlumeCfgCreationPass.writerQueueCapacity)

    override def run(): Unit = {
      try {
        var terminate = false
        while (!terminate) {
          queue.take() match {
            case None =>
              baseLogger.debug("Shutting down WriterThread")
              terminate = true
            case Some(diffGraph) =>
              val appliedDiffGraph = DiffGraph.Applier.applyDiff(diffGraph, cpg, undoable = false, None)
              driver.bulkTx(appliedDiffGraph)
          }
        }
      } catch {
        case exception: InterruptedException => baseLogger.warn("Interrupted WriterThread", exception)
      }
    }
  }
}
