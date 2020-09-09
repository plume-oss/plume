# Plume Extractor
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.org/plume-oss/plume-extractor.svg?branch=develop)](https://travis-ci.org/plume-oss/plume-extractor)
[![codecov](https://codecov.io/gh/plume-oss/plume-extractor/branch/develop/graph/badge.svg)](https://codecov.io/gh/plume-oss/plume-extractor)

Converts a Java program, JAR or class file into a code-property graph and inserts it into a graph database to be 
analysed with various program analysis algorithms. The driver used to communicate with each graph database is 
[plume-driver](https://github.com/plume-oss/plume-driver).

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

## Dependencies

### Packages

The following packages used by plume-extractor:

* `org.ow2.asm:asm:7.3.1`
* `org.ow2.asm:asm-util:7.3.1`
* `org.apache.logging.log4j:log4j-core:2.8.2`
* `org.apache.logging.log4j:log4j-slf4j-impl:2.8.2`
* `za.ac.sun.plume:plume-driver` (under `lib`)

It is not recommended using the fat jar in your project if using a build tool such as Ant, Maven, Gradle, etc. Rather,
use the main artifact and add the dependencies manually (in your `pom.xml`, `build.gradle`, etc.). 

### Java Support

The officially supported versions of Java are the following:
* OpenJDK 8
* OpenJDK 9
* OpenJDK 10
* OpenJDK 11

## Quickstart

We use this directory as the base for the following short tutorial - no build tools required. First, we need a Java 
program to analyze. Here is an example of a file we can create:
```java
public class Example {

	public static void main(String[] args) {
		int a = 1;
		int b = 2;
		if (a > b) {
			a = b + 1;
		} else {
			b -= a + 1;
		}
	}

}
```

For a quick and simple in-memory graph projection of a Java program:
```java
import za.ac.sun.plume.Extractor;
import za.ac.sun.plume.drivers.TinkerGraphDriver;
import java.io.File;
import java.io.IOException;

public class PlumeDemo {

    public static void main(String[] args) {
        try (TinkerGraphDriver driver = (TinkerGraphDriver) DriverFactory.invoke(GraphDatabase.TINKER_GRAPH)) {
            Extractor extractor = new Extractor(driver, new File("."));
            File exampleFile = new File("./Example.java");
            extractor.load(exampleFile);
            extractor.project();
            driver.exportGraph("./plume_demo.xml");
        } catch (IOException e) {
            System.out.println("Something went wrong! Details: " + e.getMessage());
            System.exit(1);
        }
    }

}
```

To compile both of these, we can use the `build/libs/plume-extractor-X.X.X-all.jar` with 
`lib/plume-driver-X.X.X-jar-all.jar`. This can be combined as:
```bash
javac -cp ".:build/libs/plume-extractor-X.X.X-all.jar:lib/plume-driver-X.X.X-all.jar:" *.java
java -cp ".:build/libs/plume-extractor-X.X.X-all.jar:lib/plume-driver-X.X.X-all.jar:" PlumeDemo
```

This exported file can be visualized using tools such as [Cytoscape](https://cytoscape.org/). Using Cytoscape and 
the tree layout, the graph should look something like this:

![Example.java Graph](https://github.com/DavidBakerEffendi/j2GraPL/blob/media/graphs/GraPLDemo.png?raw=true)

## Logging

All logging can be configured under `src/main/resources/log4j2.properties`. By default, all logs can be found under 
`/tmp/plume`.
