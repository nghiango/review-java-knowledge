# Review: Lost update on shared counter and metrics state

Review `HitCounter.java` and `UserVisitMetricsService.java` as a production pull request. This service
is called by high-throughput web controllers to track site traffic metrics and user visit frequencies.

Identify every issue you can find. Consider:

- atomicity of primitive modifications
- thread safety of Java collections under concurrent writes
- visibility of fields across threads according to the Java Memory Model
- holding synchronized locks during blocking/slow operations
- encapsulation of mutable shared state

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:03-concurrency:compileBrokenExamples
```
