# Code Review — Cache Stampede on Expiry (Thundering Herd)

## Context

A gaming analytics service calculates and caches category leaderboards. Recomputing the leaderboard is computationally expensive, executing heavy table scans and sorting across millions of records. The leaderboard is cached in Redis with a 10-minute TTL.

Review `LeaderboardService.java` for concurrency bottlenecks, failure modes under peak traffic, and database exhaustion risks.

## What to look for

- Cache miss handling under concurrent load
- Key expiration synchronization and stampede dynamics
- Protection mechanisms against the thundering herd problem
- TTL strategy and jitter
