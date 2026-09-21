# Code Review: Primitive Pattern Matching & Lossy Casts

## Scenario
A pull request introduces `MetricConverter.java` leveraging Java 25 primitive pattern matching in `instanceof` and `switch` expressions to format metric values and convert integer samples.

## Review Objectives
1. Examine the behavior of primitive type patterns (`instanceof byte`, `instanceof int`) on boxed values and evaluate whether narrowing conversions occur safely.
2. Identify potential silent integer overflow or precision truncation hazards.
3. Recommend safe, modern Java 25 pattern matching practices that prevent silent data corruption.
