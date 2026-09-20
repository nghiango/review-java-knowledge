# Architecture Review — Spring Cloud Gateway Unbounded Routing

## Context

An engineering team authored an edge routing configuration for Spring Cloud Gateway (`application.yml`) to expose microservices (order service, payment service, catalog service) to public clients.

Review `application.yml` for connection pool exhaustion hazards, missing timeouts, lack of rate limiting, and missing circuit breaker fallbacks.

## What to look for

- Connect and response timeout boundaries at both global Netty client and per-route levels
- Circuit breaker filter and fallback URI protection for downstream service calls
- Distributed rate limiting filters (e.g. Redis-backed Token Bucket) to prevent downstream saturation
- Safe request header forwarding (stripping untrusted internal routing/auth headers from client requests)
