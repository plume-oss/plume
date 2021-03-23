# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

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
