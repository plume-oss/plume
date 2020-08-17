[Plume Extractor](../../index.md) / [za.ac.sun.plume.util](../index.md) / [ResourceCompilationUtil](index.md) / [fetchClassFiles](./fetch-class-files.md)

# fetchClassFiles

`@JvmStatic fun fetchClassFiles(path: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`>`

Returns a list of all the class files under a given directory recursively.

### Parameters

`path` - the path to the directory

### Exceptions

`IOException` - if the path is not a directory or does not exist

**Return**
a list of all .class files under the given directory

`@JvmStatic fun fetchClassFiles(jar: `[`JarFile`](https://docs.oracle.com/javase/8/docs/api/java/util/jar/JarFile.html)`): `[`MutableList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)`<`[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`>`

Returns a list of all the class files inside of a JAR file.

### Parameters

`jar` - the JarFile

**Return**
a list of all `.class` files under the given JAR file.

