# Code Review — Retrying Non-Idempotent Call

## Context

A subscription billing engine charges customer credit cards via an internal billing microservice using `POST /api/v1/charges`. To guard against network drops, the author caught `ResourceAccessException` (which includes socket read timeouts) and added automatic retries.

Review `BillingService.java` for data consistency, double charges, and network ambiguity issues.

## What to look for

- Idempotency guarantees on mutating HTTP `POST` requests
- Ambiguity of `SocketTimeoutException`: did the server execute the charge before the client timed out?
- Missing `Idempotency-Key` or request tokens
- Jitter and backoff strategies
