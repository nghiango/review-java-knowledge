# Spring Core Tests

## Spring Container Testing Strategy

Testing Spring Core components emphasizes fast, isolated unit tests paired with focused `AnnotationConfigApplicationContext` tests:

1. **Pure Unit Testing with Constructor Injection**: Tests instantiate services directly using standard Java `new` constructors and mock collaborators without loading a heavy Spring context.
2. **Targeted Container Context Tests**: Minimal `AnnotationConfigApplicationContext` instances are spun up within try-with-resources blocks to verify bean lifecycles, event publishing, and AOP proxies in isolation.
3. **Multi-Threaded Concurrency Assertions**: Verifies thread safety and isolation across concurrent execution using `CountDownLatch` and `ExecutorService`.

## Test Suite Overview

```bash
./gradlew :modules:04-spring-core:test
```

| Test Class | Invariant Proved | Technique |
|---|---|---|
| `CircularDependencyDecouplingTest` | Circular dependency resolved via event publisher without cycles | `AnnotationConfigApplicationContext` event dispatching |
| `SelfInvocationProxyAspectTest` | Aspect advice executed when calls cross collaborator proxy boundary | `@EnableAspectJAutoProxy`, `@Around` verification |
| `PrototypeInSingletonTest` | Fresh prototype instance generated for each method execution | `ObjectProvider.getObject()` assertion |
| `ExchangeRateServiceInitTest` | Instant container startup with safe defaults and warmup on event | `ContextRefreshedEvent` verification |
| `DiscountCalculationServiceTest` | Stateless singleton isolation under multi-threaded concurrency | 16 parallel threads, 8,000 requests |

## Key Test Snippets

### Proving event-driven circular decoupling

```java
--8<-- "modules/04-spring-core/src/test/java/lab/springcore/circular/CircularDependencyDecouplingTest.java"
```

### Proving AOP proxy interception across collaborator boundary

```java
--8<-- "modules/04-spring-core/src/test/java/lab/springcore/selfinvocation/SelfInvocationProxyAspectTest.java"
```

### Proving stateless singleton thread safety under load

```java
--8<-- "modules/04-spring-core/src/test/java/lab/springcore/mutablesingleton/DiscountCalculationServiceTest.java"
```

## Related

- [Solutions](solutions.md)
- [Code Review](code-review.md)
- [Production](production.md)
