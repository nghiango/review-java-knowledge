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

### 24. What is the execution contract of BeanPostProcessor lifecycle hooks and early initialization hazards?

??? question "Reveal answer"

    **Short Answer:** `BeanPostProcessor` (BPP) methods wrap bean creation: `postProcessBeforeInitialization` executes before custom init methods (`@PostConstruct`, `InitializingBean`), and `postProcessAfterInitialization` executes after init, typically creating AOP dynamic proxies.

    **Internal Mechanism:** BPPs are instantiated very early in container startup. If a BPP injects standard application beans in its constructor or fields, it forces those target beans to be instantiated prematurely, bypassing subsequent BPP enhancements (such as autowiring, validation, and transactional proxy wrapping).

    **Common Mistake:** Injecting domain services or repositories directly into custom `BeanPostProcessor` implementations, leading to unproxied "raw" beans that silently bypass `@Transactional` or `@Async` aspects. [Concepts](/topics/spring-core/concepts.md#4-the-spring-bean-lifecycle)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q24BeanPostProcessorLifecycleExample.java"
        ```

### 25. How does ConfigurationCondition evaluate ConfigurationPhase (PARSE_CONFIGURATION vs REGISTER_BEAN)?

??? question "Reveal answer"

    **Short Answer:** `ConfigurationCondition` controls *when* condition checks evaluate: `PARSE_CONFIGURATION` evaluates while parsing `@Configuration` classes before bean definitions exist, while `REGISTER_BEAN` evaluates during bean registration when other bean definitions can be inspected.

    **Internal Mechanism:** Using `REGISTER_BEAN` allows conditions to query the `BeanFactory` to check whether specific bean definitions already exist (such as `@ConditionalOnBean`), avoiding false negative evaluation results during the initial configuration parsing phase.

    **Common Mistake:** Using a standard `Condition` to check for other bean definitions during configuration parsing, which fails because candidate bean definitions have not been registered yet. [Concepts](/topics/spring-core/concepts.md#1-inversion-of-control-ioc-dependency-injection-di)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q25ConditionalPhaseEvaluationExample.java"
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

### 26. How does Spring's three-level singleton cache resolve circular dependencies and why does constructor injection prevent them?

??? question "Reveal answer"

    **Short Answer:** `DefaultSingletonBeanRegistry` uses three cache levels: `singletonObjects` (ready beans), `earlySingletonObjects` (instantiated beans exposed early), and `singletonFactories` (early reference/proxy factories). Constructor injection fails on cycles because instance instantiation itself cannot complete without resolving the dependent argument.

    **Deep Explanation:** With setter/field injection, Spring instantiates Bean A with its default constructor, places an `ObjectFactory` for A in the 3rd level cache, and proceeds to populate A's properties. When Bean B needs A, B resolves A's early reference from the 3rd level cache (promoting it to the 2nd level) and completes injection. With constructor injection, neither A nor B can be instantiated first, causing `BeanCurrentlyInCreationException`.

    **Internal Mechanism:** The 3rd level factory cache allows `SmartInstantiationAwareBeanPostProcessor` to generate early AOP proxies if necessary before full property injection.

    **Example:** [Circular dependency resolution](/topics/spring-core/concepts.md#4-the-spring-bean-lifecycle).

    **Common Mistake:** Relying on `@Lazy` or setter injection to hide circular dependencies instead of refactoring tangled domains into unidirectional dependencies or event-driven patterns.

    **Production Consideration:** Constructor injection is preferred because it makes circular dependencies fail fast at startup and enforces immutability via `final` fields.

    **Follow-up Questions:**
    - How does the Dependency Inversion Principle prevent circular relationships in domain models? See [Design Patterns: Dependency Inversion](/topics/design-patterns/questions.md)
    - How does Spring Boot 3 disable circular references by default via `spring.main.allow-circular-references=false`? See [Spring Boot: Configuration Properties](/topics/spring-boot/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q26CircularDependencyResolutionExample.java"
        ```

### 27. What are the mechanical limitations of CGLIB and JDK dynamic proxies regarding final methods and object identity?

??? question "Reveal answer"

    **Short Answer:** CGLIB generates runtime subclasses, so `final` classes cannot be proxied and `final` methods cannot be overridden or intercepted. JDK Dynamic Proxies require interfaces and cannot proxy concrete classes. Proxies are distinct wrapper instances, so `proxy == target` returns `false`.

    **Deep Explanation:** When a caller invokes a `final` method on a CGLIB proxy, the JVM cannot dispatch through the generated subclass interceptor; execution falls directly through to the uninitialized proxy state or target method without triggering `@Transactional` or security advice.

    **Internal Mechanism:** CGLIB uses bytecode generation (via ByteBuddy/ASM) to extend the target class and route method calls through method interceptors (`MethodInterceptor.intercept`).

    **Example:** [Proxy mechanisms](/topics/spring-core/concepts.md#5-proxy-mechanisms-aop).

    **Common Mistake:** Declaring business methods as `final` in Spring services and expecting transaction management or method security aspects to execute.

    **Production Consideration:** Keep service classes and methods non-final when relying on CGLIB proxies, or use AspectJ compile-time/load-time weaving (LTW) when proxy limitations are unacceptable.

    **Follow-up Questions:**
    - How does self-invocation bypass transactional interceptors regardless of proxy type? See [Spring Transactions: Proxy Bypass](/topics/spring-transactions/questions.md#1-how-does-springs-transactional-annotation-work-under-the-hood-and-why-does-self-invocation-bypass-it)
    - What differences exist between Java interface reflection and bytecode subclassing? See [JVM: Bytecode Execution](/topics/jvm/questions.md#2-what-is-java-bytecode-and-how-does-the-jvm-execute-it)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q27ProxyMechanismsAndBypassingExample.java"
        ```

### 28. How does Spring Framework 6 Ahead-Of-Time (AOT) engine compute RuntimeHints for GraalVM native images?

??? question "Reveal answer"

    **Short Answer:** In GraalVM native compilation, closed-world analysis removes unreferenced reflection, dynamic proxies, and resource files. Spring 6 AOT evaluates application configurations at build time and generates explicit `RuntimeHints` for reflection and proxy generation.

    **Deep Explanation:** Standard JVM applications dynamically discover classes and invoke reflection at runtime. GraalVM AOT native compiler requires pre-declaring all reflection targets, serialization types, and JDK proxies during native image build time.

    **Internal Mechanism:** Spring 6's AOT engine scans bean definitions, runs `BeanFactoryInitializationAotProcessor` implementations, and invokes registered `RuntimeHintsRegistrar` classes to generate `reflect-config.json` and `proxy-config.json`.

    **Example:** [Spring AOT runtime hints](/topics/spring-core/concepts.md#1-inversion-of-control-ioc-dependency-injection-di).

    **Common Mistake:** Relying on runtime reflection without registering `RuntimeHintsRegistrar`, causing `ClassNotFoundException` or `NoSuchMethodException` when executing as a GraalVM native executable.

    **Production Consideration:** Implement `RuntimeHintsRegistrar` for third-party libraries and dynamic payloads parsed via reflection; test native compilation in CI via Spring Boot Native Test.

    **Follow-up Questions:**
    - How does class loading and linking in JVM JIT mode differ from GraalVM native static compilation? See [JVM: Class Loading Lifecycle](/topics/jvm/questions.md#1-what-are-the-phases-of-the-class-loading-and-linking-lifecycle)
    - What performance tradeoffs exist between JVM JIT peak throughput and GraalVM native startup speed? See [Performance: Startup vs Peak Throughput](/topics/performance/questions.md)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q28SpringAotReflectionHintsExample.java"
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

### 29. Production incident: Injecting prototype-scoped bean into a singleton service causes concurrent state contamination

??? question "Reveal answer"

    **Short Answer:** A singleton service injected with a prototype-scoped bean receives only one instance created during singleton instantiation; subsequent concurrent calls share this single instance, causing multi-threaded race conditions and data corruption.

    **Deep Explanation:** In Spring, scope resolution occurs at injection time. When a singleton bean is created, its dependencies are resolved once. If a prototype bean carries mutable request state (e.g. tenant id or user parameters), all concurrent threads invoking the singleton share that single prototype instance.

    **Internal Mechanism:** The `DefaultListableBeanFactory` creates the prototype bean during the singleton's dependency population phase and never re-resolves it unless requested explicitly.

    **Example:** [Prototype in singleton lookup](/topics/spring-core/code-review.md).

    **Common Mistake:** Expecting `@Scope("prototype")` on a collaborator to automatically provide a new instance on every method invocation of a singleton bean.

    **Production Consideration:** Use `ObjectProvider<T>.getObject()`, `@Lookup` method injection, or scoped proxies (`ScopedProxyMode.TARGET_CLASS`) to resolve fresh instances dynamically upon invocation.

    **Follow-up Questions:**
    - How does safe publication guarantee memory visibility when resolving beans via `ObjectProvider`? See [Concurrency: Safe Publication](/topics/concurrency/questions.md#21-what-constitutes-safe-publication-of-shared-objects-in-the-java-memory-model)
    - What memory and lifecycle cleanup concerns arise with prototype beans since Spring does not invoke destruction callbacks on prototypes? See [Spring Core: Bean Scopes](/topics/spring-core/concepts.md#3-bean-scopes-scoped-proxies)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q29PrototypeInSingletonLeakScenarioExample.java"
        ```

### 30. Production incident: Internal method call bypasses method-level security and auditing interceptors

??? question "Reveal answer"

    **Short Answer:** Calling a secured or transactional method from within the same class (`this.method()`) bypasses Spring AOP proxy interception, executing the target method without security checks or transaction boundaries.

    **Deep Explanation:** Spring AOP is proxy-based. External callers invoke the proxy, which executes interceptors before delegating to the target instance. Once execution enters the target instance, any internal call to another method on `this` stays within the target instance without returning through the proxy, silently ignoring annotations like `@Secured`, `@PreAuthorize`, or `@Transactional`.

    **Internal Mechanism:** The Java `this` reference points to the unwrapped target object in memory, not the Spring CGLIB/JDK proxy wrapper.

    **Example:** [Self-invocation proxy failure](/topics/spring-core/code-review.md).

    **Common Mistake:** Adding security or transactional annotations to internal helper methods and assuming they will be enforced when invoked from public methods in the same class.

    **Production Consideration:** Refactor the secured method into a separate collaborator bean injected via constructor, or use `((CurrentClass) AopContext.currentProxy()).method()` with `@EnableAspectJAutoProxy(exposeProxy = true)`.

    **Follow-up Questions:**
    - How does Spring Security evaluate method security annotations during proxy dispatch? See [Spring Security: Method Security](/topics/spring-security/questions.md)
    - How does this same proxy bypass mechanism affect database transaction rollback? See [Spring Transactions: Self-Invocation Bypass](/topics/spring-transactions/questions.md#1-how-does-springs-transactional-annotation-work-under-the-hood-and-why-does-self-invocation-bypass-it)

    ??? example "Example"

        ```java
        --8<-- "modules/04-spring-core/src/examples/java/lab/springcore/questions/Q30ProxySecurityBypassScenarioExample.java"
        ```

<!-- --8<-- [end:scenarios] -->

## Related

- [Concepts](concepts.md)
- [Internals](internals.md)
- [Code Review](code-review.md)
- [Solutions](solutions.md)
