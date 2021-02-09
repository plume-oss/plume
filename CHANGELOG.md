# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased] - yyyy-mm-dd

### Added
- `AST` edges between `TypeDecl` and their `Modifier`s
- `SOURCE_FILE` edges between `TypeDecl` and their `File`s
- A `File` vertex to represent unknown files

### Changed

### Fixed
- When Soot cannot get method data, it will log this as a warning 
  instead of throwing a `RuntimeException`

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

- The configuration option `dbfilename` changed to `storageLocation` to match OverflowDB's respective config's
  name.
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
