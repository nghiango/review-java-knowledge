# Code Review: Overly Broad PermitAll & RequestMatcher Ordering

## Context
A security configuration configures authorization rules for public API endpoints and restricted administrative reporting endpoints.

## Target Files
- [`InsecureSecurityConfig.java`](InsecureSecurityConfig.java)
- [`AdminMetricsController.java`](AdminMetricsController.java)

## Task
Review `InsecureSecurityConfig.java`. Identify authorization bypasses caused by `requestMatchers` evaluation order and overly broad wildcard patterns.
