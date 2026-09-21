# Exercises: Web MVC Katas in Spring Boot 4

!!! info "Delta from baseline"
    Baseline exercises in [`docs/topics/spring-mvc/exercises.md`](../../../topics/spring-mvc/exercises.md) cover controller decomposition and custom validators.
    These exercises focus on **Spring Framework 7 and Boot 4 additions**: migrating custom interceptor versioning to declarative framework routing, and enforcing JSpecify nullness contracts.

---

## Exercise 1: Migrate Hand-Rolled Versioning to Declarative Framework Mappings

### Problem Statement
You maintain an existing payment gateway controller that inspects an `X-Version` header inside an interceptor and branches using `if ("1".equals(v)) ... else if ("2".equals(v)) ...`.

### Requirements
1. Remove the custom interceptor and request attribute passing.
2. Refactor the controller into distinct handler methods matching version `1.0` and `2.0`.
3. Configure a fallback error response producing RFC 9457 `ProblemDetail` with status 406 when an unsupported version is requested.

---

## Exercise 2: Enforce JSpecify Null-Safety Across Controller DTOs

### Problem Statement
A user management package has been migrated to `@NullMarked` in `package-info.java`. Several endpoints return null values for missing nicknames and addresses.

### Requirements
1. Annotate optional fields in the response records with `@Nullable`.
2. Ensure `@RequestParam(required = false)` parameters are annotated with `@Nullable`.
3. Add unit tests asserting both present and absent values serialize correctly.
