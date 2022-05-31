package com.github.plume.oss.util

import io.joern.dataflowengineoss.queryengine.{EngineConfig, EngineContext, ResultTable}
import io.joern.dataflowengineoss.semanticsloader.{FlowSemantic, Semantics}

import java.nio.file.{Path, Paths}

/** Helper methods for setting up the data flow engine execution context.
  */
object DataFlowEngineUtil {

  /** Sets the context for the data-flow engine when performing
    * [[com.github.plume.oss.drivers.OverflowDbDriver.flowsBetween()]] queries.
    *
    * @param maxCallDepth the new method call depth.
    * @param methodSemantics the file containing method semantics for external methods.
    * @param initialCache an initializer for the data-flow cache containing pre-calculated paths.
    * @param shareCacheBetweenTasks enables the sharing of cache between data flow tasks.
    */
  def setDataflowContext(
      maxCallDepth: Int,
      methodSemantics: Semantics,
      initialCache: Option[ResultTable],
      shareCacheBetweenTasks: Boolean
  ): EngineContext = {
    EngineContext(
      methodSemantics,
      EngineConfig(maxCallDepth, initialCache, shareCacheBetweenTasks)
    )
  }
}

case class DataFlowCacheConfig(
    methodSemantics: Option[List[FlowSemantic]] = None,
    dataFlowCacheFile: Option[Path] = Some(Paths.get("dataFlowCache.cbor")),
    compressDataFlowCache: Boolean = true,
    maxCallDepth: Int = 6,
    maxCachedPaths: Int = 1_000,
    shareCacheBetweenTasks: Boolean = false
)
