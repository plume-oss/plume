# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased]

### Fixed

- Fixed instances where `.fieldRef.getField` would return a `null` and crash method body parsing.

## [1.0.13] - 2022-02-22

### Added

- Method parameters now have correct evaluation strategies.

### Fixed

- Fixed performance issues in Gremlin drivers related to not re-using traversal objects.
- Fixed instance in methods where `this` parameter was not passed through on dynamic calls.
- Fixed performance issues in Neo4j's driver by using more parameterized and re-usable queries.

## [1.0.12] - 2022-02-16

### Fixed

- Generate `<operator>.assignment` call node's `code` property from child argument `code`.

## [1.0.11] - 2022-02-15

### Fixed

- `TigerGraphDriver` default transaction limit was 3 instead of 30 seconds.
- AST linking in `TigerGraphDriver` did not escape `[]` or `_` but now does.
- Diversified error handling on exceptions on `TigerGraphDriver` HTTP requests.

## [1.0.10] - 2022-02-15

### Added

- `Jimple2Cpg::createCpg` can now enable an experimental "Soot only" build.

### Fixed

- `PlumeStatistics::reset` now actually sets all values for keys to `0L` instead of just clearing.

## [1.0.9] - 2022-02-14

### Changed

- `PlumeDynamicCallLinker` more generous with trying static call linking as a fallback
  before reporting an issue.
- Now wrapped `OverflowDb::clear` in a `Try` to prevent a `unable to calculate occurrenceCount` runtime
  exception.

## [1.0.8] - 2022-02-11

### Changed

- Created `util` package to contain `HashUtil` and `ProgramHandlingUtil`
- All input files are unpacked to a temporary directory.

### Fixed

- Methods are checked to be concrete before retrieving method body.

## [1.0.7] - 2022-02-10

### Added

- Interfaces are now recognized and treated as types with implementation represented by `INHERITS_FROM`.

## [1.0.6] - 2022-02-09

### Added

- `PlumeStatistics` now captures library performance.

### Changed

- `OverflowDbDriver(dataflowCachePath)` property is now `Option[Path]`.
- If `dataFlowCachePath` is `None` then data-flow results are not saved.
- Moved HTTP response case classes to `domain.HttpResponse`.

## [1.0.5] - 2022-02-07

### Added

- Data-flow paths are saved to a GZIP compressed JSON and are re-used on future runs. 
  Only available on `OverflowDbDriver`.

### Fixed

- Unchanged methods no longer have REACHABLE_BY edges regenerated/duplicated

## [1.0.4] - 2022-01-25

### Added

- Support for multi-array creation added
- Array tests derived from JavaSrc2Cpg included

### Fixed

- Fixed access path issue where array index accesses were reported to be invalid ASTs. This was just a change in AST 
  children's `order` from `(0, 1)` to `(1, 2)`
- Fixed bug where if a single file was specified then all files in the directory were loaded

### Changed

- Updated frontend to leverage `Call(<operator>.arrayInitializer)` instead of `Unknown(new)` vertices

## [1.0.3] - 2022-01-24

### Fixed

- Issue where if a single file was given, all surrounding files are checked to be included too
- Warnings related to matching generics susceptible to type erasure issues
- Performance and anti-patterns reported by DeepSource

## [1.0.2] - 2022-01-07

### Fixed

- `GremlinDriver` now handles defaults for all calls to `by` steps

## [1.0.1] - 2022-01-06

### Fixed

- `GremlinDriver` now handles nodes that do not include properties specified
  under `GremlinDriver::propertyFromNodes`

## [1.0.0] - 2022-01-04

### Changed

- Whole project migrated to Scala
- Every transaction as far as possible is a bulk transaction
- Processing follows closely to layers used in other Joern frontends
- Package structure changed from io.github to com.github

### Removed

- JanusGraph support

## [0.6.3] - 2021-12-10

### Fixed

- Removed unnecessary use of Log4j2

## [0.6.2] - 2021-10-07

### Changed

- Modified class loading to handle exceptions when CG methods cannot be extracted
- Extractor main process wrapped in try-final to ensure resource release

## [0.6.1] - 2021-09-22

### Added

- Soot only configuration for the metrics

### Changed

- Upgraded CPG to latest version before IDs were removed again
- Removed unused methods in ODB `Traversals`

## [0.6.0] - 2021-09-14

### Changed

- Plume going into maintenance mode

### Fixed

- `TableSwitchStmt` jump targets now have the correct order

## [0.5.14] - 2021-09-08

### Added

- `Extractor::project` now generated overloaded methods for Java

### Changed

- OverflowDB graphs now generated from `io.shiftleft.codepropertygraph.generated.Cpg`

## [0.5.13] - 2021-09-07

### Changed

- Upgrade ShiftLeft dependencies to 1.3.314
- Upgrade Gradle to 7.2  
- Removed `Binding` vertices
- Can now handle new type arguments API of domain classes
- `Extractor::project` now takes an optional boolean to disable reaching defs calculation
- `Extractor::projectReachingDefs` now calculates reaching defs separately
- Removed `IDriver::getProgramStructure` as it's not used by the core extractor
- `VertexMapper` now handles new default property system 

## [0.5.12] - 2021-07-30

### Changed

- SPARK is now the default call graph as it is more precise and pays off later when performing data-flow analysis
- All methods are now accepted as entry-points, no need for parachute code to catch the case where no call edges generated by Soot

## [0.5.11] - 2021-05-18

### Changed

- Upgraded `codepropertygraph` version to 1.3.151 and made respective changes.
- Re-added `TYPE_FULL_NAME` to `Call` vertices.

## [0.5.10] - 2021-04-27

### Fixed

- Corrected Identifier's code for Static Field Access

## [0.5.9] - 2021-04-27

### Changed

- Downgraded `codepropertygraph` version to 1.3.120.

### Fixed

- Fixed issue where JAR files were only being identified by their suffix. They are now
  checked for being zip files first which will include WARs.

## [0.5.8] - 2021-04-26

### Added

- Improved logging for `NeptuneDriver`.

### Fixed

- Bug where cluster builder details cause empty re-connections.
- The `id` was not being set from the deserialized IDs in `NeptuneDriver`.

## [0.5.7] - 2021-04-26

### Added

- `NeptuneDriver::idStorageLocation` specifies a storage location where ID mapper values can be written.

## [0.5.6] - 2021-04-23

### Changed

- Increased wait time after `clearGraph` call in `NeptuneDriver`

## [0.5.6] - 2021-04-22

### Fixed

- External methods getting marked for rebuild on disconnected updates.

## [0.5.5] - 2021-04-22

### Fixed

- External classes getting marked for rebuild on disconnected updates.

### Changed

- `NeptuneDriver::clearGraph` now uses the HTTP system database reset if the graph has over
  10 000 vertices.

## [0.5.4] - 2021-04-21

### Added

- Added new timer measurements `CONNECT_DESERIALIZE` and `DISCONNECT_SERIALIZE`.

### Changed

- `DataFlowPass` now converts `DiffGraph`s into `DeltaGraphs` in order to use `bulkTransaction`s.
- `DataFlowPass` now shows progress bar.
- Separated Extractor and Driver measurements under `PlumeTimer`.

## [0.5.3] - 2021-04-19

### Changed

- `TigerGraphDriver` now uses a TigerGraph v3.0 feature that allows edges to be defined between
  different vertex types. This means that now the CPG schema can be properly defined with unique
  vertex names.
- If a property is unused it is stripped from being added.  

## [0.5.2] - 2021-04-19

### Fixed

- `PlumeKeyProvider` was getting stuck on the `currentMax` variable - this is now fixed.
- `Neo4jDriver`'s `bulkTransction` was too tightly coupled on the vertex and edge add which lead
  to bugs. This is now separated and the bug is no longer present.

### Changed

- `Neo4jDriver` bulk transactions are now chunked in that they insert by chunks of 50.

## [0.5.1] - 2021-04-18

### Fixed

- `PlumeKeyProvider` was providing duplicates - this is now fixed.

### Changed

- Duplicates are now handled the same in `TigerGraphDriver` as the rest
  of the drivers.

## [0.5.0] - 2021-04-15

### Changed

- Latest SCPG schema applied with deprecated properties removed.
- Removed deprecated `DeltaGraph.apply`

## [0.4.7] - 2021-04-15

### Fixed

- `GremlinDriver` bulk transactions properly implemented now.
- Duplicate edges in `bulkTransaction` filtered out

### Changed

- Duplicates in bulk transactions of the `GremlinDriver` are more
  thoroughly removed.
- `NeptuneDriver` now clears the graph in chunks to avoid timing out
  on larger graphs.
- Grouped field construction into chunks and execute chunks in bulk
  transactions.  

## [0.4.6] - 2021-04-14

### Fixed

- `OverflowDbDriver`'s existence checking now also makes sure that the ID returned
  matches the ID given.
  
### Changed

- Indicating in the logging which number of each member being reported belongs to the
  application or an external library.
- Making a progress bar is now done via `ProgressBarUtil`.

## [0.4.5] - 2021-04-13

### Added

- `TigerGraphDriver` now has timeout as a configurable parameter.

### Fixed

- Neptune driver by mapping `Long` IDs to Neptune's native `String` IDs

### Changed

- Removed `GremlinOverriddenIdDriver` as it is no longer used.

## [0.4.4] - 2021-04-12

### Added

- `AST` and `CONTAINS` edges for external method stubs

### Fixed

- Fixed a bug where `Call` vertices which were removed were being recreated under `CGPass`
- Associated `NamespaceBlock` also removed from cache during class removal.

### Changed

- New fields are not checked for rebuild and are immediately added. Only updated class fields are 
  now checked.
- Removed driver classes that were deprecated and due for removal
- Removed unused constants

## [0.4.3] - 2021-04-11

### Fixed

- Uncaught exceptions are sometimes thrown when looking for all methods that the program references.
  These are now caught appropriately.

## [0.4.2] - 2021-04-09

### Added

- Artifact hash under `MetaData`. If artifact has no change then project will end early.

### Fixed

- In instances where classes are removed, their respective cached data is removed now too.

## [0.4.1] - 2021-04-09

### Added

- Method bodies are now hashed and stored on the `Method` node.
- Finer updates on:
  - class modifier level, 
  - field type, value, modifier level, and
  - method level
- The latest copyright on all class headers.

### Fixed

- `order` of methods in the `MethodStubPass`.
- `DynamicInvoke` bootstrap arguments are now projected.
- External methods referenced in calls are now added too.

### Changed

- Separated hash functions to into a new util class called `HashUtil`.
- Plume now expects the whole artifact to be loaded in order to detect class removals.

## [0.4.0] - 2021-04-01

### Added

- `ArrayRef` now gets projected as an `Operators.indexAccess` call with index and base identifier as the arguments.
- `InstanceOfExpr` now gets projected as an `Operators.instanceOf` call.
- `LengthExpr` now gets projected as an `Operators.lengthOf` call. This is a custom operator
- `MonitorStmt` now gets projected as an `Unknown` vertex.
- `NegExpr` now gets projected as an `Operators.minus` call.

### Fixed

- Crashing passes from making the program hang. Exceptions are caught, logged, and the build is saved as far as it got.

### Changed

- `ThrowStmt` is now an `Unknown` vertex where control flow ends at.
- `NewArrayExpr` is now an `Unknown` vertex.

## [0.3.10] - 2021-03-29

### Added

- `IDriver.bulkTransaction` to replace `DeltaGraph.apply` and make database specific bulk changes.
- `IdentityStmt` is now handled as part of the `LOCAL` and `IDENTIFIER` cycle
- `IdentityRef` is now handled under `projectOp`
- `ThrowStmt` is now handled as a special kind of `Return`

### Fixed

- External method <-`AST`- External type is now fixed.
- `EVAL_TYPE` links for `MethodReturn` and `BlockVertex` on the method stubs.
- `CacheOptions.cacheSize` is mutable via setters now.
- `DataFlowPass` now gets method head along with method body so the passes no longer throw exceptions.
- `parseBinopExpr` had some incorrect mappings which are now fixed.

### Changed

- `PlumeTimer` is simplified and now only uses `measure` function.
- Disabled cache2k from collecting its own statistics.
- Early stopping enabled when no classes needed to update is detected.
- Feedback regarding files to updated now moved from `INFO` to `DEBUG` logging.
- Marked `DeltaGraph.apply` as deprecated.
- `DeltaGraph::toOverflowDb` now only writes to an existing OverflowDB instance.
- Increased `CacheOptions.cacheSize` and the cache is now partitioned among the 4 caches
  based on average allocation from the benchmarks. Cache expiry is now removed as an option.
- `GotoStmt` is now added as a `CONTROL_STRUCTURE_VERTEX` with `JUMP_TARGET`s removed.
- CFG now connects nodes within each expression and follows the stack pointer like Joern/Ocular
- `StaticFieldRef` has moved from using `TypeRef` to `Identifier` otherwise data flow passes through errors.

## [0.3.9] - 2021-03-23

### Fixed

- Progress bar causing call graph pass to freeze on large graphs. This has been removed.
- Resource clearing was accidentally commented out in 0.3.8 - this has been addressed.

## [0.3.8] - 2021-03-23

### Added

- Progress bar when logging level is `>= Level.INFO` for method related operations
- Added cache2k to handle caching
- `CacheMetrics` to track hits and misses
- `METHOD_PARAMETER_IN` -`PARAMETER_LINK`-> `METHOD_PARAMETER_OUT` edge was included

### Changed

- Improved the node caching and centralized `tryGet` and `getOrMake`-style operations to `DriverCache.kt`
- Separated the cache and storage into `storage._Cache` classes and `storage.PlumeStorage`

### Fixed

- Method/Local/MethodParameterIn have been created more closely to Ocular's output.

## [0.3.7] - 2021-03-19

### Fixed

- `TigerGraphDriver` bug where empty strings for intentional properties would be unintentionally excluded.
- `Member.name` and `FieldIdentifier.code` properly handled
- Fixed temp dir resolution issue on macOS and Windows

## [0.3.6] - 2021-03-18

### Added

- `CONTAINS` edges are generated for `METHOD` to body vertices.
- `ListMapper` to process Scala lists to a serialized string and back. More formally processing Scala lists to and from
  OverflowDB node objects.
- Handle inheritance edges i.e. `TYPE_DECL -INHERITS_FROM-> TYPE`

### Changed

- `BaseCpgPass` now uses a local cache for method body nodes instead of relying solely on `GlobalCache`
- `SCPGPass` now known as `DataFlowPass` as all passes now come from `dataflowengineoss`.
- Added `PROGRAM_STRUCTURE` to timer keys.

## [0.3.5] - 2021-03-17

### Added

- `IDriver::getVerticesOfType` to aid in caching from existing database vertices.
- External methods signatures are parsed to figure out their method parameters.
- `MethodStubPass` and `BaseCPGPass` now includes `METHOD_PARAM_IN` and `METHOD_PARAM_OUT` and connects them to their
  type.
- Field accesses are now constructed as a `Call` vertex.
- Plume now has a new logo and branding.
- Better logging for loaded files.

### Changed

- Many of the `nodeCache` uses in `IProgramPass` passes were converted to using the `GlobalCache` instead.
- `MethodStubPass` now runs in parallel if possible.

## [0.3.4] - 2021-03-15

### Changed

- Upped the default chunk size
- `DeltaGraph::toOverflowDb` can now take in an optional `overflowdb.Graph` object to write to

### Fixed

- Memory leak where thread pools weren't getting shutdown

## [0.3.3] - 2021-03-15

### Added

- `DeltaGraph` as a `NewNodeBuilder` variant of ShiftLeft's `DiffGraph`.
- `BaseCpgPass` which is a combination of the `ASTPass`, `CFGPass`, and `PDGPass` and returns a `DeltaGraph` instead of
  directly apply changes to the driver.
- `methodBodies` was added to `GlobalCache` to save on database requests when moving to `SCPGPass` after `BaseCpgPass`
- Chunk size can now be configured via `ExtractorOptions::methodChunkSize`

### Changed

- Replaced `ASTPass`, `CFGPass`, and `PDGPass` with `BaseCpgPass`.
- Spawns a thread pool to run base CPG building in parallel and apply `DeltaGraph`s in serial.
- SCPG flows are only run on new/updated method bodies since the analysis is independent of other methods.

## [0.3.2] - 2021-03-12

### Added

- Types for global primitives
- Return types are now added to all types built in the CPG

### Changed

- Moved the maps in `Extractor` to a dedicated `GlobalCache` object that uses `ConcurrentHashMap`s.
- SCPG pass now concurrently pulls all methods and merges it into an input graph. This code has been moved to
  `passes.SCPGPass.kt`
- External method stubs have call-to-returns generated i.e. (METHOD)-CFG->(RETURN)-CFG->(METHOD_RETURN)

## [0.3.1] - 2021-03-11

### Added

- Better `INFO` threshold logging within `Extractor::project`.

### Changed

- Combined `Extractor::project` and `Extractor::postProject` into `project`.
- Deprecated `getProgramTypeData`
- Changed `UNIT_GRAPH_BUILDING` to `SOOT` and added the time taken on loading files into Soot, calling FastHierarchy,
  and using Soot's call graph.

## [0.3.0] - 2021-03-09

### Added

- Method pass `MethodStubPass`
- Structure pass `ExternalTypePass`, `FileAndPackagePass`, `MarkForRebuildPass`, and `TypePass`
- Type pass `GlobalTypePass`
- Added `getVerticesByProperty` and `getPropertyFromVertices` to `IDriver`

### Changed

- Graph builders are now known as "passes" to conform to how SCPG builds graphs. Each has an interface
  under `IGraphPass`.
- `graph/[AST|CFG|PDG|CallGraph]Builder` to `passes/graph/[AST|CFG|PDG|CallGraph]Pass`
- Deprecated `getMethodNames`
- Added timer probes regarding database closer to database methods

### Fixed

- Duplication of files, types, namespace vertices on updates

## [0.2.8] - 2021-03-05

### Added

- `ContainsEdgePass` added before `ReachingDefPass`
- `PlumeTimer` to measure various intervals of the projection process
- Added a filter step before `constructStructure` call in `Extractor::project` as not to duplicate types

### Fixed

- Fixed `PlumeKeyProvider` infinite loop and added proper tests for `getNewId`
- Added a check in the setter for `keyPoolSize` to not allow anything less than 1

## [0.2.7] - 2021-03-02

### Added

- Added `getMethodNames` and `getProgramTypeData` to `IDriver`

### Changed

- Used `getMethodNames` and `getProgramTypeData` to reduce the sub-graphs in `Extractor::postProject`

## [0.2.6] - 2021-03-01

### Changed

- Changed subgraph-style results to list of edge results in order to improve performance in `GremlinDriver`
- Switched to using `SLF4J` as the logging API

### Fixed

- Fixed issue where `${sys:LOG_DIR}` is generated when there is no `log4j2` config file
- `Call` vertices not containing consistent full names and signatures as `Method` vertices. Resolves #76.

## [0.2.5] - 2021-02-28

### Changed

- Log4j-Core is now only added as a `testImplementation` since this is used as a library and not an application
- `ExtractorConst::getPlumeVersion` now used to get package version
- `VERSION.md` is now where the build obtains version details

## [0.2.4] - 2021-02-26

### Added

- `code`, `lineNumber`, `columnNumber` to `ArrayInitializer`

### Fixed

- Escape " (quotes) to fix Neo4j bug where strings containing quotes fail vertex insertion
- `TypeDecl` to `ArrayInitializer` edge warning

## [0.2.3] - 2021-02-25

### Changed

- `TigerGraphDriver::authKey` never null and now just blank if not set
- Removed `log4f2.properties` under the main artifact
- Made the visibility of driver constructors module specific so that users are forced to use the `DriverFactory`
- `connect` methods on drivers now return the driver instead of nothing.

## [0.2.2] - 2021-02-24

### Added

- `ISchemeSafeDriver` interface for drivers who can install schemas on the database
- `JanusGraphDriver::buildSchema` to dynamically build and install JanusGraph schema

## [0.2.1] - 2021-02-24

### Added

- Dependency `com.tigergraph.client:gsql_client`
- `TigerGraphDriver::buildSchema` to dynamically build and install GSQL schema

### Changed

- Assigned all operator calls to `io.shiftleft.codepropertygraph.generated.Operators` constants
- Assigned values to `ControlStructure::controlStructureType`
- Improved logging

## [0.2.0] - 2021-02-19

### Added

- `Extractor::postProject` to add additional `io.shiftleft.semanticcpg.passes`
  and `io.shiftleft.dataflowengineoss.passes`
- Added `IDriver::getMetaData` to get the `NewMetaData` vertex from the database if present

### Changed

- `Extractor::load` and `Extractor::project` now return `Extractor` instance to allow call chaining

### Fixed

- Graph updates would add duplicate program structure information and fail to link prior `CALL` edges
- Handle the case where `NewFileBuilder#hash` is null
- Where `TypeDecl`s were attempted to be duplicated in `getProgramStructure`
- Fixed case where `Node` types were not handled in `DiffGraphUtil::processDiffGraph`
- `IDriver::getProgramStructure` would not return vertices with degree 0

## [0.1.8] - 2021-02-15

### Added

- `deleteEdge` to `IDriver`
- `updateVertexProperty` to `IDriver`
- `DiffGraphUtil::processDiffGraph` to accept `DiffGraph`s and apply changes to a given `IDriver`

### Changed

- Modified `deleteVertex` signature to take ID and optional label

## [0.1.7] - 2021-02-11

### Changed

- Lifted compilation directory to $TEMP/plume/build. This is then deleted recursively after project.

### Fixed

- Module not found bug introduced by improper class cleanup in temp dir.
- Fixed instances where CallGraphBuilder would connect non-NewCallBuilder source nodes to methods.
- Fixed GraphML not escaping ampersands

## [0.1.6] - 2021-02-10

### Added

- Support for loading JAR files via `load` function

## [0.1.5] - 2021-02-09

### Added

- `AST` edges between `TypeDecl` and their `Modifier`s
- `SOURCE_FILE` edges between `TypeDecl` and their `File`s
- A `File` vertex to represent unknown files

### Fixed

- When Soot cannot get method data, it will log this as a warning instead of throwing a `RuntimeException`

## [0.1.4] - 2021-02-08

### Added

- `TypeDecl` are now properly generated for external types

### Changed

- Replaced Plume enums with `codepropertygraph` constants

### Fixed

- `CALL` edges not created if no `static void main` present

## [0.1.3] - 2021-02-02

### Fixed

- Performance issues with `getProgramStructure` in `OverflowDbDriver`

## [0.1.2] - 2021-02-02

### Changed

- Replaced `PlumeGraph` with `overflowdb.Graph`.
- Removed Gremlin driver transaction logic being present by default.

### Fixed

- Fixed `cmp` bug by adding this to `ExtractorConst#BIN_OPS`.
- Neo4j driver now also connects in the extractor if given to extractor disconnected

## [0.1.1] - 2021-02-01

### Fixed

- Upgraded ASM5 -> ASM8 to fix some JAR support

## [0.1.0] - 2021-01-27

### Changed

- Migrated to ShiftLeft's codepropertygraph domain classes
- Migrated from Neo4j Gremlin Bolt to Neo4j Java Driver (Official Driver)

### Fixed

- Fixed order property and got rid of old implementation

## [0.0.3] - 2020-12-23

### Changed

- Removed use of reflection to improve performance of serializing and deserializing
- Extractor now longer halts process if a schema violation occurs
- ShiftLeft dependencies upgraded

### Fixed

- Argument index was not being implemented properly, this has been fixed.

## [0.0.2] - 2020-12-15

### Added

- The following additional configuration options for OverflowDB
    - overflow
    - heapPercentageThreshold
    - serializationStatsEnabled

### Changed

- The configuration option `dbfilename` changed to `storageLocation` to match OverflowDB's respective config's name.
- Removed polyglot support
- All analyzed files are sent to a temp directory so there is no longer a need to specify class path in the Extractor

### Fixed

- Replaced REF edges between calls and methods with CALL edges.
- Broken jCenter link in README

## [0.0.1] - 2020-12-12

### Added

- Support for 6 graph databases
    - TinkerGraph
    - OverflowDB
    - JanusGraph
    - TigerGraph
    - Amazon Neptune
    - Neo4j
- Can extract code property graphs using Soot for:
    - Java class and source code
    - JavaScript 170 (1.7)
    - Python 2.72
- Can construct call graphs using Soot with the following algorithms:
    - CHA
    - SPARK
