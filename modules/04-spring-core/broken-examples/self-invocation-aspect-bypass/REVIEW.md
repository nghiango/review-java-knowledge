# Review: Spring AOP aspect bypassed by self-invocation

Review `Audited.java`, `AuditAspect.java`, and `OrderProcessingService.java` as a production pull request.
This service uses a custom `@Audited` AOP aspect to record compliance audit trails during order processing.

Identify every issue you can find. Consider:

- Spring AOP proxy architecture (CGLIB / JDK Dynamic proxies)
- execution path when invoking annotated methods via `this` (internal self-invocation)
- visibility of AOP advice across public vs private/package-private methods
- architectural separation of cross-cutting concerns

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:04-spring-core:compileBrokenExamples
```
