# Code Review — Missing Correlation ID Across Threads

## Context

An order fulfillment service offloads compliance auditing to an asynchronous `ExecutorService` thread pool. An engineer authored `AsyncOrderAuditService.java` to asynchronously record audit events while logging diagnostics.

Review `AsyncOrderAuditService.java` for distributed context propagation, thread pool isolation, and log correlation reliability.

## What to look for

- Transfer of Mapped Diagnostic Context (MDC) across asynchronous thread boundaries
- Cross-tenant context leakage in pooled worker threads
- Exception safety and cleanup guarantees (`MDC.clear()`)
- Correlation ID persistence across distributed log aggregators
