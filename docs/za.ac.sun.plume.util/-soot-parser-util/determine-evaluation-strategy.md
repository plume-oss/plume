[Plume Extractor](../../index.md) / [za.ac.sun.plume.util](../index.md) / [SootParserUtil](index.md) / [determineEvaluationStrategy](./determine-evaluation-strategy.md)

# determineEvaluationStrategy

`@JvmStatic fun determineEvaluationStrategy(paramType: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, isMethodReturn: `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`): EvaluationStrategies`

Given a parameter signature and context of the parameter, determines the evaluation strategy used.
TODO: Confirm if these assumptions are true

### Parameters

`paramType` - the parameter signature from ASM5

`isMethodReturn` - true if the parameter type is from a method

**Return**
the type of evaluation strategy used

