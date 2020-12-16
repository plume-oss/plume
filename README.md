# Plume
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub Actions](https://github.com/plume-oss/plume/workflows/CI/badge.svg)
[![codecov](https://codecov.io/gh/plume-oss/plume/branch/develop/graph/badge.svg)](https://codecov.io/gh/plume-oss/plume)
[![Download](https://api.bintray.com/packages/plume-oss/maven/plume/images/download.svg)](https://bintray.com/plume-oss/maven/plume/_latestVersion)

A Kotlin driver for the Plume library to provide an interface for connecting and writing to various graph databases based
on the [code-property graph schema](https://github.com/ShiftLeftSecurity/codepropertygraph/blob/master/codepropertygraph/src/main/resources/schemas/base.json).

For more documentation check out the [Plume docs](https://plume-oss.github.io/plume-docs/).

## Download from jCenter Bintray

Replace `X.X.X` with the desired version on [jCenter](https://bintray.com/plume-oss/maven/plume/_latestVersion).

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

Don't forget to include the jCenter repository in your `pom.xml` or `build.gradle`.

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
  </repositories>
  [...]
</project>
```

Gradle:
```groovy
repositories {
    jcenter()
}
```

## Building from Source

In order to use Plume one will need to build from the source code. This will be the case until the Plume project
can be hosted on a Maven repository or similar.

```shell script
git clone https://github.com/plume-oss/plume.git
cd plume-driver
./gradlew oneJar # For main artifact only
./gradlew fatJar # For fat jar with dependencies
```
This will build `target/plume-X.X.X[-all].jar` which is imported into your local project.

## Dependencies

### Packages

The following packages used for logging:

```groovy
implementation 'org.apache.logging.log4j:log4j-core:2.13.3'
implementation 'org.apache.logging.log4j:log4j-slf4j-impl:2.13.3'
```

The extractor uses the following dependencies:
```groovy
  implementation 'org.soot-oss:soot:4.2.1'
  implementation 'org.lz4:lz4-java:1.7.1'
```

Dependencies per graph database technology:

#### _TinkerGraph_
```groovy
    implementation 'org.apache.tinkerpop:gremlin-core:3.4.8'
    implementation 'org.apache.tinkerpop:tinkergraph-gremlin:3.4.8'
```
#### _OverflowDb_
```groovy
  implementation 'io.shiftleft:codepropertygraph_2.13:1.3.5'
  implementation 'io.shiftleft:semanticcpg_2.13:1.3.5'
```
#### _JanusGraph_
```groovy
  implementation 'org.apache.tinkerpop:gremlin-core:3.4.8'
  implementation 'org.janusgraph:janusgraph-driver:0.5.2'
```
#### _TigerGraph_
```groovy
  implementation 'khttp:khttp:1.0.0'
  implementation 'com.fasterxml.jackson.core:jackson-databind:2.11.2'
```
#### _Amazon Neptune_
```groovy
  implementation 'org.apache.tinkerpop:gremlin-core:3.4.8'
  implementation 'org.apache.tinkerpop:gremlin-driver:3.4.8'
```
#### _Neo4j_
```groovy
  implementation 'org.apache.tinkerpop:gremlin-core:3.4.8'
  implementation 'com.steelbridgelabs.oss:neo4j-gremlin-bolt:0.4.4'
```

It is not recommended using the fat jar in your project if using a build tool such as Ant, Maven, Gradle, etc. Rather,
use the main artifact and add the dependencies manually (in your `pom.xml`, `build.gradle`, etc.). Note that if you are
connecting to Neo4j, for example, you would not need the TinkerGraph, TigerGraph, etc. dependencies.

## Logging

All logging can be configured under `src/main/resources/log4j2.properties`. By default, all logs can be found under
`$TEMP/plume`.