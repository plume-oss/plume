package io.github.plume.oss.drivers

import io.github.plume.oss.drivers.TigerGraphDriver._
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes}
import io.shiftleft.codepropertygraph.generated.nodes._
import io.shiftleft.passes.AppliedDiffGraph
import org.slf4j.LoggerFactory
import sttp.client3._

import java.io.{ByteArrayOutputStream, PrintStream}
import java.security.Permission
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

/** The driver used to communicate to a remote TigerGraph instance. One must build a schema on the first use of the database.
  */
class TigerGraphDriver(
    hostname: String = DEFAULT_HOSTNAME,
    restPpPort: Int = DEFAULT_RESTPP_PORT,
    gsqlPort: Int = DEFAULT_GSQL_PORT,
    username: String = DEFAULT_USERNAME,
    password: String = DEFAULT_PASSWORD,
    timeout: Int = DEFAULT_TIMEOUT,
    secure: Boolean = false,
    authKey: String = ""
) extends IDriver
    with ISchemaSafeDriver {

  private val logger = LoggerFactory.getLogger(classOf[TigerGraphDriver])
  private val api    = s"${if (secure) "https" else "http"}://$hostname:$restPpPort"

  override def isConnected: Boolean = true

  override def clear(): Unit = {}

  /** Does nothing as HTTP does not require closing
    */
  override def close(): Unit = {}

  override def exists(nodeId: Long): Boolean = ???

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean = ???

  private def nodePayload(id: Long, n: NewNode): Map[String, Any] = {
    val attributes = removeLists(n.properties).map { case (k, v) =>
      val vStr = v match {
        case x: Seq[String] => x.headOption.getOrElse(IDriver.STRING_DEFAULT)
        case x if x == null => IDriver.getPropertyDefault(k)
        case x              => x
      }
      s"_$k" -> Map("value" -> vStr)
    }
    Map(s"${n.label}_" -> Map(id -> attributes))
  }

  override def bulkTx(dg: AppliedDiffGraph): Unit = ???

  override def deleteNodeWithChildren(
      nodeType: String,
      edgeToFollow: String,
      propertyKey: String,
      propertyValue: Any
  ): Unit = ???

  override def removeSourceFiles(filenames: String*): Unit = ???

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = ???

  override def idInterval(lower: Long, upper: Long): Set[Long] = ???

  override def linkAstNodes(
      srcLabels: List[String],
      edgeType: String,
      dstNodeMap: mutable.Map[String, Long],
      dstFullNameKey: String
  ): Unit = ???

  override def buildSchema(): Unit = postGSQL(buildSchemaPayload())

  override def buildSchemaPayload(): String = {
    s"""
      |DROP ALL
      |$VERTICES
      |$EDGES
      |CREATE GRAPH cpg(*)
      |""".stripMargin
  }

  private def request(): RequestT[Empty, Either[String, String], Any] =
    basicRequest
      .contentType("application/json")
      .headers(
        Map("GSQL-TIMEOUT" -> timeout.toString) ++
          (if (!authKey.isBlank) Map("Authorization" -> s"Bearer $authKey")
           else Map.empty[String, String])
      )

  private def postGSQL(payload: String): Unit = {
    val args = Array(
      "-ip",
      s"$hostname:$gsqlPort",
      "-u",
      username,
      "-p",
      password,
      payload
    )
    val codeControl = new CodeControl()
    try {
      logger.debug(s"Posting payload:\n$payload")
      codeControl.disableSystemExit()
      val output = executeGsqlClient(args)
      logger.trace(output)
    } catch {
      case e: Throwable => logger.error(s"Unable to post GSQL payload! Payload $payload", e)
    }
    codeControl.enableSystemExit()
  }

  private def executeGsqlClient(args: Array[String]): String = {
    val originalOut = System.out
    val originalErr = System.err
    val out         = new ByteArrayOutputStream()
    val err         = new ByteArrayOutputStream()
    System.setOut(new PrintStream(out))
    System.setErr(new PrintStream(err))
    com.tigergraph.v3_0_5.client.Driver.main(args)
    System.setOut(originalOut)
    System.setErr(originalErr)
    if (!err.toString().isBlank) throw new RuntimeException(err.toString())
    out.toString()
  }
}

/** Used to stop the JVM from passing System.exit commands.
  */
class CodeControl {
  def disableSystemExit(): Unit = {
    val securityManager: SecurityManager = new StopExitSecurityManager()
    System.setSecurityManager(securityManager)
  }

  def enableSystemExit(): Unit = {
    val mgr = System.getSecurityManager
    mgr match {
      case mgr: StopExitSecurityManager if mgr != null =>
        System.setSecurityManager(mgr.getPreviousMgr)
      case _ => System.setSecurityManager(null)
    }
  }

  class StopExitSecurityManager extends SecurityManager() {
    val _prevMgr: SecurityManager = System.getSecurityManager

    override def checkPermission(perm: Permission): Unit = {
      if (perm.isInstanceOf[RuntimePermission]) {
        if (perm.getName.startsWith("exitVM")) {
          throw new StopExitException(
            "Exit VM command by external library has been rejected - this is intended."
          )
        }
      }
      if (_prevMgr != null)
        _prevMgr.checkPermission(perm)
    }

    def getPreviousMgr: SecurityManager = _prevMgr
  }

  class StopExitException(msg: String) extends RuntimeException(msg)
}

object TigerGraphDriver {

  /** Default username for the TigerGraph server.
    */
  private val DEFAULT_USERNAME = "tigergraph"

  /** Default password for the TigerGraph server.
    */
  private val DEFAULT_PASSWORD = "tigergraph"

  /** Default hostname for the TigerGraph server.
    */
  private val DEFAULT_HOSTNAME = "localhost"

  /** Default port number a remote REST++ server.
    */
  private val DEFAULT_RESTPP_PORT = 9000

  /** Default port number a remote GSQL server.
    */
  private val DEFAULT_GSQL_PORT = 14240

  /** Default timeout for HTTP requests.
    */
  private val DEFAULT_TIMEOUT = 30 * 100

  /** Returns the corresponding TigerGraph type given a Scala type.
    */
  private def odbToTgType(propKey: String): String = {
    IDriver.getPropertyDefault(propKey) match {
      case _: Long    => "UINT"
      case _: Int     => "INT"
      case _: Boolean => "BOOL"
      case _          => "STRING"
    }
  }

  /** Determines if an edge type between two node types is valid.
    */
  def checkEdgeConstraint(from: String, to: String, edge: String): Boolean = {
    val fromCheck = from match {
      case MetaData.Label           => MetaData.Edges.Out.contains(edge)
      case File.Label               => File.Edges.Out.contains(edge)
      case Method.Label             => Method.Edges.Out.contains(edge)
      case MethodParameterIn.Label  => MethodParameterIn.Edges.Out.contains(edge)
      case MethodParameterOut.Label => MethodParameterOut.Edges.Out.contains(edge)
      case MethodReturn.Label       => MethodReturn.Edges.Out.contains(edge)
      case Modifier.Label           => Modifier.Edges.Out.contains(edge)
      case Type.Label               => Type.Edges.Out.contains(edge)
      case TypeDecl.Label           => TypeDecl.Edges.Out.contains(edge)
      case TypeParameter.Label      => TypeParameter.Edges.Out.contains(edge)
      case TypeArgument.Label       => TypeArgument.Edges.Out.contains(edge)
      case Member.Label             => Member.Edges.Out.contains(edge)
      case Namespace.Label          => Namespace.Edges.Out.contains(edge)
      case NamespaceBlock.Label     => NamespaceBlock.Edges.Out.contains(edge)
      case Literal.Label            => Literal.Edges.Out.contains(edge)
      case Call.Label               => Call.Edges.Out.contains(edge)
      case Local.Label              => Local.Edges.Out.contains(edge)
      case Identifier.Label         => Identifier.Edges.Out.contains(edge)
      case FieldIdentifier.Label    => FieldIdentifier.Edges.Out.contains(edge)
      case Return.Label             => Return.Edges.Out.contains(edge)
      case Block.Label              => Block.Edges.Out.contains(edge)
      case MethodRef.Label          => MethodRef.Edges.Out.contains(edge)
      case TypeRef.Label            => TypeRef.Edges.Out.contains(edge)
      case JumpTarget.Label         => JumpTarget.Edges.Out.contains(edge)
      case ControlStructure.Label   => ControlStructure.Edges.Out.contains(edge)
      case Unknown.Label            => Unknown.Edges.Out.contains(edge)
      case _                        => false
    }
    val toCheck = to match {
      case MetaData.Label           => MetaData.Edges.In.contains(edge)
      case File.Label               => File.Edges.In.contains(edge)
      case Method.Label             => Method.Edges.In.contains(edge)
      case MethodParameterIn.Label  => MethodParameterIn.Edges.In.contains(edge)
      case MethodParameterOut.Label => MethodParameterOut.Edges.In.contains(edge)
      case MethodReturn.Label       => MethodReturn.Edges.In.contains(edge)
      case Modifier.Label           => Modifier.Edges.In.contains(edge)
      case Type.Label               => Type.Edges.In.contains(edge)
      case TypeDecl.Label           => TypeDecl.Edges.In.contains(edge)
      case TypeParameter.Label      => TypeParameter.Edges.In.contains(edge)
      case TypeArgument.Label       => TypeArgument.Edges.In.contains(edge)
      case Member.Label             => Member.Edges.In.contains(edge)
      case Namespace.Label          => Namespace.Edges.In.contains(edge)
      case NamespaceBlock.Label     => NamespaceBlock.Edges.In.contains(edge)
      case Literal.Label            => Literal.Edges.In.contains(edge)
      case Call.Label               => Call.Edges.In.contains(edge)
      case Local.Label              => Local.Edges.In.contains(edge)
      case Identifier.Label         => Identifier.Edges.In.contains(edge)
      case FieldIdentifier.Label    => FieldIdentifier.Edges.In.contains(edge)
      case Return.Label             => Return.Edges.In.contains(edge)
      case Block.Label              => Block.Edges.In.contains(edge)
      case MethodRef.Label          => MethodRef.Edges.In.contains(edge)
      case TypeRef.Label            => TypeRef.Edges.In.contains(edge)
      case JumpTarget.Label         => JumpTarget.Edges.In.contains(edge)
      case ControlStructure.Label   => ControlStructure.Edges.In.contains(edge)
      case Unknown.Label            => Unknown.Edges.In.contains(edge)
      case _                        => false
    }

    fromCheck && toCheck
  }

  /** Edges as a schema string. Each edge is prepended with "_" to escape
    * reserved words.
    */
  private def EDGES: String = {
    EdgeTypes.ALL.asScala
      .flatMap { e: String =>
        NodeTypes.ALL.asScala.flatMap { src =>
          NodeTypes.ALL.asScala.flatMap { dst =>
            if (checkEdgeConstraint(src, dst, e)) Some((src, dst, e))
            else None
          }
        }
      }
      .groupBy { case (_, _, e) => e }
      .map { x: (String, Iterable[(String, String, String)]) =>
        val prefix = s"CREATE DIRECTED EDGE _${x._1}("
        val body   = x._2.map { case (src, dst, _) => s"FROM ${src}_, TO ${dst}_" }.mkString("|")
        s"$prefix$body)"
      }
      .mkString("\n")
  }

  /** Vertices as a schema string. Each vertex and property is appended or prepended with "_" to escape
    * reserved words.
    */
  private def VERTICES: String = {
    def propToTg(x: String) = {
      val default = IDriver.getPropertyDefault(x) match {
        case x: String  => s""""$x""""
        case x: Boolean => s""""$x""""
        case x          => x
      }
      s"_$x ${odbToTgType(x)} DEFAULT $default"
    }
    def vertexSchema(label: String, props: Set[String]) =
      s"CREATE VERTEX ${label}_ (PRIMARY_ID id UINT, ${props.map(propToTg).mkString(",")}) WITH primary_id_as_attribute=" + "\"true\""
    s"""
      |${vertexSchema(MetaData.Label, MetaData.PropertyNames.all)}
      |${vertexSchema(File.Label, File.PropertyNames.all)}
      |${vertexSchema(Method.Label, Method.PropertyNames.all)}
      |${vertexSchema(MethodParameterIn.Label, MethodParameterIn.PropertyNames.all)}
      |${vertexSchema(MethodParameterOut.Label, MethodParameterOut.PropertyNames.all)}
      |${vertexSchema(MethodReturn.Label, MethodReturn.PropertyNames.all)}
      |${vertexSchema(Modifier.Label, Modifier.PropertyNames.all)}
      |${vertexSchema(Type.Label, Type.PropertyNames.all)}
      |${vertexSchema(TypeDecl.Label, TypeDecl.PropertyNames.all)}
      |${vertexSchema(TypeParameter.Label, TypeParameter.PropertyNames.all)}
      |${vertexSchema(TypeArgument.Label, TypeArgument.PropertyNames.all)}
      |${vertexSchema(Member.Label, Member.PropertyNames.all)}
      |${vertexSchema(Namespace.Label, Namespace.PropertyNames.all)}
      |${vertexSchema(NamespaceBlock.Label, NamespaceBlock.PropertyNames.all)}
      |${vertexSchema(Literal.Label, Literal.PropertyNames.all)}
      |${vertexSchema(Call.Label, Call.PropertyNames.all)}
      |${vertexSchema(Local.Label, Local.PropertyNames.all)}
      |${vertexSchema(Identifier.Label, Identifier.PropertyNames.all)}
      |${vertexSchema(FieldIdentifier.Label, FieldIdentifier.PropertyNames.all)}
      |${vertexSchema(Return.Label, Return.PropertyNames.all)}
      |${vertexSchema(Block.Label, Block.PropertyNames.all)}
      |${vertexSchema(MethodRef.Label, MethodRef.PropertyNames.all)}
      |${vertexSchema(TypeRef.Label, TypeRef.PropertyNames.all)}
      |${vertexSchema(JumpTarget.Label, JumpTarget.PropertyNames.all)}
      |${vertexSchema(ControlStructure.Label, ControlStructure.PropertyNames.all)}
      |${vertexSchema(Unknown.Label, Unknown.PropertyNames.all)}
      |""".stripMargin
  }
}
