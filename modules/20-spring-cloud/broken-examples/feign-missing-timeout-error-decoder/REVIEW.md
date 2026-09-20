# Architecture Review — Feign Missing Timeout and Error Decoder

## Context

A payment and order processing service integrates with an internal inventory service via Spring Cloud OpenFeign. An engineer authored `InventoryClient.java` and its configuration.

Review `InventoryClient.java` for resilience anti-patterns, timeout hazards, exception decoding, and retry storms.

## What to look for

- Connection and read timeout configuration on Feign clients
- Handling of downstream HTTP error status codes (4xx vs 5xx) via custom `ErrorDecoder`
- Unbounded retry loops and retryer configuration
- Circuit breaking integration with Spring Cloud CircuitBreaker / Resilience4j
- Header propagation and security context leakage
