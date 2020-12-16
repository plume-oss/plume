# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [Unreleased] - yyyy-mm-dd

### Added

### Changed

### Fixed

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