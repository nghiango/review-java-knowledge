# Code Review — Cache Penetration on Missing Entities

## Context

A user profile service caches user profile lookups by user ID using `@Cacheable(value = "userProfiles", key = "#userId")`. When a requested user ID does not exist in the database, the method returns `null`.

Review `UserProfileService.java` for resilience vulnerabilities, denial of service risks, and cache penetration defects.

## What to look for

- Behavior when queried keys do not exist in the database
- Protection against malicious scans of non-existent IDs (cache penetration)
- Null value handling in cache abstractions
- Sentinel caching vs Bloom filter screening
