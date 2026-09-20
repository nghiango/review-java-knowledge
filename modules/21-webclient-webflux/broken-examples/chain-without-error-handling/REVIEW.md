# Code Review — Chain Without Error Handling

## Context

A pricing discount engine queries multiple upstream catalog and promotional services to calculate the best available discount for a customer cart. An engineer authored `CartPricingEngine.java` combining reactive streams.

Review `CartPricingEngine.java` for unhandled reactive stream termination, lack of fallbacks, and bubbling 500 internal server errors.

## What to look for

- Unhandled error events in reactive pipelines
- Terminal nature of `onError` in Reactive Streams specification
- Graceful degradation via `.onErrorReturn()` or `.onErrorResume()`
- Distinction between fatal errors and recoverable degradation
