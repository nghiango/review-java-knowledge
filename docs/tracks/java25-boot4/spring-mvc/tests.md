# Testing: Web MVC Testing in Spring Boot 4

!!! info "Delta from baseline"
    Baseline testing in [`docs/topics/spring-mvc/tests.md`](../../../topics/spring-mvc/tests.md) demonstrates `MockMvc` standalone setups, JSON Path assertions, and WebMvcTest slices.
    This page covers **testing strategies in Spring Boot 4 / Framework 7**: verifying declarative API version routing, asserting JSpecify nullness contracts, and Bean Validation constraints.

---

## 1. Testing Native API Version Routing

When testing versioned endpoints, ensure tests assert:
1. Valid version headers/parameters route cleanly to expected DTO versions.
2. Missing or unsupported versions result in standard 404 or 406 error responses.
3. Version-specific query parameters or body payloads validate correctly.

### Example Test Suite

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/test/java/lab/java25boot4/springmvc/NativeVersionedOrderControllerTest.java"
```

---

## 2. Testing JSpecify Nullness & Validation

Ensure tests verify:
1. Null optional attributes serialize as absent/null without causing runtime NullPointerExceptions.
2. Bean Validation annotations (`@NotBlank`, `@NotNull`) enforce constraints regardless of static type hints.

### Example Test Suite

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/test/java/lab/java25boot4/springmvc/JSpecifyCustomerControllerTest.java"
```

---

## 3. Testing RFC 9457 Problem Details

Verify that domain exceptions produce standard problem detail structures:

```java
--8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/test/java/lab/java25boot4/springmvc/ModernProblemDetailExceptionHandlerTest.java"
```
