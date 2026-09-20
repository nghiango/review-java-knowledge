# Code Review Target: No Graceful Shutdown with In-Flight Work

Review the following background batch task processor and server configuration. Identify concurrency and lifecycle issues that arise during application shutdown or rolling deployments.

## Files Under Review

- `BatchTaskProcessor.java`
- `application.yml`

## Review Objectives

1. Analyze how the `ExecutorService` is managed during JVM shutdown.
2. Determine what happens to tasks that are currently executing or queued when `SIGTERM` is received.
3. Check Spring Boot server shutdown configuration.
