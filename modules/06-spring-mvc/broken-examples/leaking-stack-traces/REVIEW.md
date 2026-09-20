# Code Review Target: Leaking Internal Exceptions and Stack Traces

Review the following controller, service, and global exception handler. Identify security and information disclosure vulnerabilities.

## Files Under Review

- `OrderController.java`
- `OrderProcessingService.java`
- `GlobalExceptionHandler.java`

## Review Objectives

1. Analyze how unhandled exceptions are caught and formatted in the HTTP response.
2. Evaluate what internal infrastructure details (SQL queries, schema names, package names) are sent to callers.
3. Identify standard error formatting mechanisms (e.g. RFC 9457 `ProblemDetail`).
