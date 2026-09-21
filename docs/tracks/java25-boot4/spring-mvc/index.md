# Spring MVC in Spring Boot 4 / Spring Framework 7

!!! info "Delta from baseline"
    Baseline module [`modules/06-spring-mvc`](../../../topics/spring-mvc/index.md) covers Spring MVC foundations: DispatcherServlet flow, HandlerMapping, Controllers, Argument Resolvers, Validation, and Exception Handling on Spring Boot 3.5.
    This track module teaches the **Spring Boot 4.0 / Spring Framework 7 delta**:
    
    - **First-Class Framework API Versioning**: Declarative `@RequestMapping` version mappings and `ApiVersionResolver` matching requests natively without custom interceptors.
    - **JSpecify Null-Safety Integration**: Modern `@NullMarked` and `@Nullable` type-use contracts across controller endpoints, models, and arguments replacing deprecated `@NonNullApi`.
    - **Modern Web Testing**: Spring 7 MVC testing patterns unifying mock and client testing.
    - **Jackson 3 / Serialization Evolution**: Modern JSON contracts, records serialization, and null handling.

---

## 1. The Spring MVC Evolution

| Feature | Baseline (Boot 3.5 / Spring 6) | Track (Boot 4.0 / Spring 7) |
|---|---|---|
| **API Versioning** | Custom interceptor or URL path manipulation | First-class `@RequestMapping` version attributes & `ApiVersionResolver` |
| **Null Safety** | `@NonNullApi` / `@NonNullFields` (Spring annotations) | JSpecify standard (`@NullMarked`, `@Nullable`) |
| **Validation / Constraints** | Bean Validation runtime only | JSpecify static contracts + Bean Validation runtime enforcement |
| **JSON Serialization** | Jackson 2 (`com.fasterxml.jackson.*`) | Jackson 3 ready (`tools.jackson.*`) with modern records integration |
| **Testing** | Separate `MockMvc` and `WebTestClient` APIs | `RestTestClient` unifying mock and HTTP client ergonomics |

---

## 2. Module Roadmap

1. [Concepts](concepts.md) — Native API versioning, JSpecify nullness contracts, and Jackson 3.
2. [Internals](internals.md) — DispatcherServlet version routing, `ApiVersionResolver`, and bytecode nullness attributes.
3. [Interview Questions](questions.md) — 13 questions with runnable examples.
4. [Code Review](code-review.md) — Review targets with hand-rolled versioning and nullness contract breaches.
5. [Solutions](solutions.md) — Step-by-step refactoring with production patterns.
6. [Testing Guide](tests.md) — Testing versioned routes and null-safety contracts in MVC.
7. [Production Scenarios](production.md) — API version sunsetting, deprecation headers, and canary rollouts.
8. [Exercises](exercises.md) — Hands-on katas for migrating legacy MVC versioning.
