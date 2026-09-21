# Code Review: InheritableThreadLocal Security Context Pollution

## Context
An asynchronous audit logging service dispatches audit mutations to a background worker pool while attempting to retain the calling user's security context via `TenantSecurityContextHolder`.

## Files Under Review
- `TenantSecurityContextHolder.java`
- `AsyncAuditSecurityService.java`

## Review Questions
1. How does `InheritableThreadLocal` behave when tasks are submitted to pooled threads or when virtual threads are scheduled across carrier threads?
2. What happens to the security context on a worker thread after an asynchronous task completes? Does the thread retain the context for subsequent tasks?
3. How can Java 25 `ScopedValue` or explicit `DelegatingSecurityContextExecutor` eliminate security context leakage and authorization bypasses?
