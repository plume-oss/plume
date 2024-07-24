package com.github.plume.oss.passes.base

import better.files.File
import com.github.plume.oss.JimpleAst2Database
import com.github.plume.oss.drivers.IDriver
import com.github.plume.oss.passes.PlumeForkJoinParallelCpgPass
import io.joern.x2cpg.ValidationMode
import io.joern.x2cpg.datastructures.Global
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.DiffGraphBuilder
import soot.Scene
import java.io.File as JFile
import java.nio.file.Paths

/** Creates the AST layer from the given class file and stores all types in the given global parameter.
  */
class AstCreationPass(filenames: List[String], driver: IDriver, unpackingRoot: File)
    extends PlumeForkJoinParallelCpgPass[String](driver) {

  val global: Global = new Global()
  private val logger = LoggerFactory.getLogger(classOf[AstCreationPass])

  override def generateParts(): Array[String] = filenames.toArray

  /** Formats the file name the way Soot refers to classes within a class path. e.g.
    * /unrelated/paths/class/path/Foo.class => class.path.Foo
    */
  private def getQualifiedClassPath(filename: String): String = {
    val codePath = unpackingRoot.pathAsString
    filename
      .replace(codePath + JFile.separator, "")
      .replace(JFile.separator, ".")
  }

  override def runOnPart(builder: DiffGraphBuilder, part: String): Unit = {
    val qualifiedClassName = getQualifiedClassPath(part)
    try {
      val cNameNoSuff = qualifiedClassName.stripSuffix(".class").stripSuffix(".java")
      val sootClass =
        if (qualifiedClassName.contains(".class")) Scene.v().loadClassAndSupport(cNameNoSuff)
        else Scene.v().getSootClass(cNameNoSuff)
      sootClass.setApplicationClass()
      val localDiff =
        new io.joern.jimple2cpg.astcreation.AstCreator(part, sootClass, global)(ValidationMode.Disabled).createAst()
      builder.absorb(localDiff)
    } catch {
      case e: Exception =>
        logger.warn(s"Cannot parse: $part ($qualifiedClassName)", e)
        Iterator()
    }
  }

}
