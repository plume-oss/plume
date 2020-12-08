# Plume
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub Actions](https://github.com/plume-oss/plume-driver/workflows/Kotlin%20CI%20with%20Gradle/badge.svg)
[![codecov](https://codecov.io/gh/plume-oss/plume-driver/branch/develop/graph/badge.svg)](https://codecov.io/gh/plume-oss/plume-driver)

A Kotlin driver for the Plume library to provide an interface for connecting and writing to various graph databases based
on the [code-property graph schema](https://github.com/ShiftLeftSecurity/codepropertygraph/blob/master/codepropertygraph/src/main/resources/schemas/base.json).

For more documentation check out the [Plume docs](https://plume-oss.github.io/plume-docs/).

## Building from Source

In order to use Plume one will need to build from the source code. This will be the case until the Plume project 
can be hosted on a Maven repository or similar.

```shell script
git clone https://github.com/plume-oss/plume-driver.git
cd plume-driver
./gradlew jar # For main artifact only
./gradlew fatJar # For fat jar with dependencies
```
This will build `target/plume-driver-X.X.X[-all].jar` which is imported into your local project. E.g.
```mxml
<dependency>
  <groupId>za.ac.sun.plume</groupId>
  <artifactId>plume-driver</artifactId>
  <version>X.X.X</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/plume-driver-X.X.X.jar</systemPath>
</dependency>
``` 
```groovy
repositories {
    // ...
    flatDir {
        dirs 'lib'
    }
}
dependencies {
    // ...
    implementation name: 'plume-driver-X.X.X'
}
```

## Dependencies

### Packages

The following packages used by the Plume driver are:

* `org.apache.logging.log4j:log4j-core:2.8.2`
* `org.apache.logging.log4j:log4j-slf4j-impl:2.8.2`

Dependencies per graph database technology connected to:

* _TinkerGraph_ 

    `org.apache.tinkerpop:gremlin-core:3.4.8`
    
    `org.apache.tinkerpop:tinkergraph-gremlin:3.4.8`
* _JanusGraph_ 

    `org.apache.tinkerpop:gremlin-core:3.4.8`
    
    `org.janusgraph:janusgraph-driver:0.5.2`
* _TigerGraph_

    `khttp:khttp:1.0.0`
* _Amazon Neptune_

    `org.apache.tinkerpop:gremlin-core:3.4.8`
    
    `org.apache.tinkerpop:gremlin-driver:3.4.8`
* _Neo4j_

    `org.apache.tinkerpop:gremlin-core:3.4.8`
    
    `com.steelbridgelabs.oss:neo4j-gremlin-bolt:0.4.4`
    
It is not recommended using the fat jar in your project if using a build tool such as Ant, Maven, Gradle, etc. Rather,
use the main artifact and add the dependencies manually (in your `pom.xml`, `build.gradle`, etc.). Note that if you are
connecting to Neo4j, for example, you would not need the TinkerGraph, TigerGraph, etc. dependencies. 

### Java Support

The officially supported versions of Java are the following:
* OpenJDK 8
* OpenJDK 9
* OpenJDK 10
* OpenJDK 11

### Graph Database Support

Databases supported:
* TinkerGraph
* JanusGraph
* TigerGraph
* Amazon Neptune
* Neo4j

## Logging

All logging can be configured under `src/main/resources/log4j2.properties`. By default, all logs can be found under 
`/tmp/plume`.