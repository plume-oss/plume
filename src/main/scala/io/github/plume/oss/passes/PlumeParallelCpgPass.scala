package io.github.plume.oss.passes

import io.github.plume.oss.drivers.IDriver
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{DiffGraph, KeyPool, ParallelCpgPass, ParallelIteratorExecutor}

import java.util.concurrent.LinkedBlockingQueue

abstract class PlumeParallelCpgPass[T](
    cpg: Cpg,
    outName: String = "",
    keyPools: Option[Iterator[KeyPool]] = None
) extends ParallelCpgPass[T](cpg, outName, keyPools) {

  def createAndApply(driver: IDriver): Unit = {
    withWriter(driver) { writer =>
      enqueueInParallel(writer)
    }
  }

  private def withWriter[X](driver: IDriver)(f: Writer => Unit): Unit = {
    val writer = new Writer(driver)
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

  private def enqueueInParallel(writer: Writer): Unit = {
    withStartEndTimesLogged {
      try {
        init()
        val it = new ParallelIteratorExecutor(itWithKeyPools()).map {
          case (part, keyPool) =>
            // Note: write.enqueue(runOnPart(part)) would be wrong because
            // it would terminate the writer as soon as a pass returns None
            // as None is used as a termination symbol for the queue
            runOnPart(part).foreach(diffGraph => writer.enqueue(Some(diffGraph), keyPool))
        }
        consume(it)
      } catch {
        case exception: Exception =>
          baseLogger.warn(s"Exception in parallel CPG pass $name:", exception)
      }
    }
  }

  private def itWithKeyPools(): Iterator[(T, Option[KeyPool])] = {
    if (keyPools.isEmpty) {
      partIterator.map(p => (p, None))
    } else {
      val pools = keyPools.get
      partIterator.map { p =>
        (p, pools.nextOption() match {
          case Some(pool) => Some(pool)
          case None =>
            baseLogger.warn("Not enough key pools provided. Ids may not be constant across runs")
            None
        })
      }
    }
  }

  private def consume(it: Iterator[_]): Unit = {
    while (it.hasNext) {
      it.next()
    }
  }

  private class Writer(driver: IDriver) extends Runnable {

    case class DiffGraphAndKeyPool(diffGraph: Option[DiffGraph], keyPool: Option[KeyPool])

    private val queue = new LinkedBlockingQueue[DiffGraphAndKeyPool]

    def enqueue(diffGraph: Option[DiffGraph], keyPool: Option[KeyPool]): Unit = {
      queue.put(DiffGraphAndKeyPool(diffGraph, keyPool))
    }

    override def run(): Unit = {
      try {
        var terminate  = false
        while (!terminate) {
          queue.take() match {
            case DiffGraphAndKeyPool(Some(diffGraph), keyPool) =>
              val appliedDiffGraph = DiffGraph.Applier.applyDiff(diffGraph, cpg, keyPool = keyPool)
              // Reflect changes in driver
              driver.bulkTx(appliedDiffGraph)
            case DiffGraphAndKeyPool(None, _) =>
              baseLogger.debug("Shutting down WriterThread")
              terminate = true
          }
        }
      } catch {
        case exception: InterruptedException =>
          baseLogger.warn("Interrupted WriterThread", exception)
      }
    }
  }

}
