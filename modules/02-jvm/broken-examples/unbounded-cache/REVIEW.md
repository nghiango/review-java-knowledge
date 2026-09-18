# Review: Unbounded template cache

Review `TemplateCache.java` as a production pull request. The cache is intended to avoid rendering
the same tenant template repeatedly.

Identify every issue you can find. Consider:

- cache key cardinality and who controls it
- retained value size
- capacity, expiry and eviction policy
- observability for hit rate, size and evictions
- log volume on hot paths

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:02-jvm:compileBrokenExamples
```
