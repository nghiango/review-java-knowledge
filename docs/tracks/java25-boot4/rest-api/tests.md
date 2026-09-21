# Testing REST APIs in Spring Boot 4

!!! info "Delta from baseline"
    Baseline test guide [`docs/topics/rest-api/tests.md`](../../../topics/rest-api/tests.md) covers MockMvc, status assertions, and JSON path verifications.
    This page covers testing **Spring Boot 4.0 / Spring Framework 7 REST endpoints**: validating native version routing, RFC 8594 lifecycle headers, RFC 9457 error contracts, and declarative HTTP client proxies.

---

## 1. Verifying Version Routing and RFC 8594 Headers

Unit testing versioned endpoints ensures that deprecated routes include mandatory `Deprecation` and `Sunset` headers while new versions remain clean.

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/src/test/java/lab/java25boot4/restapi/NativeVersionedOrderApiControllerTest.java"
```

---

## 2. Testing Declarative HTTP Interface Clients & Error Decoding

Verifying that declarative HTTP clients preserve remote RFC 9457 `ProblemDetail` payloads without discarding downstream diagnostic properties.

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/src/test/java/lab/java25boot4/restapi/ModernHttpInterfaceGatewayClientTest.java"
```

---

## 3. Testing RFC 8594 and RFC 9457 Response Factories

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/src/test/java/lab/java25boot4/restapi/ModernRestApiResponseFactoryTest.java"
```
