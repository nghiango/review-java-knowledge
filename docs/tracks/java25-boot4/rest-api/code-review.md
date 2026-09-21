# REST API Code Review — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline code review targets in [`docs/topics/rest-api/code-review.md`](../../../topics/rest-api/code-review.md) address GET mutating state, 200 OK for errors, entity exposure, and unbounded list queries.
    This review section examines **Spring Boot 4.0 / Spring Framework 7 & Java 25 review targets**:
    
    1. **Ad-Hoc Header Versioning & Missing Lifecycle Headers**: Imperative header branching inside controller methods omitting RFC 8594 `Sunset` headers and throwing unhandled exceptions.
    2. **Declarative HTTP Interface Proxy Leak**: Outdated manual factory setup with unbounded socket timeouts and swallowed downstream RFC 9457 Problem Details.

---

## Review Target 1: Ad-Hoc Header Versioning & Missing Lifecycle Headers

### Context
A pull request introduces a new version of the Order REST endpoint to support multi-currency pricing and updated schemas while maintaining backward compatibility with legacy clients.

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/broken-examples/adhoc-header-versioning-trap/OrderResourceController.java"
```

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/broken-examples/adhoc-header-versioning-trap/LegacyOrderDto.java"
```

??? tip "Review Guidance"
    - Examine how invalid or unsupported version headers are handled. What HTTP status code and payload structure will clients receive?
    - Are deprecated representations signaling their lifecycle state via RFC 8594 headers?
    - How does mixing multiple representation models and imperative dispatch in a single method impact OpenAPI documentation and maintainability?

---

## Review Target 2: Declarative HTTP Interface Proxy Configuration & Leaks

### Context
A developer configured an external payment gateway client using Spring Framework `@HttpExchange` declarative interfaces.

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/broken-examples/declarative-http-interface-leak/PaymentGatewayClient.java"
```

```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/broken-examples/declarative-http-interface-leak/PaymentOrchestratorService.java"
```

??? tip "Review Guidance"
    - Are connect and socket read timeouts configured on the HTTP client adapter?
    - What happens to remote RFC 9457 `ProblemDetail` responses when payment processing fails?
    - How does manual `HttpServiceProxyFactory` instantiation compare to declarative Spring Boot 4 client registration?
