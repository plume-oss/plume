# Plume Extractor
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub Actions](https://github.com/plume-oss/plume-extractor/workflows/Kotlin%20CI%20with%20Gradle/badge.svg)
[![codecov](https://codecov.io/gh/plume-oss/plume-extractor/branch/develop/graph/badge.svg)](https://codecov.io/gh/plume-oss/plume-extractor)

Converts a Java program, JAR or class file into a code-property graph and inserts it into a graph database to be 
analysed with various program analysis algorithms. The driver used to communicate with each graph database is 
[plume-driver](https://github.com/plume-oss/plume-driver).

Checkout the [docs](https://plume-oss.github.io/plume-docs/) for more.

## Building from Source

In order to use plume-extractor, one needs to also make use of plume-driver to interface with a given graph database.
Right now, `lib` contains the latest stable version of plume-driver. This will be the case until the Plume project can be
hosted on a Maven repository or similar.

```shell script
git clone https://github.com/plume-oss/plume-extractor.git
cd plume-extractor
./gradlew jar # For main artifact only
./gradlew fatJar # For fat jar with dependencies
```
This will build `target/plume-extractor-X.X.X[-all].jar` which is imported into your local project. One can choose to use the 
main artifact or fat jar but here is how one can import this into one's Maven or Gradle project respectively. E.g.
```mxml
<dependency>
  <groupId>za.ac.sun.plume</groupId>
  <artifactId>plume-extractor</artifactId>
  <version>X.X.X</version>
  <scope>system</scope>
  <systemPath>${project.basedir}/lib/plume-extractor-X.X.X.jar</systemPath>
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
    implementation name: 'plume-extractor-X.X.X'
}
```

## Java Support

The officially supported versions of Java are the following:
* OpenJDK 8
* OpenJDK 9
* OpenJDK 10
* OpenJDK 11

## Logging

All logging can be configured under `src/main/resources/log4j2.properties`. By default, all logs can be found under 
`/tmp/plume`.
