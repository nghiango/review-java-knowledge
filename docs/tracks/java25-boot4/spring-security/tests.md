# Testing Spring Security in Spring Boot 4

!!! info "Delta from baseline"
    Baseline test guide [`docs/topics/spring-security/tests.md`](../../../topics/spring-security/tests.md) covers `@WithMockUser`, testing method security with MockMvc, and CSRF request post-processors.
    This page covers testing **Spring Boot 4.0 / Spring Security 7 features**: verifying modern security filter chains, validating `ScopedValue` context isolation, and testing authorization decisions.

---

## 1. Testing Modern Security Configuration & Encoders

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/src/test/java/lab/java25boot4/springsecurity/ModernSecurityConfigurationTest.java"
```

---

## 2. Testing `ScopedValue` Context Isolation & Virtual Threads

Verifying that security context bound via `ScopedValue` is strictly bounded and cannot contaminate sibling or child virtual threads.

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/src/test/java/lab/java25boot4/springsecurity/ScopedSecurityContextCoordinatorTest.java"
```

---

## 3. Testing Modern Authorization Services

```java
--8<-- "tracks/java25-boot4/modules/11-spring-security/src/test/java/lab/java25boot4/springsecurity/ModernAuthorizationServiceTest.java"
```
