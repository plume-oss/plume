[plume-extractor](../../index.md) / [za.ac.sun.plume.util](../index.md) / [ResourceCompilationUtil](./index.md)

# ResourceCompilationUtil

`object ResourceCompilationUtil`

### Functions

| Name | Summary |
|---|---|
| [compileJavaFile](compile-java-file.md) | Given a path to a Java source file, programmatically compiles the source (.java) file.`fun compileJavaFile(file: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`?): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [compileJavaFiles](compile-java-files.md) | Given a path to a directory, programmatically compile any .java files found in the directory.`fun compileJavaFiles(path: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [deleteClassFiles](delete-class-files.md) | Given a path to a directory, programmatically delete any .class files found in the directory.`fun deleteClassFiles(path: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [fetchClassFiles](fetch-class-files.md) | Returns a list of all the class files under a given directory recursively.`fun fetchClassFiles(path: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)`<`[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`>`<br>Returns a list of all the class files inside of a JAR file.`fun fetchClassFiles(jar: `[`JarFile`](https://docs.oracle.com/javase/8/docs/api/java/util/jar/JarFile.html)`): `[`MutableList`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-mutable-list/index.html)`<`[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`>` |
