# Code Review: Spring MVC Practice in Spring Boot 4

!!! info "Delta from baseline"
    Baseline code reviews in [`docs/topics/spring-mvc/code-review.md`](../../../topics/spring-mvc/code-review.md) focus on god controllers, missing Bean Validation, and CORS wildcard vulnerabilities.
    This page presents two code review targets focusing on **Spring Boot 4 / Framework 7 traps**: obsolete hand-rolled API version interceptors and JSpecify nullness contract violations.

---

## Review Target 1: `LegacyVersionInterceptor.java` & `OrderController.java`

Review the version dispatching implementation below. Notice how API version routing is handled manually via an interceptor and controller reflection.

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/hand-rolled-api-versioning-interceptor/LegacyVersionInterceptor.java"
```

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/hand-rolled-api-versioning-interceptor/OrderController.java"
```

### Review Questions
1. Why does manual version branching inside the controller method break OpenAPI specification generation and content negotiation?
2. What HTTP status and body format are returned when a client supplies an invalid version header?
3. How does Spring Framework 7's first-class declarative API versioning eliminate both the interceptor and the controller `if-else` branching?

---

## Review Target 2: `CustomerProfileController.java`

Review the customer profile endpoint below located in a package declared with JSpecify `@NullMarked`.

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/jspecify-nullness-contract-violation/package-info.java"
```

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/jspecify-nullness-contract-violation/CustomerProfileResponse.java"
```

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/broken-examples/jspecify-nullness-contract-violation/CustomerProfileController.java"
```

### Review Questions
1. In a `@NullMarked` package, what contract does the unannotated field `String phoneNumber` assert?
2. How does `@RequestParam(required = false)` interact with unannotated parameter types?
3. How should JSpecify null-safety contracts be declared on optional request inputs and response fields?
