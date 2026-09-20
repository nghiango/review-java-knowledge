# Code Review Target: Blocking Long Work on Request Thread Without Timeout

Review the following report generation controller and service. Identify concurrency, scalability, and thread pool starvation issues.

## Files Under Review

- `ReportController.java`
- `ReportGenerationService.java`

## Review Objectives

1. Determine which thread executes the heavy reporting computation.
2. Evaluate what happens to incoming HTTP traffic when multiple concurrent users request heavy reports.
3. Check for timeout controls and non-blocking asynchronous request processing options (`DeferredResult`, `CompletableFuture`).
