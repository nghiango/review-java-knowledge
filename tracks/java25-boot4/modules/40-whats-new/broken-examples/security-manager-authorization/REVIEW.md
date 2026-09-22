# Code Review: SecurityManager-Based Authorization

## Scenario

`SecurityManagerGate.java` and `AdminOperationService.java` are part of the Java 25 migration of an
internal admin API. The gate decides whether a caller may run destructive operations such as
purging tenant data.

The pull request description says:

> *"Authorization stays exactly as it was on Java 21 — we check the `admin.operations` permission
> through the `SecurityManager` before running anything destructive. No behaviour change."*

## Review Objectives

1. Does the `SecurityManager` still enforce anything on Java 25?
2. What does `isAuthorized` return for a normal caller on Java 25, and what does that imply for
   `purgeTenantData`?
3. Is an authorization check that can default to "allow" acceptable?
4. Where should the decision actually live, and what should it depend on?

Consider these dimensions:

- authentication vs authorization
- fail-open vs fail-closed
- deprecation and platform support
- caller identity
- testability

Write your findings down before opening `SOLUTION.md`.
