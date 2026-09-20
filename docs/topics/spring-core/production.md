# Spring Core in Production

## 1. Diagnosing Silent AOP Proxy Bypasses

When `@Transactional`, `@Async`, `@Cacheable`, or custom aspect annotations fail silently in production:

### Diagnostic Checklist

1. **Self-Invocation Check**: Verify if the annotated method is called from within the same class via `this.method()`. If so, it bypasses the Spring proxy.
2. **Access Modifier Check**: Standard Spring AOP proxies only intercept **`public`** methods. Annotations on `private`, `protected`, or package-private methods are ignored by default.
3. **`final` Keyword Check**: CGLIB proxies cannot override `final` methods or subclass `final` classes. Any invocation of a `final` method executes without entering the interceptor chain.
4. **Early Lifecycle Call**: Invoking annotated methods inside constructors or `@PostConstruct` occurs before `AbstractAutoProxyCreator` wraps the bean in a proxy.

## 2. Diagnosing Circular Dependency Failures

In Spring Boot 2.6+ and 3.x, circular dependencies trigger fatal startup exceptions by default:

```text
***************************
APPLICATION FAILED TO START
***************************

Description:
The dependencies of some of the beans in the application context form a cycle:
┌─────┐
|  orderService (field private lab.springcore.BillingService lab.springcore.OrderService.billingService)
↑     ↓
|  billingService (field private lab.springcore.OrderService lab.springcore.BillingService.orderService)
└─────┘

Action:
Relying upon circular references is discouraged and they are prohibited by default. Update your application to remove the dependency cycle between beans.
```

### Remediation Strategy
1. **Event Decoupling (Preferred)**: Replace bidirectional service calls with domain event publication via `ApplicationEventPublisher`.
2. **Extract Common Service**: Extract shared operations into a third independent service injected into both dependents.
3. **Lazy Resolution (Temporary)**: Annotate constructor argument with `@Lazy` (creates an on-demand proxy, but does not fix underlying architectural coupling).

## 3. Profiling Spring Application Startup Bottlenecks

If container startup takes too long or triggers Kubernetes startup probe timeouts:

### 1. Enable Startup Tracking
Configure an `ApplicationStartup` flight recorder step:

```bash
java -Dspring.context.checkpoint=on-refresh -jar app.jar
```

### 2. Inspect Slow Bean Instantiation
Analyze Actuator startup endpoint (`/actuator/startup`) to identify beans whose `postProcessBeforeInitialization` or `@PostConstruct` methods take hundreds of milliseconds.

## 4. Production Spring Core Checklist

- [ ] All beans declare dependencies through explicit constructors with `final` fields. No `@Autowired` on private fields.
- [ ] Singleton beans are completely stateless; all request data is passed via method arguments.
- [ ] No blocking I/O (REST, database queries, thread sleeps) is executed inside constructors or `@PostConstruct`.
- [ ] Cross-cutting operations (`@Transactional`, `@Cacheable`, `@Async`, `@Audited`) are invoked across collaborator bean boundaries.
- [ ] Prototype beans consumed by singletons use `ObjectProvider<T>` or `@Lookup`.
- [ ] Event listeners are non-blocking or annotated with `@Async` where appropriate.

## Related

- [Concepts](concepts.md)
- [Questions](questions.md)
- [Solutions](solutions.md)
