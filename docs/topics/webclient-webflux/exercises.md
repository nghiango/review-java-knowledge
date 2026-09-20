# WebClient & WebFlux Exercises

Hands-on exercises to master non-blocking stream composition, bounded concurrency, and error isolation.

## Exercise 1: Bounded Multi-Source Reactive Batch Dispatcher

### Problem
In a notification dispatcher service, send alerts for thousands of users concurrently over WebClient. If downstream services experience high latency, the pipeline must neither exhaust Netty's connection pool nor abort the batch when individual requests fail.

### Requirements
1. Consume a list of messages and fan out requests using `Flux.flatMap`.
2. Restrict maximum concurrent in-flight requests using an explicit `maxConcurrency` parameter (e.g., 16).
3. Isolate inner publisher errors using `.onErrorResume(...)` so a single failed notification (e.g. 404 or connection reset) does not cancel remaining messages in the batch.
4. Emit a `NotificationResult` for every message indicating whether delivery succeeded.

??? tip "Reveal solution"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/flatmapconcurrency/CorrectNotificationBatchSender.java"
    ```

---

## Exercise 2: Resilient Aggregator with Fallbacks and Deadlines

### Problem
Implement an e-commerce cart calculation service that calls two upstream promotional microservices (Loyalty Discount Service and Coupon Service) in parallel using `Mono.zip`. If either service hangs or returns an HTTP 500 error, the checkout pipeline must still complete successfully with a graceful fallback discount.

### Requirements
1. Query loyalty and coupon services concurrently using non-blocking `WebClient` calls.
2. Enforce an individual timeout deadline on each independent branch.
3. Apply `.onErrorReturn(0.0)` to both branches to prevent an unhandled terminal `onError` signal from aborting `Mono.zip`.
4. Calculate final cart price safely even when all promotional services are offline.

??? tip "Reveal solution"
    ```java
    --8<-- "modules/21-webclient-webflux/src/main/java/lab/webflux/noerrorhandler/CorrectCartPricingEngine.java"
    ```

---

## Related

- [Concepts](concepts.md) — Reactive stream fundamentals
- [Code Review](code-review.md) — Analyzing common anti-patterns
- [Solutions](solutions.md) — In-depth architectural trade-off analysis
- [Tests](tests.md) — Verifying reactive streams with StepVerifier
- [Production](production.md) — Production readiness and incident triage
