# Spring Core Solutions

Production-grade implementations corresponding to the code review exercises.

## Decoupled order and billing

### Implementation

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/circular/OrderCreatedEvent.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/circular/InvoiceProcessedEvent.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/circular/OrderService.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/circular/BillingService.java"
```

### Why it works

1. **Constructor Injection**: All dependencies are declared in constructors with `final` fields, enabling pure unit testing with mock publishers without Spring context.
2. **Event-Driven Decoupling**: Instead of `OrderService` calling `BillingService` and `BillingService` calling `OrderService`, both publish domain event records via `ApplicationEventPublisher` and subscribe via `@EventListener`.
3. **No Circular References**: Eliminates compile-time and runtime dependency cycles.

### Trade-offs

Event decoupling introduces indirect control flow, making call stacks non-linear compared to direct method invocations.

---

## Proxy-safe audit delegation

### Implementation

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/selfinvocation/Audited.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/selfinvocation/AuditAspect.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/selfinvocation/OrderAuditService.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/selfinvocation/OrderProcessingService.java"
```

### Why it works

1. **Collaborator Proxy Boundary**: Moving the `@Audited` method to `OrderAuditService` ensures that calls from `OrderProcessingService` pass through Spring's generated CGLIB/JDK proxy.
2. **Aspect Interceptor Chain Triggered**: `AuditAspect` intercepts every invocation of `recordAudit()`, logging compliance events accurately.

### Trade-offs

Requires creating an additional collaborator class, increasing class count in exchange for reliable proxy interception and clean Single Responsibility Principle (SRP) separation.

---

## Dynamic prototype resolution

### Implementation

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/prototypescope/ExecutionContext.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/prototypescope/ReportGenerator.java"
```

### Why it works

1. **`ObjectProvider<ExecutionContext>`**: Enables lazy, programmatic retrieval of prototype beans. Every call to `contextProvider.getObject()` requests a fresh prototype instance from the Spring container.
2. **Zero Cross-Request State Leakage**: Each execution receives its own isolated context instance, eliminating multithreaded race conditions in singletons.

### Trade-offs

`ObjectProvider.getObject()` incurs minor overhead by querying the `BeanFactory` during method execution, but guarantees correct prototype instantiation.

---

## Non-blocking lifecycle warmup

### Implementation

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/heavyinit/RemoteRateClient.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/heavyinit/ExchangeRateService.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/heavyinit/ExchangeRateWarmupListener.java"
```

### Why it works

1. **Fast Startup with Defaults**: `ExchangeRateService` initializes instantly with safe local defaults during constructor execution without blocking I/O.
2. **Post-Startup Event Warmup**: `ExchangeRateWarmupListener` listens to `ContextRefreshedEvent` to trigger remote rate preloading after the application container is fully healthy.
3. **Thread-Safe Caching**: `ConcurrentHashMap` ensures safe concurrent reads and background updates.

### Trade-offs

Initial requests arriving before the warmup event finishes will serve default rates, but the application is protected against startup failures and probe timeouts.

---

## Stateless singleton calculation

### Implementation

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/mutablesingleton/DiscountRequest.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/mutablesingleton/DiscountResult.java"
```

```java
--8<-- "modules/04-spring-core/src/main/java/lab/springcore/mutablesingleton/DiscountCalculationService.java"
```

### Why it works

1. **Pure Stateless Architecture**: `DiscountCalculationService` maintains no mutable instance fields. All calculation data is passed via immutable records and held on thread-local stack frames.
2. **Thread Safety Under Extreme Concurrency**: Eliminates data races, multi-tenant state leaks, and calculation corruption.

### Trade-offs

Requires passing request parameters explicitly rather than sharing instance variables across helper methods, which improves clarity and thread safety.

## Related

- [Tests](tests.md)
- [Concepts](concepts.md)
- [Production Diagnostics](production.md)
