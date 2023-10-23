package com.github.plume.oss.passes

import com.github.plume.oss.drivers.IDriver
import io.shiftleft.SerializedCpg
import io.shiftleft.utils.ExecutionContextProvider
import overflowdb.BatchedUpdate.DiffGraphBuilder

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object PlumeConcurrentWriterPass {
  private val writerQueueCapacity   = 4
  private val producerQueueCapacity = 2 + 4 * Runtime.getRuntime.availableProcessors()
}

abstract class PlumeConcurrentWriterPass[T <: AnyRef](driver: IDriver) {

  @volatile var nDiffT = -1

  def generateParts(): Array[_ <: AnyRef]

  // main function: add desired changes to builder
  def runOnPart(builder: DiffGraphBuilder, part: T): Unit

  def createAndApply(): Unit = {
    import PlumeConcurrentWriterPass.producerQueueCapacity
    var nParts = 0
    var nDiff  = 0
    nDiffT = -1
    val parts = generateParts()
    nParts = parts.length
    val partIter                      = parts.iterator
    val completionQueue               = mutable.ArrayDeque[Future[overflowdb.BatchedUpdate.DiffGraph]]()
    implicit val ec: ExecutionContext = ExecutionContextProvider.getExecutionContext
    var done                          = false
    while (!done) {
      if (completionQueue.size < producerQueueCapacity && partIter.hasNext) {
        val next = partIter.next()
        completionQueue.append(Future.apply {
          val builder = new DiffGraphBuilder
          runOnPart(builder, next.asInstanceOf[T])
          val builtGraph = builder.build()
          driver.bulkTx(builtGraph)
          builtGraph
        })
      } else if (completionQueue.nonEmpty) {
        val future = completionQueue.removeHead()
        val res    = Await.result(future, Duration.Inf)
        nDiff += res.size
      } else {
        done = true
      }
    }
  }

}
