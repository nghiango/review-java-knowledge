# Spring Core

## Why this matters

The Spring IoC (Inversion of Control) container powers modern enterprise Java applications. Understanding container mechanics—such as bean lifecycle phases, proxy generation (JDK dynamic vs CGLIB), AOP interceptor chains, scope resolution, and event decoupling—is essential for designing maintainable microservices and avoiding subtle production defects like self-invocation proxy bypasses, circular dependency locks, and singleton state leaks.

## Core Concepts

- [IoC, DI paradigms, bean definitions, scopes, proxies, AOP, and application events](concepts.md)
- [DefaultListableBeanFactory, three-level singleton cache, CGLIB enhancer, and BPP lifecycle](internals.md)

## How it works internally

Follow how Spring creates and initializes beans, resolves dependencies through the 3-level singleton cache, wraps instances in CGLIB/JDK proxies via `BeanPostProcessor`s, and routes method invocations through `ReflectiveMethodInvocation` interceptor chains in [Internals](internals.md).

## Common Interview Questions

The [question bank](questions.md) spans 23 structured questions: 8 Basic, 8 Intermediate, 5 Senior, and 2 Production Scenarios with dedicated runnable code examples.

## Common Production Problems

Circular dependency failures, self-invocation aspect bypassing on `@Transactional`/`@Audited`, prototype scope leakage inside singletons, startup probe timeouts from heavy `@PostConstruct`, and shared mutable singleton state are diagnosed in [Production](production.md).

## Broken Examples

1. [Circular field injection](code-review.md#circular-field-injection)
2. [Self-invocation aspect bypass](code-review.md#self-invocation-aspect-bypass)
3. [Prototype injection in singleton](code-review.md#prototype-injection-in-singleton)
4. [Heavy work in constructor](code-review.md#heavy-work-in-constructor)
5. [Mutable singleton state](code-review.md#mutable-singleton-state)

## Correct Implementations

Each broken exercise maps to tested production-grade implementations in [Solutions](solutions.md) and [Tests](tests.md).

## Trade-offs

Constructor injection provides immutability and testability at the cost of verbose constructors without Lombok; CGLIB proxying supports classes without interfaces but requires non-final classes and non-final methods; event-driven architectures decouple domain services while sacrificing synchronous linear call stack traces.

## Production Checklist

- All Spring components use explicit constructor injection with `final` fields.
- Singleton beans are strictly stateless; per-request data is passed via method parameters.
- Cross-cutting aspects (`@Transactional`, `@Async`, `@Cacheable`, `@Audited`) are invoked across bean proxy boundaries, never via `this` self-invocation.
- Prototype or request-scoped beans in singletons are fetched dynamically using `ObjectProvider<T>` or `@Lookup`.
- Heavy initialization (network calls, DB preloading) is offloaded to `@EventListener(ApplicationReadyEvent.class)`.

## Senior-Level Questions

Explore advanced topics like custom `BeanPostProcessor` proxying, `@Configuration(proxyBeanMethods)` bytecode enhancement, and three-level singleton cache cycle resolution in [Senior Questions](questions.md#senior).

## Exercises

Hands-on Spring IoC katas to practice custom post-processors, event publishing, and proxy mechanics in [Exercises](exercises.md).

## Related

- [Core Java](../core-java/index.md)
- [Concurrency](../concurrency/index.md)
- [Maintainability Issues](../../issues/maintainability.md)
- [Interview Checklist](../../interview-checklist.md)
