package com.github.plume.oss.passes.forkjoin

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPass
import com.github.plume.oss.passes.forkjoin.PlumeForkJoinParallelCpgPass.forkJoinSerializeAndStore
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{ForkJoinParallelCpgPass, KeyPool}
import org.slf4j.Logger

import java.util.function.{BiConsumer, Supplier}

object PlumeForkJoinParallelCpgPass {

  type DiffGraphBuilder = overflowdb.BatchedUpdate.DiffGraphBuilder

  def forkJoinSerializeAndStore[T](
      driver: IDriver,
      name: String,
      cpg: Cpg,
      baseLogger: Logger,
      parts: Array[_ <: AnyRef],
      runOnPart: (DiffGraphBuilder, T) => Unit,
      keyPool: Option[KeyPool],
      blacklist: Set[String] = Set.empty,
      blacklistProperty: String = "",
      blacklistRejectOnFail: Boolean = false
  ): Unit = {
    baseLogger.info(s"Start of pass: $name")
    val nanosStart = System.nanoTime()
    var nParts     = 0
    var nanosBuilt = -1L
    var nDiff      = -1
    var nDiffT     = -1
    try {
      nParts = parts.length
      val diffGraph = nParts match {
        case 0 => (new DiffGraphBuilder).build()
        case 1 =>
          val builder = new DiffGraphBuilder
          runOnPart(builder, parts(0).asInstanceOf[T])
          builder.build()
        case _ =>
          java.util.Arrays
            .stream(parts)
            .parallel()
            .collect(
              new Supplier[DiffGraphBuilder] {
                override def get(): DiffGraphBuilder =
                  new DiffGraphBuilder
              },
              new BiConsumer[DiffGraphBuilder, AnyRef] {
                override def accept(builder: DiffGraphBuilder, part: AnyRef): Unit =
                  runOnPart(builder, part.asInstanceOf[T])
              },
              new BiConsumer[DiffGraphBuilder, DiffGraphBuilder] {
                override def accept(leftBuilder: DiffGraphBuilder, rightBuilder: DiffGraphBuilder)
                    : Unit =
                  leftBuilder.absorb(rightBuilder)
              }
            )
            .build()
      }
      nanosBuilt = System.nanoTime()
      nDiff = diffGraph.size()
      val diffToCommit =
        if (blacklist.nonEmpty)
          PlumeCpgPass
            .filterBatchedDiffGraph(diffGraph, blacklistProperty, blacklist, blacklistRejectOnFail)
        else
          diffGraph

      val appliedDiffGraph = overflowdb.BatchedUpdate
        .applyDiff(cpg.graph, diffToCommit, keyPool.orNull, null)
      driver.bulkTx(appliedDiffGraph)
      nDiffT = appliedDiffGraph.transitiveModifications()

    } catch {
      case exc: Exception =>
        baseLogger.error(s"Pass ${name} failed", exc)
        throw exc
    } finally {
      val nanosStop = System.nanoTime()
      val fracRun =
        if (nanosBuilt == -1) 100.0
        else (nanosBuilt - nanosStart) * 100.0 / (nanosStop - nanosStart + 1)
      baseLogger.info(
        f"Pass $name completed in ${(nanosStop - nanosStart) * 1e-6}%.0f ms (${fracRun}%.0f%% on mutations). ${nDiff}%d + ${nDiffT - nDiff}%d changes commited from ${nParts}%d parts."
      )
    }
  }

}

abstract class PlumeForkJoinParallelCpgPass[T <: AnyRef](cpg: Cpg, keyPool: Option[KeyPool] = None)
    extends ForkJoinParallelCpgPass[T](cpg, keyPool = keyPool) {

  def createAndApply(driver: IDriver): Unit = {
    createApplySerializeAndStore(driver) // Apply to driver
  }

  def createApplySerializeAndStore(driver: IDriver): Unit = {
    try {
      init()
      forkJoinSerializeAndStore(
        driver,
        name,
        cpg,
        baseLogger,
        generateParts(),
        (builder: DiffGraphBuilder, part: T) => runOnPart(builder, part),
        keyPool
      )
    } finally {
      finish()
    }
  }

}
