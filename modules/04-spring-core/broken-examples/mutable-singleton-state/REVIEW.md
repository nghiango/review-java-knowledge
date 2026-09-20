# Review: Mutable per-request state in a Spring singleton bean

Review `DiscountCalculationService.java` as a production pull request.
This service calculates tiered and loyalty promotional discounts during checkout.

Identify every issue you can find. Consider:

- the singleton lifecycle and multi-threaded request model of Spring `@Service` beans
- thread safety and race conditions on instance fields
- cross-tenant/user data leakage under concurrent checkout traffic
- stateless design vs parameter-passing in enterprise services

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:04-spring-core:compileBrokenExamples
```
