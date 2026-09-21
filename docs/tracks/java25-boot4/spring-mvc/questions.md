# Interview Questions: Modern Spring MVC in Spring Boot 4

!!! info "Delta from baseline"
    Baseline questions in [`docs/topics/spring-mvc/questions.md`](../../../topics/spring-mvc/questions.md) test Spring MVC fundamentals: DispatcherServlet, Argument Resolvers, Filters vs Interceptors, and ProblemDetail.
    These 13 questions focus on **Spring Boot 4.0 / Spring Framework 7 advancements**: Framework API Versioning, JSpecify nullness contracts, Jackson 3 readiness, and migration strategies.

---

### 1. How does Spring Framework 7 support API versioning natively?

??? question "Reveal answer"
    Spring Framework 7 introduces first-class framework API versioning. Instead of using custom interceptors, filter URL rewriting, or path prefixes, endpoints declare version criteria directly on `@RequestMapping` or via header/parameter attributes.
    
    The framework's `ApiVersionResolver` inspects incoming requests (via headers, parameters, or media types), and `RequestMappingHandlerMapping` matches the request to the matching method handler. Unsupported versions automatically produce standard RFC 9457 `ProblemDetail` errors.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q01NativeApiVersioningBasicsExample.java"
        ```

---

### 2. What is JSpecify and why did Spring Boot 4 adopt it over Spring's `@NonNullApi`?

??? question "Reveal answer"
    JSpecify is an industry-standard Java nullness specification supported across IDEs, compilers, Kotlin, and static analysis tools (e.g. NullAway, Checker Framework).
    
    Spring Boot 4 adopts JSpecify (`org.jspecify.annotations.*`) and deprecates proprietary Spring annotations (`@NonNullApi`, `@NonNullFields`). JSpecify uses `TYPE_USE` target annotations, allowing precise nullability contracts on generics (e.g. `List<@Nullable String>`), records, and method return values.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q02JSpecifyNullMarkedBasicsExample.java"
        ```

---

### 3. How should optional request parameters be declared in a `@NullMarked` controller package?

??? question "Reveal answer"
    In a `@NullMarked` package, all reference types are strictly non-null by default. If a controller method uses `@RequestParam(required = false)`, Spring injects `null` when the parameter is absent.
    
    The parameter must be explicitly annotated with `@Nullable` (e.g., `@RequestParam(required = false) @Nullable String title`). Omitting `@Nullable` violates the JSpecify contract and generates compiler or static analysis warnings.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q03NullableParameterHandlingExample.java"
        ```

---

### 4. How does Spring MVC handle unsupported or missing API versions with RFC 9457?

??? question "Reveal answer"
    When a request specifies an invalid or deprecated API version, or omits a required version identifier, Spring MVC generates a standard RFC 9457 `ProblemDetail` response with HTTP status `406 Not Acceptable` or `400 Bad Request`. The payload details the rejected version and lists currently supported versions in its extension properties.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q04ProblemDetailApiVersionExample.java"
        ```

---

### 5. What are the common API versioning strategies supported by Spring MVC?

??? question "Reveal answer"
    Spring MVC supports three primary version resolution strategies via `ApiVersionResolver`:
    1. **HTTP Header**: E.g., `X-API-Version: 2.0`. Keeps URIs clean and uniform across versions.
    2. **Query Parameter**: E.g., `/api/orders?version=2.0`. Simple for browser testing.
    3. **Content Negotiation (Accept Header)**: E.g., `Accept: application/vnd.company.order.v2+json`. Adheres strictly to REST hypermedia principles.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q05VersionResolutionStrategyExample.java"
        ```

---

### 6. How does `@NullMarked` in `package-info.java` affect Spring MVC controller contracts?

??? question "Reveal answer"
    Placing `@NullMarked` in `package-info.java` establishes a non-null default across all classes, records, and methods in that package.
    - Handler methods returning DTOs promise that all unannotated fields are non-null.
    - Argument resolvers treat unannotated parameters as mandatory non-null inputs.
    - Any field or parameter capable of holding `null` must be explicitly annotated with `@Nullable`.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q06JSpecifyPackageInfoContractExample.java"
        ```

---

### 7. How do you migrate from `@NonNullApi` to JSpecify in Spring Boot 4?

??? question "Reveal answer"
    Migration steps:
    1. Replace `import org.springframework.lang.NonNullApi;` with `import org.jspecify.annotations.NullMarked;` in `package-info.java`.
    2. Replace `org.springframework.lang.Nullable` with `org.jspecify.annotations.Nullable` across all classes and DTOs.
    3. Run compilation with strict null-checking enabled (e.g. NullAway) to detect newly revealed contract violations.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q07DeprecatedNonNullApiMigrationExample.java"
        ```

---

### 8. How do JSpecify nullness annotations interact with Jakarta Bean Validation (`@NotNull`)?

??? question "Reveal answer"
    They operate at two distinct lifecycle phases:
    - **JSpecify (`@NullMarked`, `@Nullable`)**: Compile-time static contract. Used by compilers, IDEs, and static analysis tools to prevent null-pointer bugs during development.
    - **Bean Validation (`@NotNull`, `@NotBlank`)**: Runtime constraint validation. Executed dynamically by Hibernate Validator when a JSON payload is deserialized into a controller parameter annotated with `@Valid`.
    Both should coexist in robust production applications.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q08ValidationJSpecifyCoexistenceExample.java"
        ```

---

### 9. How do you migrate a legacy custom version interceptor to Spring Framework 7?

??? question "Reveal answer"
    Migration steps:
    1. Delete the custom `HandlerInterceptor` that extracts and validates the version header.
    2. Remove custom request attribute storage (`request.setAttribute("version", ...)`).
    3. Split polymorphic or `if-else` controller methods into distinct handler methods annotated with specific version constraints (e.g. `headers = "X-API-Version=2.0"`).
    4. Provide strongly-typed request and response DTO records for each version.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q09MigrationHandRolledVersioningToFrameworkExample.java"
        ```

---

### 10. What are the key breaking changes when migrating to Jackson 3 in Spring Boot 4?

??? question "Reveal answer"
    Jackson 3 introduces major modernization:
    1. **Package Namespace**: Moved from `com.fasterxml.jackson.*` to `tools.jackson.*`.
    2. **Immutability**: `ObjectMapper` is replaced by immutable `JsonMapper` built using builder patterns.
    3. **Record Support**: First-class support for Java records without needing special parameter-name reflection modules.
    4. **Module SPI**: Custom Jackson 2 `Module` and serializer implementations must be ported to the Jackson 3 API.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q10MigrationJackson2ToJackson3ContractExample.java"
        ```

---

### 11. What is `RestTestClient` and how does it improve testing in Spring Framework 7?

??? question "Reveal answer"
    `RestTestClient` in Spring Framework 7 unifies web layer testing:
    - Provides a single fluent, assertion-rich client API for both unit/mock testing and real HTTP integration testing.
    - Bridges the gap between legacy `MockMvc` (mock servlet environment) and `WebTestClient` (reactive client).
    - Eliminates the need to maintain duplicate test configurations across unit and integration test suites.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q11RestTestClientVsMockMvcExample.java"
        ```

---

### 12. How can upgrading to `@NullMarked` introduce subtle runtime contract bugs?

??? question "Reveal answer"
    When adding `@NullMarked` to an existing package, any unannotated method return type implicitly promises to never return null. If legacy implementation code returns `null` for missing database records or optional fields without `@Nullable`, callers compiled against the new contract will skip null checks, leading to downstream `NullPointerException`s at runtime.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q12ContractBreaksWithNullMarkedExample.java"
        ```

---

### 13. A production service must decommission an older API version. How do you implement zero-downtime deprecation?

??? question "Reveal answer"
    Implementation steps:
    1. **Publish Sunset Headers**: Configure older version handlers to return RFC 8594 `Deprecation: true` and `Sunset: <date>` HTTP headers.
    2. **Link Migration Documentation**: Include `Link: <url>; rel="successor-version"` headers pointing to upgrade guides.
    3. **Observe Traffic Drop**: Monitor Micrometer `http.server.requests` metrics tagged with `version` until legacy traffic drops to zero.
    4. **Return 410 Gone**: Replace decommissioned handlers with a `ProblemDetail` 410 Gone response before eventual removal.

    ??? example "Example"
        ```java
        --8<-- "tracks/java25-boot4/modules/06-spring-mvc/src/examples/java/lab/java25boot4/springmvc/questions/Q13ScenarioApiVersionDeprecationSunsetExample.java"
        ```
