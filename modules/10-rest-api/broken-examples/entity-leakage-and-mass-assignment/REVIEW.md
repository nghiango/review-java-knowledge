# Code Review: Entity Leakage and Mass Assignment

## Context
A user profile update endpoint binds JSON requests directly into internal domain models and returns domain objects directly to API consumers.

## Code Under Review
- `UserAccount.java` — Domain entity with sensitive fields (`role`, `passwordHash`, `accountBalance`).
- `UserAdminController.java` — REST controller handling `GET` and `PUT` with direct entity binding.

## Review Questions
1. How does direct entity binding enable Mass Assignment (OWASP API6:2023) and privilege escalation?
2. What information disclosure vulnerability exists in `GET /api/admin/users/{id}`?
3. How should immutable Java records be designed for request inputs and response representations?
