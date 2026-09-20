# Code Review — Catch and Ignore Failures

## Context

A loan underwriting platform checks external credit bureau scores via `CustomerRiskService`. The developer added a Resilience4j `@CircuitBreaker` annotation with a fallback method to protect against downstream credit bureau degradation, but wrapped the internal call in a `try-catch` block returning a default risk score.

Review `CustomerRiskService.java` for circuit breaker state management, metrics, and error propagation.

## What to look for

- How Resilience4j AOP aspects intercept exceptions
- Impact of internal `try-catch` blocks on CircuitBreaker metrics and state transitions
- Execution of `@CircuitBreaker(fallbackMethod = ...)`
- Zombie degraded mode and silent telemetry failures
