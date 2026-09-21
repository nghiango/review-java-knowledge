# REST API Solutions & Production Implementations — Java 25 & Spring Boot 4 Track

!!! info "Delta from baseline"
    Baseline solutions in [`docs/topics/rest-api/solutions.md`](../../../topics/rest-api/solutions.md) cover correct HTTP status usage, `@Valid` record payloads, safe idempotent handlers, and RFC 9457 exception handlers.
    This section presents the production solutions for the **Spring Boot 4.0 / Spring Framework 7 & Java 25 delta**:

---

## Solution 1: Refactoring Ad-Hoc Versioning to Native Declarative Routing

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/10-rest-api/broken-examples/adhoc-header-versioning-trap/SOLUTION.md"
```

### Production Implementation: `NativeVersionedOrderApiController.java`
```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/src/main/java/lab/java25boot4/restapi/NativeVersionedOrderApiController.java"
```

### Production Factory: `ModernRestApiResponseFactory.java`
```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/src/main/java/lab/java25boot4/restapi/ModernRestApiResponseFactory.java"
```

---

## Solution 2: Bounded Declarative HTTP Interface Proxy with ProblemDetail Preservation

### Analysis & Annotated Code
```markdown
--8<-- "tracks/java25-boot4/modules/10-rest-api/broken-examples/declarative-http-interface-leak/SOLUTION.md"
```

### Production Implementation: `ModernHttpInterfaceGatewayClient.java`
```java
--8<-- "tracks/java25-boot4/modules/10-rest-api/src/main/java/lab/java25boot4/restapi/ModernHttpInterfaceGatewayClient.java"
```

---

## Trade-Off Analysis

| Strategy | Advantages | Trade-Offs |
|---|---|---|
| **Declarative Header Versioning** (`headers = "X-API-Version=..."`) | Clear method separation; distinct OpenAPI operation IDs per version; zero manual parsing. | Requires clients to supply custom headers; CDNs require `Vary` header configuration. |
| **Vendor Media-Type Versioning** (`produces = "application/vnd.orders.v2+json"`) | Conforms to strict REST content negotiation; URL remains pure. | Browsers and generic API testers require specific `Accept` configuration. |
| **Bounded Declarative Clients** | Strong compile-time interface guarantees; declarative error decoding; virtual thread safe. | Requires careful timeout configuration per downstream dependency. |
