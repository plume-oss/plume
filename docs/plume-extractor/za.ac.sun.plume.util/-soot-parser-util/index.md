[plume-extractor](../../index.md) / [za.ac.sun.plume.util](../index.md) / [SootParserUtil](./index.md)

# SootParserUtil

`object SootParserUtil`

### Functions

| Name | Summary |
|---|---|
| [determineEvaluationStrategy](determine-evaluation-strategy.md) | Given a parameter signature and context of the parameter, determines the evaluation strategy used. TODO: Confirm if these assumptions are true`fun determineEvaluationStrategy(paramType: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, isMethodReturn: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): EvaluationStrategies` |
| [determineModifiers](determine-modifiers.md) | Given the ASM5 access parameter and method name, determines the modifier types.`fun determineModifiers(access: `[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`, name: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`? = null): `[`EnumSet`](https://docs.oracle.com/javase/8/docs/api/java/util/EnumSet.html)`<ModifierTypes>` |
| [isArrayType](is-array-type.md) | `fun isArrayType(type: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
