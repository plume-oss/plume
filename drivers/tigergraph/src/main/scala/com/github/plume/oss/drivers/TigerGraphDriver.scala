package com.github.plume.oss.drivers

import com.github.plume.oss.drivers.TigerGraphDriver.*
import com.github.plume.oss.util.BatchedUpdateUtil.*
import io.circe
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Encoder, Json, JsonObject}
import io.shiftleft.codepropertygraph.generated.nodes.*
import io.shiftleft.codepropertygraph.generated.{EdgeTypes, NodeTypes, PropertyNames}
import org.slf4j.LoggerFactory
import overflowdb.BatchedUpdate.DiffOrBuilder
import overflowdb.{BatchedUpdate, DetachedNodeData}
import sttp.client3.*
import sttp.client3.circe.*
import sttp.model.{MediaType, Uri}

import java.io.IOException
import scala.concurrent.duration.{Duration, DurationInt}
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IteratorHasAsScala}
import scala.util.{Failure, Success, Try}

/** The driver used to communicate to a remote TigerGraph instance. One must build a schema on the first use of the
  * database.
  */
final class TigerGraphDriver(
  hostname: String = DEFAULT_HOSTNAME,
  restPpPort: Int = DEFAULT_RESTPP_PORT,
  gsqlPort: Int = DEFAULT_GSQL_PORT,
  username: String = DEFAULT_USERNAME,
  password: String = DEFAULT_PASSWORD,
  timeout: Int = DEFAULT_TIMEOUT,
  scheme: String = "http",
  txMax: Int = DEFAULT_TX_MAX,
  tgVersion: String = "3.9.3",
  authKey: String = ""
) extends IDriver
    with ISchemaSafeDriver {

  private val logger = LoggerFactory.getLogger(classOf[TigerGraphDriver])
  private val api    = Uri(scheme, hostname, restPpPort)
  private val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend(
    SttpBackendOptions.connectionTimeout(timeout.milliseconds)
  )

  implicit val payloadEncoder: Encoder[PayloadBody] =
    Encoder.forProduct2("vertices", "edges")(u => (u.vertices, u.edges))

  override def isConnected: Boolean = try {
    val response = quickRequest
      .get(Uri(scheme, hostname, restPpPort).addPath(Seq("endpoints")))
      .response(asJson[Map[String, JsonObject]])
      .send(backend)
    response.body match {
      case Left(_)  => false
      case Right(_) => true
    }
  } catch {
    case _: Exception => false
  }

  override def clear(): Unit = NODES_IN_SCHEMA.foreach { nodeType =>
    Try(delete(s"graph/cpg/delete_by_type/vertices/${nodeType}_"))
  }

  /** Does nothing as HTTP does not require closing
    */
  override def close(): Unit = {}

  override def exists(nodeId: Long): Boolean = {
    try {
      val response = get("query/cpg/v_exists", Map("id" -> nodeId))
      response.head.asObject match {
        case Some(map) => map.toMap("exists").asBoolean.get
        case None      => false
      }
    } catch {
      case _: IOException => false
    }
  }

  override def exists(srcId: Long, dstId: Long, edge: String): Boolean = {
    try {
      val response = get("query/cpg/e_exists", Map("src_id" -> srcId, "dst_id" -> dstId, "edge_label" -> s"_$edge"))
      response.head.asObject match {
        case Some(map) => map.toMap("exists").asBoolean.get
        case None      => false
      }
    } catch {
      case _: IOException => false
    }
  }

  private def jsonValue(value: Any): Option[Json] = {
    value match {
      case x: String  => Option(Json.fromString(x))
      case x: Int     => Option(Json.fromInt(x))
      case x: Boolean => Option(Json.fromBoolean(x))
      case xs: Seq[_] => Option(Json.fromString(xs.mkString(",")))
      case _          => None
    }
  }

  private def scalaValue(value: Json): Option[Any] = {
    if (value.isBoolean) {
      value.asBoolean
    } else if (value.isNumber) {
      value.asNumber.get.toInt
    } else if (value.isString) {
      value.asString
    } else {
      value.asObject
    }
  }

  private def nodePayload(id: Long, label: String, properties: Map[String, Any]): JsonObject = {
    val attributes = properties.flatMap { case (k, v) =>
      val vStr = v match {
        case xs: Seq[_]     => xs.mkString(",")
        case x if x == null => SchemaBuilder.getPropertyDefault(k)
        case x              => x
      }
      jsonValue(vStr) match {
        case Some(vson) => Some(s"_$k" -> JsonObject.fromMap(Map("value" -> vson)).asJson)
        case None       => None
      }
    }

    JsonObject.fromMap(
      Map(
        s"${label}_" -> JsonObject
          .fromMap(Map(id.toString -> JsonObject.fromMap(attributes).asJson))
          .asJson
      )
    )
  }

  private def edgePayload(srcId: Long, srcLabel: String, dstId: Long, dstLabel: String, edge: String): JsonObject = {
    val toIdMap =
      JsonObject.fromMap(Map(dstId.toString -> JsonObject.fromMap(Map.empty).asJson)).asJson
    val toMap     = JsonObject.fromMap(Map(s"${dstLabel}_" -> toIdMap)).asJson
    val edgeMap   = JsonObject.fromMap(Map(s"_$edge" -> toMap)).asJson
    val fromIdMap = JsonObject.fromMap(Map(srcId.toString -> edgeMap)).asJson
    JsonObject.fromMap(Map(s"${srcLabel}_" -> fromIdMap))
  }

  override def bulkTx(dg: DiffOrBuilder): Int = {
    // Node operations
    dg.iterator.asScala
      .collect { case x: DetachedNodeData => x }
      .grouped(txMax)
      .foreach(bulkCreateNode)
    dg.iterator.asScala
      .collect { case x: BatchedUpdate.SetNodeProperty => x }
      .grouped(txMax)
      .foreach(bulkNodeSetProperty)
    // Edge operations
    dg.iterator.asScala
      .collect { case x: BatchedUpdate.CreateEdge => x }
      .grouped(txMax)
      .foreach(bulkCreateEdge)

    dg.size()
  }

  private def bulkCreateNode(ops: Seq[DetachedNodeData]): Unit = {
    val payload = ops
      .flatMap { c =>
        val nodeId = c.pID
        c.setRefOrId(nodeId)
        Some(nodePayload(idFromNodeData(c), c.label(), propertiesFromNodeData(c).toMap))
      }
      .reduce { (a: JsonObject, b: JsonObject) => a.deepMerge(b) }
    post("graph/cpg", PayloadBody(vertices = payload))
  }

  private def bulkNodeSetProperty(ops: Seq[BatchedUpdate.SetNodeProperty]): Unit = {
    val payload = ops
      .map(c => (c.label, c.value, c.node))
      .flatMap {
        case (key, value, n: StoredNode) =>
          jsonValue(value) match {
            case Some(v) =>
              val kv =
                JsonObject.fromMap(Map(s"_$key" -> JsonObject.fromMap(Map("value" -> v)).asJson))
              val body = JsonObject.fromMap(Map(n.id().toString -> kv.asJson))
              Some(JsonObject.fromMap(Map(s"${n.label}_" -> body.asJson)))
            case None => None
          }
        case _ => None
      }
      .reduce { (a: JsonObject, b: JsonObject) => a.deepMerge(b) }
    post("graph/cpg", PayloadBody(vertices = payload))
  }

  private def bulkCreateEdge(ops: Seq[BatchedUpdate.CreateEdge]): Unit = {
    val payload = ops
      .flatMap(c =>
        Some(
          edgePayload(
            idFromNodeData(c.src),
            labelFromNodeData(c.src),
            idFromNodeData(c.dst),
            labelFromNodeData(c.dst),
            c.label
          )
        )
      )
      .reduce { (a: JsonObject, b: JsonObject) => a.deepMerge(b) }
    post("graph/cpg", PayloadBody(edges = payload))
  }

  override def removeSourceFiles(filenames: String*): Unit =
    get("query/cpg/delete_source_file", filenames.map(("filenames", _)))

  override def propertyFromNodes(nodeType: String, keys: String*): List[Map[String, Any]] = {
    keys
      .map { k =>
        val params = Map("node_type" -> s"${nodeType}_", "property" -> s"_$k")
        SchemaBuilder.getPropertyDefault(k) match {
          case _: Boolean => k -> get("query/cpg/b_property_from_nodes", params)
          case _: Int     => k -> get("query/cpg/i_property_from_nodes", params)
          case _          => k -> get("query/cpg/s_property_from_nodes", params)
        }
      }
      .flatMap { case (k, res) =>
        res.head.asObject.get
          .toMap("properties")
          .asArray
          .get
          .map(_.asObject.get.toMap)
          .map { m =>
            Map("id" -> m("id").asNumber.get.toLong.get, k -> scalaValue(m("property")).get)
          }
      }
      .groupBy { m => m("id").asInstanceOf[Long] }
      .map { case (_: Long, ps: Seq[Map[String, Any]]) => ps.reduce { (a, b) => a ++ b } }
      .map(deserializeLists)
      .toList
  }

  override def buildSchema(): Unit = postGSQL(buildSchemaPayload())

  override def buildSchemaPayload(): String = {
    s"""
      |DROP ALL
      |$VERTICES
      |$EDGES
      |$WILDCARD_EDGES
      |CREATE GRAPH cpg(*)
      |$QUERIES
      |INSTALL QUERY ALL
      |""".stripMargin
  }

  private def request(): RequestT[Empty, Either[String, String], Any] =
    basicRequest
      .contentType(MediaType.ApplicationJson)
      .headers(
        Map("GSQL-TIMEOUT" -> timeout.toString) ++
          (if (!authKey.isBlank) Map("Authorization" -> s"Bearer $authKey")
           else Map.empty[String, String])
      )

  private def buildUri(endpoint: String, params: Map[String, Any] = Map.empty[String, Any]) = {
    api
      .copy()
      .addParams(params.map(x => x._1 -> x._2.toString))
      .addPath(endpoint.split("/"))
  }

  private def unboxResponse(
    response: Identity[Response[Either[ResponseException[String, circe.Error], TigerGraphResponse]]]
  ) = {
    response.body match {
      case Left(e: ResponseException[_, _]) => throw unpackUnboxingException(e)
      case Right(body) =>
        if (body.error) {
          throw new IOException(
            s"Could not complete delete request due to status code ${response.code} at ${response.request.uri}"
          )
        } else {
          body.results
        }
    }
  }

  private def unpackUnboxingException(e: ResponseException[String, circe.Error]): Exception = {
    e.printStackTrace()
    e match {
      case HttpError(body, statusCode) =>
        logger.error(s"HTTP Error $statusCode: $body")
        e
      case DeserializationException(body, error) =>
        logger.error(s"Failed to deserialize response: $body. $error")
        e
    }
  }

  private def get(endpoint: String, params: Seq[(String, String)]): Seq[Json] = {
    val uri = buildUri(endpoint).addParams(params: _*)
    Try(
      request()
        .get(uri)
        .readTimeout(Duration.Inf)
        .response(asJson[TigerGraphResponse])
        .send(backend)
    ) match {
      case Failure(e)        => logger.error(s"HTTP GET Request error.", e); throw e
      case Success(response) => unboxResponse(response)
    }
  }

  private def get(endpoint: String, params: Map[String, Any] = Map.empty[String, Any]): Seq[Json] = {
    val uri = buildUri(endpoint, params)
    Try(
      request()
        .get(uri)
        .readTimeout(Duration.Inf)
        .response(asJson[TigerGraphResponse])
        .send(backend)
    ) match {
      case Failure(e)        => logger.error(s"HTTP GET Request error.", e); throw e
      case Success(response) => unboxResponse(response)
    }
  }

  private def post(endpoint: String, payload: PayloadBody): Seq[Json] = {
    val uri = buildUri(endpoint)
    val response = request()
      .post(uri)
      .body(payload)
      .response(asJson[TigerGraphResponse])
      .send(backend)
    unboxResponse(response)
  }

  private def delete(endpoint: String, params: Map[String, Any] = Map.empty[String, Any]): Seq[Json] = {
    val uri = buildUri(endpoint, params)
    val response = request()
      .delete(uri)
      .response(asJson[TigerGraphResponse])
      .send(backend)
    unboxResponse(response)
  }

  private def postGSQL(payload: String): Unit = {
    Try(sys.env("GSQL_HOME")) match {
      case Failure(_) =>
        throw new RuntimeException("""
        |Environment variable 'GSQL_HOME' not found on the OS. Please install the gsql_client.jar
        |from https://docs.tigergraph.com/tigergraph-server/current/gsql-shell/using-a-remote-gsql-client and set the
        |path to the JAR file as GSQL_HOME.
        |""".stripMargin)
      case Success(gsqlPath) =>
        val args = Seq("-ip", s"$hostname:$gsqlPort", "-u", username, "-p", password, payload)
        logger.debug(s"Posting payload:\n$payload")
        executeGsqlClient(gsqlPath, args)
    }
  }

  private def executeGsqlClient(gsqlPath: String, args: Seq[String]): Unit = {
    import sys.process.*

    val allLogs = new StringBuilder()
    val processLogger =
      ProcessLogger.apply(
        (s: String) => { logger.info(s); allLogs.append(s) },
        (s: String) => { logger.error(s); allLogs.append(s) }
      )
    Try(sys.env("JAVA_HOME")) match {
      case Failure(_) =>
        throw new RuntimeException("Environment variable 'JAVA_HOME' not found on the OS.")
      case Success(javaLocation) =>
        logger.info(s"Using the Java runtime found at $javaLocation to run the GSQL client.")
    }
    val command: Seq[String] =
      Seq("java", s"-DGSQL_CLIENT_VERSION=v${tgVersion.replace('.', '_')}", "-jar", gsqlPath) ++ args
    // Exclude password and payload from the header
    val commandHeader = (command.dropRight(2) ++ Seq("<omitted>")).mkString(" ")
    logger.info(s"Executing GSQL client command with header: $commandHeader")
    val status = Process(command) !< processLogger

    if (status != 0) {
      System.err.println(s"Exception encountered, logs: $allLogs")
      throw new RuntimeException(s"Failure in posting GSQL payload. Error code $status. See logs for more details.")
    } else {
      logger.info("Successfully posted GSQL request")
    }
  }
}

/** The payload body for upserting graph data.
  */
final case class PayloadBody(vertices: JsonObject = JsonObject.empty, edges: JsonObject = JsonObject.empty)

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
  private val DEFAULT_TIMEOUT = 30 * 1000

  /** Default maximum number of transactions to bundle in a single transaction
    */
  private val DEFAULT_TX_MAX = 25

  /** Returns the corresponding TigerGraph type given a Scala type.
    */
  private def odbToTgType(propKey: String): String = {
    SchemaBuilder.getPropertyDefault(propKey) match {
      case _: Long    => "UINT"
      case _: Int     => "INT"
      case _: Boolean => "BOOL"
      case _          => "STRING"
    }
  }

  /** Edges as a schema string. Each edge is prepended with "_" to escape reserved words.
    */
  private def EDGES: String = {
    EdgeTypes.ALL.asScala
      .flatMap { e =>
        NodeTypes.ALL.asScala.flatMap { src =>
          NodeTypes.ALL.asScala.flatMap { dst =>
            if (SchemaBuilder.checkEdgeConstraint(src, dst, e)) Some((src, dst, e))
            else None
          }
        }
      }
      .groupBy { case (_, _, e) => e }
      .filterNot { x => SchemaBuilder.WILDCARD_EDGE_LABELS.contains(x._1) }
      .map { case (label, xs) =>
        val prefix = s"CREATE DIRECTED EDGE _$label("
        val body   = xs.map { case (src, dst, _) => s"FROM ${src}_, TO ${dst}_" }.mkString("|")
        s"$prefix$body)"
      }
      .mkString("\n")
  }

  /** Creates the schema string of all edges that should be treated as wildcards.
    */
  private def WILDCARD_EDGES: String = {
    SchemaBuilder.WILDCARD_EDGE_LABELS.map(x => s"CREATE DIRECTED EDGE _$x(FROM *, TO *)").mkString("\n")
  }

  /** Vertices as a schema string. Each vertex and property is appended or prepended with "_" to escape reserved words.
    */
  private def VERTICES: String = {
    def propToTg(x: String) = {
      val default = SchemaBuilder.getPropertyDefault(x) match {
        case x: String  => "\"" + x + "\""
        case x: Boolean => "\"" + x + "\""
        case _: Seq[_]  => "\"" + "" + "\""
        case x          => x
      }
      s"_$x ${odbToTgType(x)} DEFAULT $default"
    }
    def vertexSchema(label: String, props: Set[String]): String =
      s"CREATE VERTEX ${label}_ (PRIMARY_ID id UINT, ${props.map(propToTg).mkString(",")}) WITH primary_id_as_attribute=" + "\"true\""
    s"""
      |${SchemaBuilder.NodeToProperties.map(vertexSchema).mkString("\n")}
      |""".stripMargin
  }

  private def QUERIES: String = {
    (Array(
      """
        |CREATE QUERY show_all() FOR GRAPH cpg {
        |  SetAccum<EDGE> @@edges;
        |  seed = {ANY};
        |  result = SELECT s
        |           FROM seed:s -(:e)-> :t
        |           ACCUM @@edges += e;
        |  PRINT seed as vertices;
        |  PRINT @@edges as edges;
        |}
        |""".stripMargin,
      """
        |CREATE QUERY e_exists(UINT src_id, UINT dst_id, STRING edge_label) FOR GRAPH cpg {
        |  seed = {ANY};
        |  temp = SELECT tgt
        |          FROM seed:src -(:e)-> :tgt
        |          WHERE src.id == src_id
        |            AND tgt.id == dst_id
        |            AND e.type == edge_label;
        |  PRINT (temp.size() > 0) as exists;
        |}
        """.stripMargin,
      """
        |CREATE QUERY v_exists(UINT id) FOR GRAPH cpg {
        |  seed = {ANY};
        |  temp = SELECT n
        |        FROM seed:n
        |        WHERE n.id == id;
        |  PRINT (temp.size() > 0) as exists;
        |}
        |""".stripMargin,
      """
        |CREATE QUERY v_delete(SET<UINT> ids) FOR GRAPH cpg {
        |  seed = {ANY};
        |  DELETE src
        |  FROM seed:src
        |  WHERE src.id IN ids;
        |}
        |""".stripMargin,
      """
        |CREATE QUERY id_interval(INT lower, INT upper) FOR GRAPH cpg {
        |  SetAccum<INT> @@ids;
        |  seed = {ANY};
        |  temp = SELECT src
        |      FROM seed:src
        |      WHERE src.id >= lower AND src.id <= upper
        |      ACCUM @@ids += src.id;
        |  PRINT @@ids as ids;
        |}
        |""".stripMargin,
      """
        |CREATE QUERY delete_source_file(SET<STRING> filenames) FOR GRAPH cpg SYNTAX v2 {
        |  fs = {FILE_.*};
        |  fvs = SELECT f
        |        FROM fs:f
        |        WHERE f._NAME IN filenames;
        |  tds = SELECT td
        |        FROM fvs -(<_SOURCE_FILE)- TYPE_DECL_:td;
        |  ts = SELECT t
        |       FROM tds -(<_REF)- TYPE_:t;
        |  nbs = SELECT n
        |        FROM fvs - (<_SOURCE_FILE)- NAMESPACE_BLOCK_:n;
        |
        |  childVs = SELECT t
        |            FROM nbs:s -((_AST>|_CONDITION>)*) - :t;
        |
        |  nsToDelete = fvs UNION tds UNION ts UNION childVs;
        |  DELETE v FROM nsToDelete:v;
        |}
        |""".stripMargin,
      """
        |CREATE QUERY static_call_linker() FOR GRAPH cpg {
        |  TYPEDEF TUPLE<FULL_NAME STRING, method VERTEX<METHOD_>> M_TUP;
        |  SetAccum<M_TUP> @@methods;
        |  calls = {CALL_.*};
        |  methods = {METHOD_.*};
        |
        |  ms = SELECT m
        |       FROM methods:m
        |       ACCUM @@methods += M_TUP(m._FULL_NAME, m);
        |
        |  FOREACH m in @@methods DO
        |    cs = SELECT c
        |         FROM calls:c
        |         WHERE c._DISPATCH_TYPE == "STATIC_DISPATCH"
        |          AND m.FULL_NAME == c._METHOD_FULL_NAME
        |         ACCUM INSERT INTO _CALL VALUES(c, m.method);
        |  END;
        |}
        |""".stripMargin
    ) ++ Array(
      (EdgeTypes.REF, PropertyNames.NAME),
      (EdgeTypes.REF, PropertyNames.FULL_NAME),
      (EdgeTypes.EVAL_TYPE, PropertyNames.TYPE_FULL_NAME),
      (EdgeTypes.REF, PropertyNames.METHOD_FULL_NAME),
      (EdgeTypes.INHERITS_FROM, PropertyNames.INHERITS_FROM_TYPE_FULL_NAME),
      (EdgeTypes.ALIAS_OF, PropertyNames.ALIAS_TYPE_FULL_NAME)
    ).map { case (e: String, p: String) =>
      s"""
        |CREATE QUERY link_ast_${e.toLowerCase}_${p.toLowerCase}(SET<STRING> src_labels, STRING dst_value, VERTEX dst) FOR GRAPH cpg {
        |  seed = {ANY};
        |  temp = SELECT src
        |         FROM seed:src
        |         WHERE src.type IN src_labels AND src._$p LIKE dst_value
        |         ACCUM INSERT INTO _$e VALUES (src, dst);
        |}
        |""".stripMargin
    } ++
      Array("STRING", "INT", "BOOL").map(x => s"""
        |CREATE QUERY ${x(0).toLower}_property_from_nodes(STRING node_type, STRING property) FOR GRAPH cpg {
        |  TYPEDEF TUPLE<id UINT, property $x> RES;
        |  SetAccum<RES> @@result;
        |  seed = {ANY};
        |  temp =  SELECT src
        |          FROM seed:src
        |          WHERE src.type == node_type
        |          ACCUM @@result += RES(src.id, src.getAttr(property, "$x"));
        |  PRINT @@result as properties;
        |}
        |""".stripMargin)).mkString
  }
}

/** The response specification for REST++ responses.
  */
final case class TigerGraphResponse(version: TigerGraphVersionInfo, error: Boolean, message: String, results: Seq[Json])

/** The version information response object.
  */
final case class TigerGraphVersionInfo(edition: String, api: String, schema: Int)
