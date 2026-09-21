# Solutions: Spring MVC Refactoring in Spring Boot 4

!!! info "Delta from baseline"
    Baseline solutions in [`docs/topics/spring-mvc/solutions.md`](../../../topics/spring-mvc/solutions.md) cover controller layer separation, custom validators, and CORS configuration.
    This page details production solutions for **Spring Boot 4 / Framework 7 traps**: native declarative API versioning and JSpecify nullness contracts.

---

## Solution 1: Native Declarative API Versioning

--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/hand-rolled-api-versioning-interceptor/SOLUTION.md"

### Production Reference Implementation

Here is the clean implementation using Spring Framework 7 declarative version mapping:

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/main/java/lab/java25boot4/springmvc/NativeVersionedOrderController.java"
```

### Key Architectural Benefits
- **Zero Interceptor Boilerplate**: The framework handles matching, parameter binding, and version resolution.
- **Independent Method Contracts**: V1 and V2 have dedicated request/response DTOs, preventing contract degradation.

---

## Solution 2: JSpecify Nullness Contract Integration

--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/jspecify-nullness-contract-violation/SOLUTION.md"

### Production Reference Implementation

Here is the clean controller implementation with explicit `@Nullable` annotations:

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/main/java/lab/java25boot4/springmvc/JSpecifyCustomerController.java"
```

### Modern RFC 9457 ProblemDetail Handler

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/main/java/lab/java25boot4/springmvc/ModernProblemDetailExceptionHandler.java"
```
