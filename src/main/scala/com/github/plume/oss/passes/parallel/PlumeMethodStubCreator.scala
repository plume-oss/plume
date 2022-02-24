package com.github.plume.oss.passes.parallel

import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.parallel.PlumeParallelCpgPass.{
  parallelEnqueue,
  parallelItWithKeyPools,
  parallelWithWriter
}
import com.github.plume.oss.passes.{IncrementalKeyPool, PlumeCpgPassBase}
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.{DiffGraph, KeyPool, ParallelIteratorExecutor}
import io.shiftleft.semanticcpg.passes.base.{MethodStubCreator, NameAndSignature}

class PlumeMethodStubCreator(
    cpg: Cpg,
    keyPool: Option[IncrementalKeyPool],
    blacklist: Set[String] = Set()
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
    val methodTypeName = part._1.fullName.replace(s".${part._1.name}:${part._1.signature}", "")
    if (blacklist.contains(methodTypeName) || (blacklist.nonEmpty && methodTypeName == "<empty>")) {
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

  private def withWriter[X](driver: IDriver)(f: PlumeParallelWriter => Unit): Unit =
    parallelWithWriter[X](driver, f, cpg, baseLogger)

  private def enqueueInParallel(writer: PlumeParallelWriter): Unit =
    withStartEndTimesLogged {
      init()
      parallelEnqueue(
        baseLogger,
        name,
        writer,
        (x: (NameAndSignature, Int)) => runOnPart(x),
        keyPools,
        partIterator
      )
    }

}
