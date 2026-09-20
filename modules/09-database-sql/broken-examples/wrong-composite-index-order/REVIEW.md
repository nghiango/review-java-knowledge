# Code Review: Wrong Composite Index Column Order

## Context
A multi-tenant audit logging system queries events by `tenant_id` and `created_at`. The database administrator added a composite index on `(status, created_at, tenant_id)`.

## Code Under Review
- `schema.sql` — Composite index definition on `audit_logs`.
- `AuditLogRepository.java` — Tenant event query with `WHERE tenant_id = :tenantId AND created_at >= :since ORDER BY created_at DESC LIMIT :limit`.

## Review Questions
1. Why does the B-tree leftmost prefix rule prevent PostgreSQL from performing a direct Index Scan when querying without `status`?
2. How should composite index column ordering be structured based on equality filters, range filters, and sort orders?
3. How does index selectivity and column cardinality influence index efficiency?
