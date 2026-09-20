# Code Review: Inconsistent Error Contract and Stack Trace Leakage

## Context
A customer management REST API handles validation and runtime errors with ad-hoc responses (plain text, differing JSON keys, and un-sanitized internal stack traces).

## Code Under Review
- `CustomerController.java` — REST controller returning varied error shapes.
- `LegacyErrorHandler.java` — Global exception handler emitting raw stack traces.

## Review Questions
1. Why is an ad-hoc, heterogeneous error contract painful for client SDK generation and automated error handling?
2. What information disclosure risks occur when internal stack traces and exception hierarchies are exposed to public callers?
3. How does Spring Boot 3+ adopt RFC 9457 Problem Details (`ProblemDetail` / `application/problem+json`) to standardize error responses?
