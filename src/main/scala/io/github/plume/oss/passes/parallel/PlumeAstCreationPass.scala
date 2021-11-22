package io.github.plume.oss.passes.parallel

import io.github.plume.oss.Jimple2Cpg
import io.github.plume.oss.passes.IncrementalKeyPool
import io.shiftleft.codepropertygraph.Cpg
import io.shiftleft.passes.DiffGraph
import org.slf4j.LoggerFactory
import soot.Scene

import java.util.concurrent.ConcurrentSkipListSet

case class Global(
    usedTypes: ConcurrentSkipListSet[String] = new ConcurrentSkipListSet[String]()
)

/** Creates the AST layer from the given class file and stores all types in the given global parameter.
  */
class AstCreationPass(
    codePath: String,
    filenames: List[String],
    cpg: Cpg,
    keyPool: IncrementalKeyPool
) extends PlumeParallelCpgPass[String](cpg, keyPools = Some(keyPool.split(filenames.size))) {

  val global: Global = Global()
  private val logger = LoggerFactory.getLogger(classOf[AstCreationPass])

  override def partIterator: Iterator[String] = filenames.iterator

  override def runOnPart(filename: String): Iterator[DiffGraph] = {
    val qualifiedClassName = Jimple2Cpg.getQualifiedClassPath(codePath, filename)
    try {
      val sootClass = Scene.v().loadClassAndSupport(qualifiedClassName)
      sootClass.setApplicationClass()
      new PlumeAstCreator(filename, global).createAst(sootClass)
    } catch {
      case e: Exception =>
        logger.warn(s"Cannot parse: $filename ($qualifiedClassName)", e)
        Iterator()
    }
  }

}
