# Spring Core Interview Questions

Four levels of interview questions covering Inversion of Control, Dependency Injection, bean lifecycles, scopes, proxies, Spring AOP, event decoupling, and container internals.

<!-- --8<-- [start:basic] -->
## Basic

### 1. What is Inversion of Control (IoC) and how does Dependency Injection (DI) relate to it?

??? question "Reveal answer"
    **Inversion of Control (IoC)** is a design principle where the control of object creation, configuration, and lifecycle management is delegated to an external container or framework rather than being hardcoded in application logic.
    
    **Dependency Injection (DI)** is the primary design pattern used to implement IoC: the container supplies dependent objects to a class (via constructor, setter, or factory method) rather than the class constructing them internally using `new`.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q01IocVsDiExample.java"
        ```

### 2. Compare Constructor Injection, Setter Injection, and Field Injection. Why is Constructor Injection preferred?

??? question "Reveal answer"
    - **Constructor Injection**: Injects dependencies via constructor arguments. Fields can be declared `final`, guaranteeing immutability and complete initialization. Enables fast unit testing with standard `new` without Spring context.
    - **Setter Injection**: Injects via public setters. Useful for optional dependencies or post-construction reconfiguration.
    - **Field Injection (`@Autowired` on private fields)**: Anti-pattern. Hides dependencies, bypasses immutability, complicates unit testing without reflection, and masks circular dependencies.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q02ConstructorVsFieldInjectionExample.java"
        ```

### 3. What is the difference between `ApplicationContext` and `BeanFactory` in Spring?

??? question "Reveal answer"
    `BeanFactory` is the root interface providing basic IoC and DI capabilities with lazy bean initialization.
    
    `ApplicationContext` extends `BeanFactory` and provides enterprise-level features: eager pre-instantiation of singleton beans at startup, integrated Spring AOP support, `ApplicationEvent` publishing, message resource resolution for i18n, and environment profile configuration.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q03ApplicationContextVsBeanFactoryExample.java"
        ```

### 4. What are the standard Spring stereotype annotations and how do they differ?

??? question "Reveal answer"
    - `@Component`: Generic stereotype for any Spring-managed component.
    - `@Service`: Specialization of `@Component` indicating business logic in the service layer.
    - `@Repository`: Specialization indicating data access objects (DAOs), providing automatic translation of database-specific exceptions into Spring's `DataAccessException` hierarchy.
    - `@Controller` / `@RestController`: Specialization for Spring MVC web presentation layer components handling HTTP requests.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q04SpringStereotypesExample.java"
        ```

### 5. When should you use `@Bean` versus `@Component`?

??? question "Reveal answer"
    - `@Component`: Applied directly on classes you author within your codebase for automatic discovery via component scanning.
    - `@Bean`: Applied on methods inside `@Configuration` classes to explicitly instantiate and configure third-party library classes (e.g. `ObjectMapper`, AWS SDK clients, `RestTemplate`) whose source code cannot be annotated directly.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q05BeanVsComponentExample.java"
        ```

### 6. What are the standard Spring bean scopes and how do `singleton` and `prototype` differ?

??? question "Reveal answer"
    - **`singleton` (Default)**: A single shared instance is created per Spring `ApplicationContext`. All injections reference the same object.
    - **`prototype`**: A new independent instance is created every time the bean is requested from the container (`getBean()` or provider).
    - Web scopes: `request`, `session`, `application`, and `websocket` bound to HTTP/WebSocket lifecycles.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q06SingletonVsPrototypeExample.java"
        ```

### 7. What are the primary phases of the Spring Bean Lifecycle?

??? question "Reveal answer"
    1. **Instantiation**: Constructor invocation.
    2. **Populate Properties**: Inject dependencies and configuration values.
    3. **Aware Callbacks**: `BeanNameAware`, `BeanFactoryAware`, `ApplicationContextAware`.
    4. **Initialization**: `BeanPostProcessor.postProcessBeforeInitialization()`, `@PostConstruct` / `InitializingBean.afterPropertiesSet()`, `BeanPostProcessor.postProcessAfterInitialization()`.
    5. **Destruction**: On container shutdown, `@PreDestroy` / `DisposableBean.destroy()`.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q07BeanLifecyclePhasesExample.java"
        ```

### 8. How does event-driven communication work in Spring Core?

??? question "Reveal answer"
    Spring provides loosely-coupled event messaging:
    - **Event**: A custom payload record or class.
    - **Publisher**: Injects `ApplicationEventPublisher` and invokes `publisher.publishEvent(event)`.
    - **Listener**: Any bean method annotated with `@EventListener` that accepts the event type as a parameter. By default, events are processed synchronously in the publisher's thread.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q08ApplicationEventsExample.java"
        ```
<!-- --8<-- [end:basic] -->

<!-- --8<-- [start:intermediate] -->
## Intermediate

### 9. What is the difference between `BeanPostProcessor` and `BeanFactoryPostProcessor`?

??? question "Reveal answer"
    - **`BeanFactoryPostProcessor` (BFPP)**: Operates on `BeanDefinition` metadata **before** any bean instances are created. Used for modifying property values or reading configuration (e.g. `PropertySourcesPlaceholderConfigurer`, `ConfigurationClassPostProcessor`).
    - **`BeanPostProcessor` (BPP)**: Operates on actual bean instances **after** instantiation, before and after initialization. Used for scanning annotations, injecting custom proxies (e.g. AOP proxy creation), and initializing wrappers.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q09BppVsBfppExample.java"
        ```

### 10. What does `@Configuration(proxyBeanMethods = true)` do versus `proxyBeanMethods = false` (Lite mode)?

??? question "Reveal answer"
    - `proxyBeanMethods = true` (default): Spring enhances the `@Configuration` class at runtime using CGLIB. Direct inter-method calls between `@Bean` methods (e.g. `beanA()` calling `beanB()`) are intercepted to return the existing singleton instance from the container rather than invoking the method body multiple times.
    - `proxyBeanMethods = false` (Lite mode): Disables CGLIB subclassing for faster startup and lower memory. Inter-method `@Bean` calls execute as standard Java method calls, creating multiple distinct instances unless dependencies are injected as parameters.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q10ProxyBeanMethodsExample.java"
        ```

### 11. How do JDK Dynamic Proxies differ from CGLIB Proxies in Spring?

??? question "Reveal answer"
    - **JDK Dynamic Proxies**: Created via `java.lang.reflect.Proxy`. Requires target classes to implement an interface; only methods declared in the interface are proxied.
    - **CGLIB Proxies**: Created via bytecode enhancement subclassing the target class at runtime. Does not require interfaces. Cannot proxy `final` classes or `final` methods. Spring Boot 2.x+ and 3.x use CGLIB by default (`spring.aop.proxy-target-class=true`).

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q11JdkDynamicVsCglibProxyExample.java"
        ```

### 12. What is the Spring AOP self-invocation problem and how is it resolved?

??? question "Reveal answer"
    Spring AOP generates runtime proxies around beans. When method `A()` in `Service` calls method `B()` in the same class (`this.B()`), execution remains inside the target instance and never passes through the proxy interceptor chain. Consequently, `@Transactional`, `@Async`, or custom `@Aspect` annotations on `B()` are silently ignored.
    
    **Resolution**: Refactor method `B()` into a dedicated collaborator bean, use `ObjectProvider<Service>` for self-injection, or adopt compile-time/load-time AspectJ weaving.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q12SelfInvocationProxyBypassExample.java"
        ```

### 13. What problem arises when injecting a Prototype bean into a Singleton bean, and how do you resolve it?

??? question "Reveal answer"
    **Problem**: A singleton bean is instantiated and wired only once at startup. If a prototype bean is directly injected into a singleton constructor or field, Spring resolves it once during startup, causing the singleton to reuse the same prototype instance forever.
    
    **Resolution**:
    1. Inject `ObjectProvider<PrototypeBean>` and call `provider.getObject()` on demand.
    2. Use method injection with `@Lookup`.
    3. Configure `@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)`.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q13PrototypeInSingletonLookupExample.java"
        ```

### 14. How does Spring resolve circular dependencies and why does constructor injection fail on cycles?

??? question "Reveal answer"
    Spring uses a **three-level cache** in `DefaultSingletonBeanRegistry` to resolve setter/field circular dependencies by exposing a partially initialized early instance/factory before property injection.
    
    With **constructor injection**, an instance cannot be created until its constructor parameters are resolved. If Bean A needs Bean B in its constructor, and Bean B needs Bean A in its constructor, neither can be instantiated first, throwing `BeanCurrentlyInCreationException`.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q14CircularDependencyResolutionExample.java"
        ```

### 15. How do custom `@Conditional` annotations and `Condition` evaluations work in Spring?

??? question "Reveal answer"
    Classes implement `org.springframework.context.annotation.Condition` and override `matches(ConditionContext, AnnotatedTypeMetadata)`. Spring evaluates the condition during configuration class parsing; if `matches()` returns `false`, the `@Bean` or `@Component` is skipped and excluded from the `ApplicationContext`.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q15CustomConditionalEvaluationExample.java"
        ```

### 16. How does the Spring `Environment` hierarchy and profile resolution work?

??? question "Reveal answer"
    Spring's `Environment` manages **Profiles** (active/default profiles for conditional bean activation via `@Profile`) and **Properties** from layered `PropertySource`s (JVM system properties, OS environment variables, application properties/YAML). Properties are resolved following strict precedence order from highest to lowest priority sources.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q16EnvironmentPropertySourceExample.java"
        ```
<!-- --8<-- [end:intermediate] -->

<!-- --8<-- [start:senior] -->
## Senior

### 17. How do Pointcut expressions, `@Around` ProceedingJoinPoint, and `@Order` precedence work in Spring AOP?

??? question "Reveal answer"
    - **Pointcut**: Defines execution match rules (`execution(* com.example.service..*.*(..))`, `@annotation(...)`).
    - **`@Around`**: Surrounds target method execution. The advice must explicitly invoke `joinPoint.proceed()` to yield control to subsequent interceptors and the target method.
    - **`@Order`**: Controls aspect chaining order. Lower order numbers have higher precedence, executing first upon entering the chain and last upon exiting.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q17AopPointcutAdviceOrderingExample.java"
        ```

### 18. How do you implement a custom `BeanPostProcessor` to dynamically wrap beans in runtime proxies?

??? question "Reveal answer"
    Implement `BeanPostProcessor` and override `postProcessAfterInitialization(Object bean, String beanName)`. Inspect the bean class or its methods for custom annotations; if matching, return a `Proxy.newProxyInstance(...)` (JDK) or `Enhancer.create(...)` (CGLIB) wrapping the target instance.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q18CustomBeanPostProcessorProxyExample.java"
        ```

### 19. How do Scoped Proxies (`ScopedProxyMode.TARGET_CLASS`) work under the hood?

??? question "Reveal answer"
    When a bean is marked with `ScopedProxyMode.TARGET_CLASS`, Spring generates a CGLIB proxy at container startup and injects the proxy into singletons. When a method is called on the proxy, its method interceptor resolves the active target instance dynamically from the current thread/session context and delegates the call.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q19ScopedProxiesTargetClassExample.java"
        ```

### 20. How does `@TransactionalEventListener` differ from standard `@EventListener`?

??? question "Reveal answer"
    Standard `@EventListener` executes immediately and synchronously in the publisher's thread.
    
    `@TransactionalEventListener` binds to the ongoing Spring database transaction and defers execution to a specific `TransactionPhase` (e.g. `AFTER_COMMIT`, `AFTER_ROLLBACK`, `BEFORE_COMMIT`), ensuring external side-effects (e.g. sending emails or publishing Kafka messages) only fire if the database transaction commits successfully.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q20TransactionalEventListenerPhasesExample.java"
        ```

### 21. How does `DefaultListableBeanFactory` register and merge `RootBeanDefinition` instances?

??? question "Reveal answer"
    Spring parses configuration into generic `BeanDefinition`s and registers them by name in `DefaultListableBeanFactory`'s internal `beanDefinitionMap`. During instantiation, generic definitions are merged into `RootBeanDefinition`s, resolving parent-child inheritance hierarchies, overridden constructor arguments, and computed factory method metadata.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q21DefaultListableBeanFactoryInternalsExample.java"
        ```
<!-- --8<-- [end:senior] -->

<!-- --8<-- [start:scenarios] -->
## Scenarios

### 22. Production incident: An `@Audited` or `@Transactional` annotation is silently ignored in production. How do you diagnose and fix it?

??? question "Reveal answer"
    **Symptom**: Transactions do not roll back or audit records are missing, but no exceptions are thrown.
    
    **Root Cause**: The annotated method is called from within the same class via self-invocation (`this.method()`). The call bypasses Spring's CGLIB proxy interceptor chain.
    
    **Fix**: Move the transactional/audited method to a dedicated collaborator bean and inject it via constructor, ensuring calls pass through the generated proxy.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q22SelfInvocationAspectFailureScenarioExample.java"
        ```

### 23. Production incident: Spring Boot application crashes during startup due to health check / Kubernetes liveness timeout. How do you diagnose and fix it?

??? question "Reveal answer"
    **Symptom**: Pod startup exceeds probe timeout or fails with `ApplicationContextException` on startup.
    
    **Root Cause**: Heavy blocking network calls, external database queries, or unmanaged thread creation executed inside a bean constructor or `@PostConstruct` method.
    
    **Fix**: Remove blocking I/O from bean initialization. Use `@EventListener(ApplicationReadyEvent.class)` or asynchronous warmup tasks to preload caches after the container has started successfully.

    ??? example "Example"
        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q23StartupBlockingInitScenarioExample.java"
        ```
<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
