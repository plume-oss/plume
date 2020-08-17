[plume-extractor](../../index.md) / [za.ac.sun.plume](../index.md) / [Extractor](index.md) / [load](./load.md)

# load

`fun load(file: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Loads a single Java class file or directory of class files into the cannon.

### Parameters

`file` - the Java source/class file, directory of source/class files, or a JAR file.

### Exceptions

`NullPointerException` - if the file is null

`IOException` - In the case of a directory given, this would throw if .java files fail to compile