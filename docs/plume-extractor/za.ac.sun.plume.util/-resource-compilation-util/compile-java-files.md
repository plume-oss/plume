[plume-extractor](../../index.md) / [za.ac.sun.plume.util](../index.md) / [ResourceCompilationUtil](index.md) / [compileJavaFiles](./compile-java-files.md)

# compileJavaFiles

`@JvmStatic fun compileJavaFiles(path: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Given a path to a directory, programmatically compile any .java files found in the directory.

### Parameters

`path` - the path to the directory

### Exceptions

`IOException` - if the path is not a directory or does not exist