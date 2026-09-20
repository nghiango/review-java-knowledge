# Review: Circular dependency and field injection

Review `OrderService.java` and `BillingService.java` as a production pull request.
These components coordinate order placement and customer invoicing.

Identify every issue you can find. Consider:

- injection paradigm (field vs constructor injection)
- circular dependency coupling between domain services
- ease of unit testing without reflection or Spring test context
- immutability and final fields
- architectural decoupling through event-driven mechanisms

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:04-spring-core:compileBrokenExamples
```
