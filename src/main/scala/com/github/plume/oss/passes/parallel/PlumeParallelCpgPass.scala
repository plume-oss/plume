package com.github.plume.oss.passes.parallel

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeCpgPassBase
import com.github.plume.oss.passes.parallel.PlumeParallelCpgPass.{
  parallelEnqueue,
  parallelWithWriter
}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{DiffGraph, KeyPool, ParallelCpgPass, ParallelIteratorExecutor}
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits.global

abstract class PlumeParallelCpgPass[T](
    cpg: Cpg,
    keyPools: Option[Iterator[KeyPool]] = None
) extends ParallelCpgPass[T](cpg, keyPools = keyPools)
    with PlumeCpgPassBase {

  override def createAndApply(driver: IDriver): Unit = {
    withWriter(driver) { writer =>
      enqueueInParallel(writer)
    }
  }

  private def withWriter[X](driver: IDriver)(f: PlumeParallelWriter => Unit): Unit =
    parallelWithWriter[X](driver, f, cpg, baseLogger)

  private def enqueueInParallel(writer: PlumeParallelWriter): Unit =
    withStartEndTimesLogged {
      init()
      parallelEnqueue[T](
        baseLogger,
        name,
        writer,
        (part: T) => runOnPart(part),
        keyPools,
        partIterator
      )
    }

}

object PlumeParallelCpgPass {
  def parallelWithWriter[X](
      driver: IDriver,
      f: PlumeParallelWriter => Unit,
      cpg: Cpg,
      baseLogger: Logger
  ): Unit = {
    val writer       = new PlumeParallelWriter(driver, cpg)
    val writerThread = new Thread(writer)
    writerThread.setName("Writer")
    writerThread.start()
    try {
      f(writer)
    } catch {
      case exception: Exception =>
        baseLogger.warn("pass failed", exception)
    } finally {
      writer.enqueue(None, None)
      writerThread.join()
    }
  }

  def parallelEnqueue[T](
      baseLogger: Logger,
      name: String,
      writer: PlumeParallelWriter,
      runOnPart: T => Iterator[DiffGraph],
      keyPools: Option[Iterator[KeyPool]],
      partIterator: Iterator[T]
  ): Unit = {
    try {
      val it = new ParallelIteratorExecutor(
        parallelItWithKeyPools[T](
          baseLogger,
          keyPools,
          partIterator
        )
      ).map { case (part, keyPool) =>
        runOnPart(part).foreach(diffGraph => writer.enqueue(Some(diffGraph), keyPool))
      }
      consume(it)
    } catch {
      case exception: Exception =>
        baseLogger.warn(s"Exception in parallel CPG pass $name:", exception)
    }
  }

  def parallelItWithKeyPools[T](
      baseLogger: Logger,
      keyPools: Option[Iterator[KeyPool]],
      partIterator: Iterator[T]
  ): Iterator[(T, Option[KeyPool])] =
    if (keyPools.isEmpty) {
      partIterator.map(p => (p, None))
    } else {
      val pools = keyPools.get
      partIterator.map { p =>
        (
          p,
          pools.nextOption() match {
            case Some(pool) => Some(pool)
            case None =>
              baseLogger.warn("Not enough key pools provided. Ids may not be constant across runs")
              None
          }
        )
      }
    }

  private def consume(it: Iterator[_]): Unit = {
    while (it.hasNext) {
      it.next()
    }
  }

}
