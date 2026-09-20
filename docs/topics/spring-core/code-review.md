# Spring Core Code Review

Review each clean source before expanding its answer.

## Circular field injection

An ordering and billing component fails under modern Spring Boot startup and resists isolated unit testing.

```java
--8<-- "modules/04-spring-core/broken-examples/circular-field-injection/OrderService.java"
```

```java
--8<-- "modules/04-spring-core/broken-examples/circular-field-injection/BillingService.java"
```

Consider injection paradigms, circular dependency graph cycles, testability, and event-driven decoupling.

??? warning "Reveal issues"
    **Maintainability issue — field injection:** `@Autowired` on private fields hides dependencies, prevents `final` immutable fields, and makes unit testing impossible without reflection or Spring runners.

    **Architecture issue — bidirectional circular dependency:** `OrderService` and `BillingService` directly depend on each other, creating tight coupling rejected by Spring Boot by default.

    **Architecture fix — event decoupling:** Decouple order placement and invoice charging using Spring `ApplicationEventPublisher` and `@EventListener`.

[Correct implementation](solutions.md#decoupled-order-and-billing)

---

## Self-invocation aspect bypass

A critical compliance audit aspect is configured on order processing, but no audit records appear in logs.

```java
--8<-- "modules/04-spring-core/broken-examples/self-invocation-aspect-bypass/Audited.java"
```

```java
--8<-- "modules/04-spring-core/broken-examples/self-invocation-aspect-bypass/AuditAspect.java"
```

```java
--8<-- "modules/04-spring-core/broken-examples/self-invocation-aspect-bypass/OrderProcessingService.java"
```

Consider Spring AOP proxy architecture, caller entry points, and internal method invocation on `this`.

??? warning "Reveal issues"
    **Architecture issue — self-invocation proxy bypass:** `processOrder()` calls `auditOrder()` directly on `this`. Execution does not pass through the Spring CGLIB proxy, so `@Audited` advice is silently skipped.

    **Reliability issue — missing compliance audit logs:** Critical business audit records are dropped without error.

    **Architecture fix — dedicated collaborator bean:** Move `auditOrder()` into a separate bean (`OrderAuditService`) and inject it into `OrderProcessingService`.

[Correct implementation](solutions.md#proxy-safe-audit-delegation)

---

## Prototype injection in singleton

An execution context intended to be fresh per report generation request leaks request IDs across concurrent users.

```java
--8<-- "modules/04-spring-core/broken-examples/prototype-injection-singleton/ExecutionContext.java"
```

```java
--8<-- "modules/04-spring-core/broken-examples/prototype-injection-singleton/ReportGenerator.java"
```

Consider singleton vs prototype scope resolution timing and concurrency in singleton beans.

??? warning "Reveal issues"
    **Scope issue — singleton-prototype mismatch:** Injecting `@Scope("prototype")` into a singleton constructor resolves the prototype only once at startup. The singleton reuses the same instance forever.

    **Concurrency issue — shared mutable context:** Concurrent callers overwrite fields on the single shared `ExecutionContext` instance, causing data races and cross-request state pollution.

    **Scope fix — ObjectProvider / @Lookup:** Inject `ObjectProvider<ExecutionContext>` to dynamically instantiate a fresh prototype instance per method execution.

[Correct implementation](solutions.md#dynamic-prototype-resolution)

---

## Heavy work in constructor

A microservice takes minutes to boot and fails Kubernetes startup readiness checks.

```java
--8<-- "modules/04-spring-core/broken-examples/heavy-work-in-constructor/RemoteRateClient.java"
```

```java
--8<-- "modules/04-spring-core/broken-examples/heavy-work-in-constructor/ExchangeRateService.java"
```

Consider bean lifecycle timing, blocking network calls in `@PostConstruct`, and unmanaged thread creation.

??? warning "Reveal issues"
    **Reliability issue — blocking remote I/O during startup:** Executing slow HTTP calls inside `@PostConstruct` delays `ApplicationContext` readiness and causes startup probe timeouts.

    **Resource management issue — unmanaged background thread:** Spawning `new Thread()` inside bean initialization escapes Spring lifecycle management and prevents graceful shutdown.

    **Concurrency issue — plain `HashMap` updated in background:** Reader threads accessing `rates` race with background update writes on unsynchronized collection.

[Correct implementation](solutions.md#non-blocking-lifecycle-warmup)

---

## Mutable singleton state

A promotional discount calculator yields incorrect discounts and leaks customer IDs under multi-threaded load.

```java
--8<-- "modules/04-spring-core/broken-examples/mutable-singleton-state/DiscountCalculationService.java"
```

Consider Spring singleton concurrency models, stateful fields in services, and stateless parameter passing.

??? warning "Reveal issues"
    **Concurrency issue — mutable instance fields in singleton:** Spring `@Service` beans are shared singletons. Storing per-request customer state in instance fields causes concurrent requests to overwrite each other's data.

    **Security issue — cross-tenant customer ID leakage:** A customer's ID and calculation state are exposed to concurrent sessions.

    **Design fix — pure stateless methods:** Pass all request state as method parameters or immutable records, keeping state local to the execution stack.

[Correct implementation](solutions.md#stateless-singleton-calculation)

## Related

- [Solutions](solutions.md)
- [Tests](tests.md)
- [Concepts](concepts.md)
