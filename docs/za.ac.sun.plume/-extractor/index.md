[Plume Extractor](../../index.md) / [za.ac.sun.plume](../index.md) / [Extractor](index.md)

# Extractor

`class Extractor`

### Constructors

| Name | Summary |
|---|---|
| [&lt;init&gt;](-init-.md) | `Extractor(hook: IDriver)`<br>`Extractor(hook: IDriver, classPath: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`?)` |

### Functions

| Name | Summary |
|---|---|
| [load](load.md) | Loads a single Java class file or directory of class files into the cannon.`fun load(file: `[`File`](https://docs.oracle.com/javase/8/docs/api/java/io/File.html)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [project](project.md) | Projects all loaded Java classes currently loaded.`fun project(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
