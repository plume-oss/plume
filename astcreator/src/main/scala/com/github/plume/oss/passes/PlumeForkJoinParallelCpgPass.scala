package com.github.plume.oss.passes

import com.github.plume.oss.drivers.IDriver
import io.shiftleft.SerializedCpg
import io.shiftleft.codepropertygraph.generated.Cpg
import io.shiftleft.utils.ExecutionContextProvider
import io.shiftleft.codepropertygraph.generated.nodes.AbstractNode
import io.shiftleft.passes.CpgPassBase
import overflowdb.BatchedUpdate.DiffGraphBuilder

import java.util.function.*
import scala.annotation.nowarn
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

abstract class PlumeForkJoinParallelCpgPass[T <: AnyRef](driver: IDriver, @nowarn outName: String = "")
    extends CpgPassBase {

  // generate Array of parts that can be processed in parallel
  def generateParts(): Array[? <: AnyRef]

  // setup large data structures, acquire external resources
  def init(): Unit = {}

  // release large data structures and external resources
  def finish(): Unit = {}

  // main function: add desired changes to builder
  def runOnPart(builder: DiffGraphBuilder, part: T): Unit

  // Override this to disable parallelism of passes. Useful for debugging.
  def isParallel: Boolean = true

  override def createAndApply(): Unit = createApplySerializeAndStore(null)

  override def runWithBuilder(externalBuilder: DiffGraphBuilder): Int = {
    try {
      init()
      val parts  = generateParts()
      val nParts = parts.size
      nParts match {
        case 0 =>
        case 1 =>
          runOnPart(externalBuilder, parts(0).asInstanceOf[T])
        case _ =>
          val stream =
            if (!isParallel)
              java.util.Arrays
                .stream(parts)
                .sequential()
            else
              java.util.Arrays
                .stream(parts)
                .parallel()
          val diff = stream.collect(
            new Supplier[DiffGraphBuilder] {
              override def get(): DiffGraphBuilder =
                Cpg.newDiffGraphBuilder
            },
            new BiConsumer[DiffGraphBuilder, AnyRef] {
              override def accept(builder: DiffGraphBuilder, part: AnyRef): Unit =
                runOnPart(builder, part.asInstanceOf[T])
            },
            new BiConsumer[DiffGraphBuilder, DiffGraphBuilder] {
              override def accept(leftBuilder: DiffGraphBuilder, rightBuilder: DiffGraphBuilder): Unit =
                leftBuilder.absorb(rightBuilder)
            }
          )
          externalBuilder.absorb(diff)
      }
      nParts
    } finally {
      finish()
    }
  }

  override def createApplySerializeAndStore(serializedCpg: SerializedCpg, prefix: String = ""): Unit = {
    baseLogger.info(s"Start of pass: $name")
    val nanosStart = System.nanoTime()
    var nParts     = 0
    var nanosBuilt = -1L
    var nDiff      = -1
    var nDiffT     = -1
    try {
      val diffGraph = Cpg.newDiffGraphBuilder
      nParts = runWithBuilder(diffGraph)
      nanosBuilt = System.nanoTime()
      nDiff = diffGraph.size
      driver.bulkTx(diffGraph)
    } catch {
      case exc: Exception =>
        baseLogger.error(s"Pass ${name} failed", exc)
        throw exc
    } finally {
      try {
        finish()
      } finally {
        // the nested finally is somewhat ugly -- but we promised to clean up with finish(), we want to include finish()
        // in the reported timings, and we must have our final log message if finish() throws
        val nanosStop = System.nanoTime()
        val fracRun   = if (nanosBuilt == -1) 0.0 else (nanosStop - nanosBuilt) * 100.0 / (nanosStop - nanosStart + 1)
        val serializationString = if (serializedCpg != null && !serializedCpg.isEmpty) {
          " Diff serialized and stored."
        } else ""
        baseLogger.info(
          f"Pass $name completed in ${(nanosStop - nanosStart) * 1e-6}%.0f ms (${fracRun}%.0f%% on mutations). ${nDiff}%d + ${nDiffT - nDiff}%d changes committed from ${nParts}%d parts.${serializationString}%s"
        )
      }
    }
  }

}
