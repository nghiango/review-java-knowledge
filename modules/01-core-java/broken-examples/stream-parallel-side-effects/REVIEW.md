# Review: Parallel order report

Review the report service for a request path that prices hundreds of orders through a blocking
remote client. The response must contain every order in input order and failures must be diagnosable.

Consider stream contracts, shared state, executor ownership, blocking work, ordering and exception
context. Write findings before opening `SOLUTION.md`.

```bash
./gradlew :modules:01-core-java:compileBrokenExamples
```
