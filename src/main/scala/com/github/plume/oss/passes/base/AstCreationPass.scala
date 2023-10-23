package com.github.plume.oss.passes.base

import com.github.plume.oss.JimpleAst2Database
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeConcurrentWriterPass
import io.joern.x2cpg.datastructures.Global
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.DiffGraphBuilder
import soot.Scene

/** Creates the AST layer from the given class file and stores all types in the given global parameter.
  */
class AstCreationPass(filenames: List[String], driver: IDriver) extends PlumeConcurrentWriterPass[String](driver) {

  val global: Global = new Global()
  private val logger = LoggerFactory.getLogger(classOf[AstCreationPass])

  override def generateParts(): Array[_ <: AnyRef] = filenames.toArray

  override def runOnPart(builder: DiffGraphBuilder, part: String): Unit = {
    val qualifiedClassName = JimpleAst2Database.getQualifiedClassPath(part)
    try {
      val cNameNoSuff = qualifiedClassName.stripSuffix(".class").stripSuffix(".java")
      val sootClass =
        if (qualifiedClassName.contains(".class")) Scene.v().loadClassAndSupport(cNameNoSuff)
        else Scene.v().getSootClass(cNameNoSuff)
      sootClass.setApplicationClass()
      val localDiff = new io.joern.jimple2cpg.passes.AstCreator(part, sootClass, global).createAst()
      builder.absorb(localDiff)
    } catch {
      case e: Exception =>
        logger.warn(s"Cannot parse: $part ($qualifiedClassName)", e)
        Iterator()
    }
  }

}
