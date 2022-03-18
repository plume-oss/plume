package com.github.plume.oss.passes.base

import com.github.plume.oss.Jimple2Cpg
import com.github.plume.oss.passes.{IncrementalKeyPool, PlumeConcurrentCpgPass}
import io.joern.x2cpg.datastructures.Global
import io.shiftleft.codepropertygraph.Cpg
import org.slf4j.LoggerFactory
import soot.Scene

/** Creates the AST layer from the given class file and stores all types in the given global parameter.
  */
class AstCreationPass(
    filenames: List[String],
    cpg: Cpg,
    keyPool: IncrementalKeyPool
) extends PlumeConcurrentCpgPass[String](cpg, keyPool = Some(keyPool)) {

  val global: Global = new Global()
  private val logger = LoggerFactory.getLogger(classOf[AstCreationPass])

  override def generateParts(): Array[_ <: AnyRef] = filenames.toArray

  override def runOnPart(builder: DiffGraphBuilder, part: String): Unit = {
    val qualifiedClassName = Jimple2Cpg.getQualifiedClassPath(part)
    try {
      val sootClass = Scene.v().loadClassAndSupport(qualifiedClassName)
      sootClass.setApplicationClass()
      new io.joern.jimple2cpg.passes.AstCreator(part, builder, global).createAst(sootClass)
    } catch {
      case e: Exception =>
        logger.warn(s"Cannot parse: $part ($qualifiedClassName)", e)
        Iterator()
    }
  }

}
