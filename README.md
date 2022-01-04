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

## Important

Plume is going into maintenance mode. [jimple2cpg](https://github.com/joernio/jimple2cpg) is the new JVM bytecode to CPG
project that is optimized around OverflowDB and is much more lightweight. Future development will take place on 
[jimple2cpg](https://github.com/joernio/jimple2cpg).

Right now Plume is getting a Scala overhaul in order to improve efficiency and interface better with the CPG schema library. This can be followed [here](https://github.com/plume-oss/plume/tree/dave/scala-overhaul). This addresses issues related to performance bias towards in-memory databases and includes a shell script to launch Plume from.

## Learn More

For more documentation and basic guides, check out the [project homepage](https://plume-oss.github.io/plume-docs/).

## Community

* If you have any questions or want to be involved then check out
  our [discussions page](https://github.com/plume-oss/plume/discussions).
* Joern's [Discord](https://discord.gg/28uCANEkK2). Note, this will give you temporary membership
  to the server. Once joined you can obtain permanent membership by being assigned role if necessary. 
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

## Logging

Plume uses [SLF4J](http://www.slf4j.org/) as the fascade with
[Log4j2](https://logging.apache.org/log4j/2.x/) as the implementation.

Note that due to the size of method related operations, there is a CLI loading bar used to indicate progress. This bar
is only shown on TRACE, DEBUG, and INFO levels.

## Sponsored by

![Amazon Science](https://assets.amazon.science/dims4/default/ce84994/2147483647/strip/true/crop/1200x630+0+0/resize/1200x630!/quality/90/?url=http%3A%2F%2Famazon-topics-brightspot.s3.amazonaws.com%2Fscience%2F32%2F80%2Fc230480c4f60a534bc077755bae7%2Famazon-science-og-image-squid.png)
