# Spring Security Solutions & Production Implementations — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline solutions in [`docs/topics/spring-security/solutions.md`](../../../topics/spring-security/solutions.md) cover BCrypt password hashing, method-level `@PreAuthorize`, secure filter order, and stateless JWT filters.
    This section presents the production solutions for the **Spring Boot 4.0 / Spring Security 7 & Java 25 delta**:

---

## Solution 1: Refactoring to Spring Security 7 Lambda-Only DSL

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/11-spring-security/broken-examples/legacy-and-chaining-dsl/SOLUTION.md"
```

### Production Implementation: `ModernSecurityConfiguration.java`
```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/src/main/java/lab/java25boot4/springsecurity/ModernSecurityConfiguration.java"
```

---

## Solution 2: Eliminating Context Pollution with Java 25 `ScopedValue`

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/11-spring-security/broken-examples/virtual-thread-security-context-leak/SOLUTION.md"
```

### Production Coordinator: `ScopedSecurityContextCoordinator.java`
```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/src/main/java/lab/java25boot4/springsecurity/ScopedSecurityContextCoordinator.java"
```

### Production Service: `ModernAuthorizationService.java`
```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/src/main/java/lab/java25boot4/springsecurity/ModernAuthorizationService.java"
```

---

## Trade-Off Analysis

| Strategy | Advantages | Trade-Offs |
|---|---|---|
| **Spring Security 7 Lambda DSL** | Compile-time lexical boundaries; zero ambiguity from `and()`; explicit customizers. | Requires refactoring legacy fluent chains during framework upgrades. |
| **Java 25 `ScopedValue` Propagation** | Immutable; zero-copy thread inheritance; automatic cleanup with zero leakage. | Requires enclosing execution in `ScopedValue.where(...).run(...)`. |
| **Secure-by-Default Authorization** (`anyRequest().authenticated()`) | Eliminates accidental endpoint exposure as new routes are introduced. | Requires explicit `permitAll()` declarations on every public static resource. |
