# Code Review — Infinite TTL & Memory Exhaustion in Redis

## Context

A security session and catalog search service tracks user login sessions and caches search result payloads in Redis using `StringRedisTemplate`.

Review `UserSessionTracker.java` for resource leaks, memory exhaustion risks, and cache retention policy flaws.

## What to look for

- Key lifecycle, expiration dates, and TTL enforcement
- High-cardinality dynamic key proliferation in in-memory databases
- Redis eviction policy behavior (`noeviction` vs `volatile-lru` vs `allkeys-lru`)
- Long-term memory exhaustion and Redis OOM crash scenarios
