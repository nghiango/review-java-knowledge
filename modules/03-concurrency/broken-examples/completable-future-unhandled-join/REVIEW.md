# Review: CompletableFuture synchronous join in loops and common-pool starvation

Review `PricingClient.java`, `OrderHistoryClient.java`, and `CustomerDashboardService.java` as a production pull request.
This service aggregates asynchronous metrics and pricing data to assemble a customer dashboard.

Identify every issue you can find. Consider:

- the asynchronous execution model of `CompletableFuture.supplyAsync()`
- thread pool selection for blocking I/O tasks
- calling `.join()` inside loop iterations vs non-blocking composition (`CompletableFuture.allOf()`)
- exception propagation, resilience, and fallback handling
- absence of bounded timeouts (`orTimeout()` / `completeOnTimeout()`)

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:03-concurrency:compileBrokenExamples
```
