package com.github.plume.oss.domain

import io.circe.Json

/** The response from Neptune after initiating a database reset.
  * @param status the status of the system.
  * @param payload the token used to perform the database reset.
  */
final case class InitiateResetResponse(status: String, payload: TokenPayload)

/** The response from Neptune after performing a database reset.
  * @param status the status of the system.
  */
final case class PerformResetResponse(status: String)

/** The Neptune token used to correlate database operations.
  * @param token a string token used for database operations.
  */
final case class TokenPayload(token: String)

/** The response from Neptune when requesting the system status.
  * @param status the status of the system.
  * @param startTime set to the UTC time at which the current server process started.
  * @param dbEngineVersion set to the Neptune engine version running on your DB cluster.
  * @param role set to "reader" if the instance is a read-replica, or to "writer" if the instance is the primary
  *             instance.
  * @param gremlin contains information about the Gremlin query language available on your cluster. Specifically, it
  *                contains a version field that specifies the current TinkerPop version being used by the engine.
  */
final case class InstanceStatusResponse(
    status: String,
    startTime: String,
    dbEngineVersion: String,
    role: String,
    gremlin: GremlinVersion
)

/** Contains information about the Gremlin query language available on your cluster. Specifically, it contains a
  * version field that specifies the current TinkerPop version being used by the engine.
  * @param version Gremlin version number.
  */
final case class GremlinVersion(version: String)

/** The response specification for REST++ responses.
  */
final case class TigerGraphResponse(
    version: TigerGraphVersionInfo,
    error: Boolean,
    message: String,
    results: Seq[Json]
)

/** The version information response object.
  */
final case class TigerGraphVersionInfo(edition: String, api: String, schema: Int)
