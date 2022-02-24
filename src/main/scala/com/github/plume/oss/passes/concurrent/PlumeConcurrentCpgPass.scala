package com.github.plume.oss.passes.concurrent

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.concurrent.PlumeConcurrentWriter.concurrentCreateApply
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
}
