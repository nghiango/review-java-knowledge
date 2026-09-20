# Code Review — Dual-Write Consistency & Transaction Ordering

## Context

A digital wallet and payment processing service transfers funds between customer accounts. The service executes database balance mutations inside `@Transactional` methods and synchronously updates cached wallet balances in Redis.

Review `WalletTransferService.java` for transaction atomicity violations, cache-database consistency defects, and rollback failure modes.

## What to look for

- Cache mutation inside active database transactions
- Handling of database rollback after cache write (dirty cache writes)
- Dual-write race conditions under concurrent updates
- Cache update vs cache invalidation (eviction) semantics
