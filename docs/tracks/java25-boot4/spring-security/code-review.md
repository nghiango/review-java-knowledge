# Spring Security Code Review — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline code review targets in [`docs/topics/spring-security/code-review.md`](../../../topics/spring-security/code-review.md) address plaintext passwords, IDOR, broken object authorization, and broad permitAll.
    This review section examines **Spring Boot 4.0 / Spring Security 7 & Java 25 review targets**:
    
    1. **Legacy DSL Chaining & Matcher Ordering**: Obsolete `and()` method chaining removed in Spring Security 7 coupled with misplaced authorization matchers.
    2. **InheritableThreadLocal Security Context Pollution**: Concurrency pitfalls with thread-local context retention across asynchronous workers.

---

## Review Target 1: Legacy Spring Security DSL Chaining & Matcher Ordering

### Context
A pull request configures the HTTP security filter chain for an administrative portal, attempting to permit public endpoints and restrict admin operations using legacy Spring Security chaining syntax.

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/broken-examples/legacy-and-chaining-dsl/LegacySecurityConfig.java"
```

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/broken-examples/legacy-and-chaining-dsl/AdminUserController.java"
```

??? tip "Review Guidance"
    - How does Spring Security 7 treat `and()` method chaining and parameterless configuration methods?
    - How does the matcher configuration handle incoming requests that match multiple rules?
    - Does `anyRequest().permitAll()` represent a safe default posture?

---

## Review Target 2: InheritableThreadLocal Security Context Pollution

### Context
An asynchronous audit logging service dispatches audit mutations to a background worker pool while attempting to retain the calling user's security context via `TenantSecurityContextHolder`.

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/broken-examples/virtual-thread-security-context-leak/TenantSecurityContextHolder.java"
```

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/broken-examples/virtual-thread-security-context-leak/AsyncAuditSecurityService.java"
```

??? tip "Review Guidance"
    - How does `InheritableThreadLocal` behave when tasks are submitted to pooled threads or when virtual threads are scheduled across carrier threads?
    - What happens to the security context on a worker thread after an asynchronous task completes?
    - How can Java 25 `ScopedValue` eliminate security context leakage and authorization bypasses?
