# Review: Prototype bean injected directly into a singleton

Review `ExecutionContext.java` and `ReportGenerator.java` as a production pull request.
This service tracks per-request metadata and executes asynchronous batch reporting.

Identify every issue you can find. Consider:

- Spring bean scope lifecycles (singleton vs prototype)
- dependency resolution timing during container initialization
- concurrent access to prototype state injected into singletons
- proper mechanisms for dynamic prototype instantiation (`ObjectProvider`, `@Lookup`, scoped proxies)

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:04-spring-core:compileBrokenExamples
```
