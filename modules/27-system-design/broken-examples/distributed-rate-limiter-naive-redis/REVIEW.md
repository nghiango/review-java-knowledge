# System Design Review: Distributed API Rate Limiter

## Context
This design proposal introduces a global rate limiter to protect downstream Spring Boot microservices from denial of service and noisy-neighbor API key abuse.
The proposal uses Redis as a centralized state store shared across 20 gateway instances.

## Files Under Review
- `design.md` — Proposed design and filter code.

## Review Questions
1. What concurrency bugs (TOCTOU) occur in the `get()` then `increment()` implementation when 20 gateway nodes handle simultaneous concurrent requests for the same API key?
2. What happens at the 1-minute window boundary (e.g. from 10:00:59 to 10:01:01)? Does this design enforce the intended 100 req/min limit?
3. What is the impact on gateway throughput and availability if the Redis cluster experiences high latency, network partition, or failover?
4. How would you redesign this using **Sliding Window Counter** or **Token Bucket** algorithms executed atomically via **Redis Lua scripts**?
