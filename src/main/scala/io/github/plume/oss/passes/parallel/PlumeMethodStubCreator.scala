package io.github.plume.oss.passes.parallel

import io.github.plume.oss.drivers.IDriver
import io.github.plume.oss.passes.{IncrementalKeyPool, PlumeCpgPassBase}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{DiffGraph, KeyPool, ParallelIteratorExecutor}
import io.shiftleft.semanticcpg.passes.languagespecific.fuzzyc.{MethodStubCreator, NameAndSignature}

class PlumeMethodStubCreator(
    cpg: Cpg,
    keyPool: Option[IncrementalKeyPool],
    blacklist: Set[NameAndSignature] = Set()
) extends MethodStubCreator(cpg)
    with PlumeCpgPassBase {

  var keyPools: Option[Iterator[KeyPool]] = None

  override def init(): Unit = {
    super.init()
    keyPool match {
      case Some(value) => keyPools = Option(value.split(partIterator.size))
      case None        =>
    }
  }

  // Do not create stubs for methods that exist
  override def runOnPart(part: (NameAndSignature, Int)): Iterator[DiffGraph] = {
    if (blacklist.contains(part._1)) {
      Iterator()
    } else {
      super.runOnPart(part)
    }
  }

  override def createAndApply(driver: IDriver): Unit = {
    withWriter(driver) { writer =>
      enqueueInParallel(writer)
    }
  }

  private def withWriter[X](driver: IDriver)(f: PlumeParallelWriter => Unit): Unit = {
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

  private def enqueueInParallel(writer: PlumeParallelWriter): Unit = {
    withStartEndTimesLogged {
      try {
        init()
        val it = new ParallelIteratorExecutor(itWithKeyPools()).map { case (part, keyPool) =>
          runOnPart(part).foreach(diffGraph => writer.enqueue(Some(diffGraph), keyPool))
        }
        consume(it)
      } catch {
        case exception: Exception =>
          baseLogger.warn(s"Exception in parallel CPG pass $name:", exception)
      }
    }
  }

  private def itWithKeyPools(): Iterator[((NameAndSignature, Int), Option[KeyPool])] = {
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
  }

  private def consume(it: Iterator[_]): Unit = {
    while (it.hasNext) {
      it.next()
    }
  }

}
