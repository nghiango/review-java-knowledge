# Design Patterns Interview Questions & Practice

Comprehensive, battle-tested interview questions exploring creational, structural, and behavioral design patterns, Spring Framework internal pattern applications, and refactoring anti-patterns.

---

<!-- --8<-- [start:basic] -->
## Basic Questions

### 1. How does the Strategy Pattern differ from the Template Method Pattern?

??? question "Reveal answer"
    Both patterns allow varying an algorithm, but they differ fundamentally in composition vs. inheritance:

    - **Strategy Pattern**: Uses **object composition**. Algorithms are extracted into independent classes implementing a shared interface. The context delegates execution to an injected strategy instance, allowing strategies to be swapped dynamically at runtime.
    - **Template Method Pattern**: Uses **class inheritance**. An abstract base class defines the fixed invariant skeleton of the algorithm in a `final` method, deferring specific variant steps to abstract or hook methods overridden by subclasses at compile time.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q01StrategyVsTemplateMethodExample.java"

### 2. What is the difference between the Factory Method and Abstract Factory patterns?

??? question "Reveal answer"
    - **Factory Method**: Relies on inheritance or a single method to create a single product. Subclasses decide which concrete class to instantiate.
    - **Abstract Factory**: Provides an interface for creating **families of related or dependent objects** (e.g. `DarkButton` + `DarkCheckbox`) without specifying their concrete classes. An Abstract Factory often contains multiple Factory Methods.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q02FactoryMethodVsAbstractFactoryExample.java"

### 3. Why should the Builder Pattern enforce validation inside the `build()` method rather than in field setters?

??? question "Reveal answer"
    Validating in intermediate setter methods can only check individual parameters in isolation, but business invariants frequently span **multiple interrelated attributes** (e.g. `startDate` must precede `endDate`, or `port` is only required if `protocol == TCP`).

    Validating inside `build()` ensures:
    1. Cross-field invariants are verified atomically after all parameters have been supplied.
    2. The resulting target object is immutable and guaranteed to never exist in an invalid or partially initialized state.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q03BuilderPatternValidationExample.java"

### 4. What problem does the Adapter Pattern solve, and how does it differ from a Decorator?

??? question "Reveal answer"
    - **Adapter Pattern**: Converts the interface of an existing class (the Adaptee) into another interface that callers expect (the Target). Its sole purpose is **interface translation** to make incompatible classes work together without modifying existing source code.
    - **Decorator Pattern**: Maintains the exact same interface as the target object, wrapping it to dynamically **add new behavior or responsibilities** (such as caching, compression, or encryption).

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q04AdapterPatternContractExample.java"

### 5. What distinguishes the Decorator Pattern from the Proxy Pattern?

??? question "Reveal answer"
    While both wrap a target object and implement its interface, their **design intent** is different:
    - **Decorator**: Focuses on dynamically **augmenting behavior**. Decorators are typically chained or composed recursively by the client.
    - **Proxy**: Focuses on **access control, lifecycle management, or remote boundary translation** (e.g. lazy initialization, security authorization, RPC communication). Proxies usually create or manage the target instance internally and are transparent to callers.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q05DecoratorVsProxyExample.java"

### 6. How does the Observer Pattern promote loose coupling in event-driven systems?

??? question "Reveal answer"
    The Observer Pattern decouples the **Subject** (event publisher) from its **Observers** (event consumers). The subject maintains only a generic list of listeners adhering to a common interface. When state changes, the subject broadcasts the event to all registered observers without knowing their concrete identities, business logic, or how many observers exist.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q06ObserverPatternSpringEventsExample.java"

### 7. What is the Chain of Responsibility Pattern and how does short-circuiting work?

??? question "Reveal answer"
    The Chain of Responsibility decouples the sender of a request from its potential receivers by chaining handler objects together. Each handler inspects the request and decides whether to process it, pass it to the next handler, or **short-circuit** (terminate execution immediately) if an invariant or security check fails.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q07ChainOfResponsibilityPipelineExample.java"

### 8. What is the architectural difference between a Repository and a Data Access Object (DAO)?

??? question "Reveal answer"
    - **DAO (Data Access Object)**: A database-centric abstraction. It typically models database tables or SQL result sets directly and provides CRUD operations matching database operations (`insert`, `update`, `delete`).
    - **Repository**: A domain-centric abstraction. It mimics an in-memory collection of **Domain Aggregate Roots** (`add()`, `findById()`). It hides persistence details entirely and communicates in Ubiquitous Language rather than database tables.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q08RepositoryVsDaoExample.java"
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Questions

### 9. How does the Specification Pattern enable composable business rules?

??? question "Reveal answer"
    The **Specification Pattern** encapsulates a domain business rule into a reusable object with an `isSatisfiedBy(candidate)` method. By implementing default combinators (`and`, `or`, `not`), specifications can be composed into complex logical boolean trees while keeping each atomic rule isolated, unit testable, and reusable across memory evaluations and database queries.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q09SpecificationPatternCompositionExample.java"

### 10. How does the State Pattern eliminate complex conditional flags and enforce lifecycle invariants?

??? question "Reveal answer"
    Instead of maintaining multiple boolean flags (`isPaid`, `isShipped`, `isCancelled`) and complex nested conditionals, the **State Pattern** models each lifecycle state as a distinct class implementing a shared interface.
    
    Each state class explicitly implements valid transitions (e.g. `Draft.pay() -> Paid`) and throws fast (e.g. `Draft.ship() -> IllegalStateException`) on invalid transitions, protecting the aggregate root from illegal state combinations.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q10StatePatternTransitionExample.java"

### 11. How does Spring use BeanPostProcessor and dynamic proxies to implement declarative transactions?

??? question "Reveal answer"
    Spring uses the Template Method pattern in `AbstractAutowireCapableBeanFactory` to create beans. During the `postProcessAfterInitialization` phase, `AbstractAutoProxyCreator` inspects the bean for annotations like `@Transactional`.
    
    If present, it creates a JDK dynamic proxy or CGLIB subclass wrapping the raw bean with a `TransactionInterceptor`. When clients invoke the bean, the proxy intercepts the call, opens a database transaction, invokes the real bean method, and commits or rolls back upon completion.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q11SpringBeanPostProcessorProxyExample.java"

### 12. How do you implement a thread-safe lazy Singleton in Java without synchronization performance overhead?

??? question "Reveal answer"
    The most robust approach is the **Initialization-on-Demand Holder Idiom** (Bill Pugh Singleton). It relies on JVM classloading guarantees: the static inner class is not loaded into memory until the `getInstance()` method is explicitly invoked.

    When loaded, the JVM initializes the static `INSTANCE` field atomically under internal class initialization locks, providing thread-safe lazy initialization with zero synchronization overhead on subsequent reads.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q12SingletonThreadSafetyExample.java"

### 13. When should you choose the Composite Pattern, and how does it treat individual vs composite nodes?

??? question "Reveal answer"
    The **Composite Pattern** is used when modeling hierarchical part-whole tree structures (such as file systems, organization charts, or UI component trees). It allows clients to treat individual leaf elements and composite branch nodes uniformly by having both implement a common component interface (e.g. `getSize()`).

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q13CompositePatternHierarchyExample.java"

### 14. How does the Flyweight Pattern optimize memory consumption in high-volume applications?

??? question "Reveal answer"
    The **Flyweight Pattern** minimizes memory usage by sharing immutable, intrinsic state across a large number of fine-grained objects. Instead of allocating thousands of identical instances, shared flyweight instances are cached and reused via a factory registry, leaving only extrinsic, contextual state to be passed during method invocation.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q14FlyweightPatternInterningExample.java"

### 15. What is the Visitor Pattern and how does Double Dispatch work?

??? question "Reveal answer"
    The **Visitor Pattern** allows adding new operations to an existing object structure without modifying the element classes.
    
    It operates via **Double Dispatch**:
    1. The client invokes `element.accept(visitor)` (first dispatch, resolves on element type).
    2. The element's implementation invokes `visitor.visit(this)` (second dispatch, resolves on visitor type).
    This allows the runtime execution to depend on the types of two distinct objects.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q15VisitorPatternDoubleDispatchExample.java"

### 16. What is the difference between a Mediator and an Event Broker?

??? question "Reveal answer"
    - **Mediator Pattern**: A centralized in-process coordinator that knows all participating colleague objects and explicitly directs their direct communication to eliminate $N \times N$ mesh coupling.
    - **Event Broker (Pub/Sub)**: A decoupled channel (e.g. Kafka, RabbitMQ, or Spring EventMulticaster) where producers publish events into a topic without any knowledge of consumers or their business intents.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q16MediatorVsEventBrokerExample.java"

### 24. How does the Memento Pattern enable transactional rollback and undo mechanisms without breaking encapsulation?

??? question "Reveal answer"
    The **Memento Pattern** externalizes an object's internal state into an opaque snapshot (`Memento`) without exposing internal private fields or implementation details.
    
    A caretaker (e.g. a transaction manager or undo buffer) stores the memento. When a rollback or undo is triggered, the caretaker passes the memento back to the originator (`originator.restore(memento)`), which restores its internal state cleanly.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q24MementoStateRollbackExample.java"

### 25. How does the Null Object Pattern eliminate defensive null checks across domain workflows?

??? question "Reveal answer"
    Instead of returning `null` references and forcing callers to perform repetitive `if (obj != null)` defensive checks, the **Null Object Pattern** provides a concrete class implementing the domain interface with neutral, predictable no-op behavior.
    
    This preserves polymorphism, keeps client code clean and readable, and completely eliminates `NullPointerException` risks.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q25NullObjectPatternExample.java"
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Questions

### 17. How do you refactor the Switch-on-Type anti-pattern into a Spring-managed Strategy registry?

??? question "Reveal answer"
    **The Problem**:
    Monolithic switch statements over enum types (e.g. payment rails, discount calculations) violate the Open/Closed Principle. Every new type forces risky edits to existing classes and leads to merge conflicts.

    **The Refactoring Pattern**:
    1. Define a cohesive strategy interface (`PaymentStrategy`) declaring `PaymentType getSupportedType()` and operations.
    2. Implement each variant as a dedicated `@Component` bean.
    3. Construct a registry bean (`PaymentStrategyFactory`) that injects `List<PaymentStrategy>` via constructor injection and builds an immutable `Map<PaymentType, PaymentStrategy>`.
    4. Callers query the map in $O(1)$ time. Adding a new payment type requires only authoring a new class with zero edits to existing code.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q17AntiPatternSwitchOnTypeExample.java"

### 18. Why is decorator ordering critical for security, and how does inverted composition cause authorization leaks?

??? question "Reveal answer"
    When combining cross-cutting decorators (such as Authorization, Caching, and Metric Logging), the decorator stack forms an onion:
    
    $$\text{Client} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{CachingDecorator} \longrightarrow \text{TargetService}$$

    **The Inverted Vulnerability**:
    If caching is placed *outside* authorization:
    1. An Admin user queries confidential data. The cache misses, authorization succeeds, and confidential data is cached.
    2. An unauthenticated Guest queries the same record. The outer Caching decorator finds a cache hit and immediately returns the confidential data!
    3. The inner Authorization decorator is **never called**, resulting in a critical security breach.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q18DecoratorOrderingSecurityExample.java"

### 19. How do you identify and refactor pattern over-engineering in code reviews?

??? question "Reveal answer"
    **Symptoms of Pattern Over-Engineering**:
    - Applying Gang of Four patterns as an academic exercise rather than solving real friction.
    - Cascading layers of indirection (AbstractFactoryProviderBridge, Visitor, Builder) for trivial transformations like formatting a 3-field CSV.
    - Deep call stacks with 10+ frames of pure delegating pass-through methods.

    **Refactoring Strategy**:
    Apply **KISS (Keep It Simple, Stupid)** and **YAGNI (You Aren't Gonna Need It)**. Replace synthetic visitor and builder hierarchies with idiomatic Java 21 features (records, static utility methods, and clean stream pipelines).

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q19OverEngineeringSimplificationExample.java"

### 20. How do design patterns inadvertently cause memory leaks in Java applications?

??? question "Reveal answer"
    1. **The Lapsed Listener Problem (Observer Pattern)**:
       When a long-lived subject registers listeners implemented by short-lived objects without deregistering them, the subject retains strong references to the listeners, preventing Garbage Collection and causing major memory leaks. Mitigated using `WeakReference` or auto-deregistration lifecycles.
    2. **Unbounded Static Caching (Flyweight / Decorator)**:
       Flyweight factories or caching decorators using raw `ConcurrentHashMap` without eviction policies (LRU, TTL) or memory bounds eventually exhaust heap memory in production.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q20DesignPatternMemoryLeaksExample.java"

### 21. How do modern Java 21 features replace classic Gang of Four patterns?

??? question "Reveal answer"
    Modern Java 21 language features provide concise native alternatives to GoF patterns:
    - **Sealed Interfaces & Pattern Matching**: Completely replaces the **Visitor Pattern** and double dispatch. Exhaustive `switch` expressions on sealed hierarchies provide compile-time safety without modifying classes or maintaining visitor interfaces.
    - **Records**: Eliminate the boilerplate of the **Builder Pattern** for simple immutable value carriers.
    - **Functional Interfaces & Method References**: Replace single-method **Strategy** and **Command** classes with concise lambdas (`Function`, `Consumer`, `Predicate`).

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q21IdiomaticJava21PatternsExample.java"

### 26. How does the Pipeline / Intercepting Filter Pattern differ from Chain of Responsibility?

??? question "Reveal answer"
    - **Chain of Responsibility**: A request travels down a sequence of handlers until **one** handler decides to process it and terminates the chain, or passes it along if it cannot handle it.
    - **Pipeline / Intercepting Filter**: Every registered filter in the sequence processes the request (or transformed data payload) in order, executing composable pre-processing and post-processing stages (e.g. decompression $\to$ decryption $\to$ authentication $\to$ schema validation).

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q26PipelineInterceptingFilterPatternExample.java"

### 27. How does the Dynamic Registry Pattern eliminate switch statements across polymorphic services?

??? question "Reveal answer"
    The **Registry Pattern** maintains an internal lookup table of strategy implementations indexed by business discriminator tags.
    
    In Spring Boot applications, strategy implementations are annotated as components, and the registry injects `List<Strategy>` via constructor injection, populating an immutable map at startup. Adding a new behavior requires zero edits to existing classes, fully honoring the Open/Closed Principle.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q27RegistryPatternDynamicDispatchExample.java"

### 28. How does Currying and Partial Application simplify complex configurable strategy patterns in modern Java?

??? question "Reveal answer"
    **Partial Application** and **Currying** transform a function taking multiple arguments into a sequence of functions taking fewer arguments.
    
    In Java, higher-order functions return specialized `@FunctionalInterface` instances with pre-bound configuration parameters (such as tax rates, discount caps, or connection timeouts), eliminating verbose strategy class hierarchies and mutable builder configurations.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q28PartialApplicationCurryingPatternExample.java"
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Scenario Questions

### 22. Scenario: A 2,000-line monolithic payment processor with 15 payment rails causes weekly merge conflicts and outages. Design the refactoring roadmap.

??? question "Reveal answer"
    **Diagnosis**:
    The service couples all third-party SDKs, fee calculations, validation rules, and status checks into a single monolithic class driven by massive `switch` blocks. Adding Apple Pay or Klarna risks breaking Credit Card processing.

    **Refactoring Roadmap**:
    1. **Define PaymentStrategy Interface**:
       Extract standard operations: `processPayment(request)`, `calculateFee(request)`, `refund(request)`.
    2. **Decompose Rails into Spring Components**:
       Create isolated classes: `CreditCardPaymentStrategy`, `PayPalPaymentStrategy`, `KlarnaPaymentStrategy`.
    3. **Build Strategy Registry**:
       Implement `PaymentStrategyFactory` auto-collecting all beans via constructor injection of `List<PaymentStrategy>`.
    4. **Safety & Zero-Downtime Rollout**:
       Use feature toggles to redirect traffic rail-by-rail to the new strategy beans, verifying identical behavior before removing legacy switch statements.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q22ScenarioRefactoringMonolithicPaymentGatewayExample.java"

### 23. Scenario: A security audit discovers that guest users can view confidential VIP documents due to a misconfigured caching decorator. How do you resolve it?

??? question "Reveal answer"
    **Incident Context**:
    A decorator pipeline was configured as:
    $$\text{Client} \longrightarrow \text{CachingDecorator} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{DocumentRepository}$$
    When an Auditor queries a confidential document, the cache is populated. Subsequent requests from regular users hit the cache and receive the sensitive document without ever invoking the Authorization check.

    **Remediation**:
    1. **Immediate Hotfix: Invert Decorator Order**:
       Restructure the pipeline so Authorization wraps Caching:
       $$\text{Client} \longrightarrow \text{AuthorizingDecorator} \longrightarrow \text{CachingDecorator} \longrightarrow \text{DocumentRepository}$$
       Every request must pass the role check *before* the cache is queried.
    2. **Architectural Guard: Tenant/Role Partitioned Cache Keys**:
       Update cache keys to incorporate security context: `(docId, userRole)`. Even on accidental reordering, guests cannot hit auditor cache entries.
    3. **Automated Verification**:
       Add unit tests explicitly asserting that guest queries to cached confidential items throw `SecurityException`.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q23ScenarioFixingSecurityBypassInDecoratorCacheExample.java"

### 29. Scenario: An enterprise monitoring service crashes with OutOfMemoryError after 24 hours of uptime due to an unmanaged Observer pattern implementation. Diagnose and remediate the Lapsed Listener defect.

??? question "Reveal answer"
    **Incident Context**:
    A high-frequency metrics service allowed short-lived worker components to subscribe to a singleton `EventPublisher`. The workers completed their tasks and went out of scope, but never explicitly called `unsubscribe()`. Because the singleton publisher held strong references to each listener in an internal list, the Garbage Collector could not reclaim the worker objects or their heavy payload buffers. Over 24 hours, millions of stale listener references accumulated, causing an eventual JVM `OutOfMemoryError: Java heap space`.

    **Remediation**:
    1. **WeakReference Listeners**:
       - Refactor the publisher to store listeners in a `WeakHashMap` or wrap them in `WeakReference<EventListener>`, allowing GC to collect eligible worker instances automatically.
    2. **Explicit Lifecycle Management (`AutoCloseable`)**:
       - Update the `subscribe()` method to return a `Subscription` or `AutoCloseable` token, allowing worker tasks to manage listener lifecycle via try-with-resources blocks.
    3. **Automated Leak Testing**:
       - Author unit tests that simulate GC triggering and verify that subscriber counts drop to zero after subscribers lose external references.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q29IncidentObserverMemoryLeakLapsedListenerExample.java"

### 30. Scenario: A high-throughput multithreaded application experiences intermittent NullPointerExceptions and corrupted state due to a flawed Double-Checked Locking Singleton implementation. Diagnose and resolve.

??? question "Reveal answer"
    **Incident Context**:
    A team implemented a lazy singleton with double-checked locking:
    ```java
    if (instance == null) {
        synchronized (Lock.class) {
            if (instance == null) {
                instance = new ExpensiveService();
            }
        }
    }
    ```
    Under production load with 64 concurrent threads, threads occasionally observed `instance != null` but encountered `NullPointerException` or corrupted data when reading internal fields of the singleton.

    **Root Cause Analysis**:
    The field was **not declared `volatile`**. In Java, object instantiation involves three steps:
    1. Allocate memory.
    2. Execute constructor to initialize fields.
    3. Assign memory address to reference variable `instance`.
    The JVM JIT compiler and CPU out-of-order execution are permitted to reorder steps 2 and 3. When reordering occurs, `instance` becomes non-null *before* field initialization completes. A concurrent thread executing the first null check sees `instance != null` and accesses a half-initialized object.

    **Remediation**:
    1. **Add `volatile` Modifier**:
       - Declaring `private static volatile ExpensiveService instance;` establishes a Java Memory Model (JMM) happens-before barrier, preventing instruction reordering.
    2. **Alternative: Bill Pugh Initialization-on-Demand Holder**:
       - Migrate to a static inner holder class, which guarantees lazy, thread-safe initialization without explicit synchronization.

    ??? example "Example"
        --8<-- "modules/29-design-patterns/src/examples/java/lab/designpatterns/questions/Q30IncidentDoubleCheckedLockingHalfInitializedExample.java"
<!-- --8<-- [end:scenarios] -->

## Related

- [Design patterns concepts](concepts.md)
- [Design patterns internals](internals.md)
- [Design patterns code review](code-review.md)
- [Design patterns solutions](solutions.md)
- [Design patterns tests](tests.md)
