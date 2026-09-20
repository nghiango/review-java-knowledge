# Code Review: Wrong Propagation and Connection Pool Exhaustion

## Background
`OrderService` calls `AuditLogService.recordAudit` configured with `Propagation.REQUIRES_NEW`. Under peak load, threads in `OrderService` hung indefinitely, resulting in complete application deadlock.

## Questions to Consider
1. How does `Propagation.REQUIRES_NEW` interact with the outer transaction and HikariCP connection pool?
2. How many database connections does a single thread hold when invoking `REQUIRES_NEW` inside an active transaction?
3. What happens if all pool connections are held by suspended outer transactions waiting for a second connection?
