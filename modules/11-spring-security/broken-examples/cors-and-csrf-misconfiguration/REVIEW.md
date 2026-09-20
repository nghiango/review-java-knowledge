# Code Review: CORS & CSRF Misconfiguration

## Context
A banking web application supports cookie-based form login and balance transfers between user accounts.

## Target Files
- [`InsecureWebSecurityConfig.java`](InsecureWebSecurityConfig.java)
- [`AccountFundsController.java`](AccountFundsController.java)

## Task
Review `InsecureWebSecurityConfig.java` and `AccountFundsController.java`. Identify Cross-Site Request Forgery (CSRF) vulnerabilities, improper CSRF disabling on session-based endpoints, and dangerous wildcard CORS credentials configurations.
