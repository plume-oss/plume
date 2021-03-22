<div style="display: block; margin-left: auto;margin-right: auto;width: 40%;">
  <a href="https://plume-oss.github.io/plume-docs/">
    <img src="https://plume-oss.github.io/plume-docs/assets/images/logo-text.png" width="350" alt="Plume Banner">
  </a>
</div>

Plume is a language front-end to construct
an [intermediate representation](https://en.wikipedia.org/wiki/Intermediate_representation) called
a [code-property graphs](https://github.com/ShiftLeftSecurity/codepropertygraph) from JVM bytecode. Plume is graph
database agnosic and can store code-property graphs to multiple graph databases.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub Actions](https://github.com/plume-oss/plume/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/plume-oss/plume/branch/master/graph/badge.svg?token=4WY0U6QCU6)](https://codecov.io/gh/plume-oss/plume)
[![](https://jitpack.io/v/plume-oss/plume.svg)](https://jitpack.io/#plume-oss/plume)

## Learn More

For more documentation and basic guides, check out the [project homepage](https://plume-oss.github.io/plume-docs/).

## Community

* If you have any questions or want to be involved then check out
  our [discussions page](https://github.com/plume-oss/plume/discussions).
* Joern's [Gitter](https://gitter.im/joern-code-analyzer/community).
* Plume is primarily maintained by [David Baker Effendi](https://davidbakereffendi.github.io/)
    * DM on [Twitter](https://twitter.com/SDBakerEffendi)
    * Email at dbe@sun.ac.za

## Adding Plume as a Dependency

Replace `X.X.X` with the desired version on [JitPack](https://jitpack.io/#plume-oss/plume).

Maven:

```mxml
<dependency>
  <groupId>io.github.plume-oss</groupId>
  <artifactId>plume</artifactId>
  <version>X.X.X</version>
  <type>pom</type>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.plume-oss:plume:X.X.X'
```

Don't forget to include the JCenter and JitPack repository in your `pom.xml` or `build.gradle`.

Maven:

```mxml
<project>
  [...]
  <repositories>
    <repository>
      <id>jcenter</id>
      <name>jcenter</name>
      <url>https://jcenter.bintray.com</url>
    </repository>
    <repository>
      <id>jitpack</id>
      <name>jitpack</name>
      <url>https://jitpack.io</url>
    </repository>
  </repositories>
  [...]
</project>
```

Gradle:

```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}
```

## Building from Source

Plume releases are available on JitPack. If downloading from JitPack is not an option, or you would like to depend on a
modified version of Plume, you can build Plume locally and use it as an unmanaged dependency. JDK version 11 or higher
is required.

```shell script
git clone https://github.com/plume-oss/plume.git
cd plume
./gradlew jar
```

This will build `build/libs/plume-X.X.X.jar` which can be imported into your local project.

## Dependencies

### Packages

The following packages used for logging:

```groovy
implementation 'org.apache.logging.log4j:log4j-core'
implementation 'org.apache.logging.log4j:log4j-slf4j-impl'
```

The extractor uses the following dependencies:

```groovy
implementation 'org.soot-oss:soot'
implementation 'org.lz4:lz4-java'
```

Dependencies per graph database technology:

#### _TinkerGraph_

```groovy
implementation 'org.apache.tinkerpop:gremlin-core'
implementation 'org.apache.tinkerpop:tinkergraph-gremlin'
```

#### _OverflowDb_

```groovy
implementation 'io.shiftleft:codepropertygraph_2.13'
implementation 'io.shiftleft:semanticcpg_2.13'
```

#### _JanusGraph_

```groovy
implementation 'org.apache.tinkerpop:gremlin-core'
implementation 'org.janusgraph:janusgraph-driver'
```

#### _TigerGraph_

```groovy
implementation 'khttp:khttp'
implementation 'com.fasterxml.jackson.core:jackson-databind'
implementation 'com.tigergraph.client:gsql_client'
```

#### _Amazon Neptune_

```groovy
  implementation 'org.apache.tinkerpop:gremlin-core'
implementation 'org.apache.tinkerpop:gremlin-driver'
```

#### _Neo4j_

```groovy
implementation 'org.neo4j.driver:neo4j-java-driver'
```

Note that if you are connecting to Neo4j, for example, you would not need the TinkerGraph, TigerGraph, etc.
dependencies.

## Logging

Plume uses [SLF4J](http://www.slf4j.org/) as the fascade with
[Log4j2](https://logging.apache.org/log4j/2.x/) as the implementation.

Note that due to the size of method related operations, there is a CLI loading bar used to indicate progress. This bar
is only shown on TRACE, DEBUG, and INFO levels.