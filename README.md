<div style="display: block; margin-left: auto;margin-right: auto;width: 40%;">
  <a href="https://plume-oss.github.io/plume-docs/">
    <img src="https://plume-oss.github.io/plume-docs/assets/images/logo-text.png" width="350" alt="Plume Banner">
  </a>
</div>

Plume is a language front-end to construct
ASTs based on the [code-property graphs](https://github.com/ShiftLeftSecurity/codepropertygraph) schema from JVM bytecode. Plume is graph
database agnostic and can store the graphs to multiple graph databases.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub Actions](https://github.com/plume-oss/plume/workflows/CI/badge.svg)
[![](https://jitpack.io/v/plume-oss/plume.svg)](https://jitpack.io/#plume-oss/plume)

## Important

Plume is the original implementation of 
[jimple2cpg](https://github.com/joernio/joern/tree/master/joern-cli/frontends/jimple2cpg). The frontend on Joern project
is optimized around OverflowDB and is much more lightweight. This is project focuses on experimenting with incremental
dataflow analysis and comparing database backend performance.

Versions < 0.6.3 of Plume were Kotlin based but versions from 1.0.0 onwards have been moved to a Scala implementation
for better interfacing with the [CPG schema library](https://github.com/ShiftLeftSecurity/codepropertygraph).

If your project depends on Plume I am happy to still provide maintenance and support but I recommend any new 
research to begin on Joern where I also spend time providing help and support.

## Quickstart

One can run Plume from the `plume` binary which will use `OverflowDB` as the graph database backend if no config is 
found. If one would like to configure another backend then use the CLI arguments:

```bash
Usage: plume [tinkergraph|overflowdb|neo4j|neo4j-embedded|tigergraph|neptune] [options] input-dir

An AST creator for comparing graph databases as static analysis backends.
  -h, --help
  input-dir                The target application to parse.
Command: tinkergraph [options]

  --import-path <value>    The TinkerGraph to import.
  --export-path <value>    The TinkerGraph export path to serialize the result to.
Command: overflowdb [options]

  --storage-location <value>
  --heap-percentage-threshold <value>
  --enable-serialization-stats
Command: neo4j [options]

  --hostname <value>
  --port <value>
  --username <value>
  --password <value>
  --tx-max <value>
Command: neo4j-embedded [options]

  --databaseName <value>
  --databaseDir <value>
  --tx-max <value>
Command: tigergraph [options]

  --hostname <value>
  --restpp-port <value>
  --gsql-port <value>
  --username <value>
  --password <value>
  --timeout <value>
  --tx-max <value>
  --scheme <value>
Command: neptune [options]

  --hostname <value>
  --port <value>
  --key-cert-chain-file <value>
  --tx-max <value>
```

For more documentation and basic guides, check out the [project homepage](https://plume-oss.github.io/plume-docs/) or
the [ScalaDoc](https://plume-oss.github.io/plume/latest/api/io/github/plume/oss/index.html).

*Important*: If you are using the TigerGraph driver you need to install the `gsql_client.jar`and add it to an 
environment variable called GSQL_CLIENT. Instructions are 
[here](https://docs.tigergraph.com/tigergraph-server/current/gsql-shell/using-a-remote-gsql-client) e.g.,

```bash
curl https://docs.tigergraph.com/tigergraph-server/current/gsql-shell/_attachments/gsql_client.jar --output gsql_client.jar
export GSQL_HOME=`pwd`/gsql_client.jar
```

Remember to set the `tgVersion` correctly in the `TigerGraphDriver`.

## Community

* If you have any questions or want to be involved then check out
  our [discussions page](https://github.com/plume-oss/plume/discussions).
* Joern's [Discord](https://discord.gg/28uCANEkK2). Note, this will give you temporary membership
  to the server. Once joined you can obtain permanent membership by being assigned role if necessary. 
* Plume is primarily maintained by [David Baker Effendi](https://davidbakereffendi.github.io/)
    * DM on [Twitter](https://twitter.com/SDBakerEffendi)
    * Email at dbe@sun.ac.za

## Known Bugs

- Due to module encapsulation in Java 17, Kryo serialization for `TinkerGraphDriver` will not work due to serialization
  errors.

## Adding Plume as a Dependency

Replace `X.X.X` with the desired version on [JitPack](https://jitpack.io/#plume-oss/plume).

```sbt
libraryDependencies ++= Seq(
  com.github.plume-oss %% plume % X.X.X
)
```

Don't forget to include the JCenter and JitPack repository in your `build.sbt`.

```sbt
resolvers += "jitpack" at "https://jitpack.io"
```

## Building from Source

Plume releases are available on JitPack. If downloading from JitPack is not an option, or you would like to depend on a
modified version of Plume, you can build Plume locally and use it as an unmanaged dependency. JDK version 11 or higher
is required.

```shell script
git clone https://github.com/plume-oss/plume.git
cd plume
sbt stage
```

This will build `target/scala-2.13/plume_2.13-X.X.X.jar` which can be imported into your local project.

## Benchmarks

Plume specifies a `benchmark` binary which orchestrates running JMH benchmarks for AST creation with various graph 
database backends. While the binary explains the available functions, the execution should be run within `sbt`, e.g.

```sbt
Jmh/runMain com.github.plume.oss.Benchmark overflowdb testprogram -o output -r results --storage-location test.cpg
```

## Logging

Plume uses [SLF4J](http://www.slf4j.org/) as the logging fascade.

## Sponsored by

![Amazon Science](https://assets.amazon.science/dims4/default/ce84994/2147483647/strip/true/crop/1200x630+0+0/resize/1200x630!/quality/90/?url=http%3A%2F%2Famazon-topics-brightspot.s3.amazonaws.com%2Fscience%2F32%2F80%2Fc230480c4f60a534bc077755bae7%2Famazon-science-og-image-squid.png)
