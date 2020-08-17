[plume-extractor](../../index.md) / [za.ac.sun.plume.util](../index.md) / [SootParserUtil](index.md) / [determineModifiers](./determine-modifiers.md)

# determineModifiers

`@JvmStatic @JvmOverloads fun determineModifiers(access: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null): `[`EnumSet`](https://docs.oracle.com/javase/8/docs/api/java/util/EnumSet.html)`<ModifierTypes>`

Given the ASM5 access parameter and method name, determines the modifier types.

In Java, all non-static methods are by default "virtual functions." Only methods marked with the keyword final,
which cannot be overridden, along with private methods, which are not inherited, are non-virtual.

### Parameters

`access` - ASM5 access parameter obtained from visitClass and visitMethod.

`name` - name of the method obtained from visitClass and visitMethod.

**Return**
an EnumSet of the applicable modifier types.

