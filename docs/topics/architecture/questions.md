# Architecture Interview Questions & Practice

Comprehensive, battle-tested interview questions exploring software architecture, domain-driven design, modular monoliths, hexagonal boundaries, CQRS, and ArchUnit verification.

---

<!-- --8<-- [start:basic] -->
## Basic Questions

### 1. How does Hexagonal (Ports & Adapters) Architecture differ from traditional Layered Architecture?

??? question "Reveal answer"
    In traditional Layered Architecture, dependencies flow top-to-bottom: Web Controller $\to$ Service $\to$ Repository $\to$ Database. The business domain depends directly on the persistence layer, fostering a database-driven mindset where entity models mirror database tables and infrastructure concerns leak into business logic.

    In Hexagonal Architecture, the Dependency Inversion Principle is applied: the core business domain is placed at the center and depends on **nothing**. It defines inbound contracts (Driving Ports / Use Cases) and outbound contracts (Driven Ports / Repositories, Payment Gateways). Infrastructure adapters live on the outside and implement or invoke these ports. This ensures domain rules can be tested in microseconds without Spring or databases and allows swapping infrastructure without touching domain code.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q01LayeredVsHexagonalExample.java"

### 2. What is an Anemic Domain Model, and why is it considered an anti-pattern compared to a Rich Domain Model?

??? question "Reveal answer"
    An **Anemic Domain Model** consists of entity classes that are mere data bags—containing private fields with public getters and setters and zero business logic. All business rules, calculations, and invariant validations are placed into procedural service classes. This violates object-oriented encapsulation: any caller can mutate internal fields into invalid states, validation logic is duplicated across multiple services, and state transitions cannot be guaranteed.

    A **Rich Domain Model** encapsulates both state and behavior within the aggregate. Internal collections are protected, constructors and methods enforce business invariants upon entry, and state mutations occur strictly through intention-revealing methods (`order.cancel()`, `account.withdraw()`). Services become thin orchestrators that load the aggregate, invoke domain behavior, and persist changes.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q02AnemicVsRichDomainExample.java"

### 3. In Domain-Driven Design, what distinguishes an Entity from a Value Object?

??? question "Reveal answer"
    An **Entity** has a persistent thread of identity that remains constant across time and mutable state changes. Two entities with identical attributes but different unique identifiers (e.g., UUID or database ID) represent two distinct business objects.

    A **Value Object** has no conceptual identity; it is defined entirely by the combination of its attributes. Value Objects are immutable, concept-rich, and compared strictly by structural equality. If any attribute changes, it represents a new Value Object instance. Examples include `Money(amount, currency)`, `Address`, and `Quantity`. Using Value Objects prevents primitive obsession and centralizes self-validation.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q03EntityVsValueObjectExample.java"

### 4. What is a DDD Aggregate and an Aggregate Root, and what rules govern their boundaries?

??? question "Reveal answer"
    An **Aggregate** is an encapsulation boundary around a cluster of associated entities and value objects treated as a single unit for data changes and transactional consistency.

    The **Aggregate Root** is the sole gateway through which outside code can interact with the aggregate:
    1. External objects may only hold a reference to the Aggregate Root, never to internal child entities.
    2. The Aggregate Root is responsible for enforcing all business invariants across the entire boundary.
    3. A single database transaction should modify exactly **one** aggregate root instance. Inter-aggregate consistency is achieved through Domain Events and eventual consistency.
    4. Internal child entities can only be accessed or modified through methods on the Aggregate Root.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q04AggregateRootBoundaryExample.java"

### 5. What is the difference between a Domain Service and an Application Service?

??? question "Reveal answer"
    A **Domain Service** represents pure business logic or calculations that naturally span multiple aggregates or do not belong to a single entity (e.g., `CurrencyExchangeDomainService`, `TaxCalculationService`). It operates purely on domain objects, enforces business rules, and performs **zero I/O** (no database calls, no network requests, no framework dependencies).

    An **Application Service** is an orchestration layer. It exposes use cases to the outside world, coordinates transaction boundaries, authenticates callers, loads aggregates from repositories, invokes domain methods, and publishes domain events. It contains no business rules itself; it merely orchestrates the workflow.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q05DomainVsApplicationServiceExample.java"

### 6. What is CQRS (Command Query Responsibility Segregation) and when should it be chosen?

??? question "Reveal answer"
    CQRS is an architectural pattern that separates the data model for mutating operations (Commands) from the data model for reading operations (Queries).

    - **Command Model**: Enforces business invariants, executes validation, processes writes, and maintains transactional consistency. Does not return UI view models.
    - **Query Model**: Optimized for fast reads, reporting, and UI presentation. Reads from denormalized views or search indexes (e.g., Elasticsearch, Redis).

    **When to choose**: Choose CQRS when read and write workloads have drastically different throughput or scaling profiles, when read queries require complex joins across multiple domain boundaries, or when combining with Event Sourcing. Avoid CQRS for simple CRUD applications where a single shared model is sufficient.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q06CqrsSeparationExample.java"

### 7. What are the trade-offs between synchronous RPC and asynchronous Event-Driven messaging?

??? question "Reveal answer"
    - **Synchronous RPC (REST / gRPC)**:
      - *Pros*: Simple request-response mental model, immediate feedback to caller, easier debugging and tracing.
      - *Cons*: Temporal coupling (both caller and callee must be online), thread pool starvation during downstream latency spikes, cascading failures, difficult distributed transaction handling.
    - **Asynchronous Event-Driven (Kafka / RabbitMQ)**:
      - *Pros*: Temporal decoupling (producer succeeds even if consumer is offline), traffic buffering/spike absorption, independent scaling, easy addition of new consumers without modifying producers.
      - *Cons*: Eventual consistency, complex error handling and dead-letter queue management, distributed tracing overhead, message ordering challenges.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q07SyncVsAsyncEventExample.java"

### 8. What is a Modular Monolith and how does it compare to Microservices?

??? question "Reveal answer"
    A **Modular Monolith** is a software system deployed as a single runtime process (e.g., one Spring Boot JAR), but internally divided into strict, decoupled modules aligned with business bounded contexts. Modules communicate strictly via public Java API contracts or in-memory Spring Application Events, with direct cross-module database access forbidden.

    Compared to microservices, a modular monolith eliminates distributed network latency, serialized RPC payloads, distributed transaction complexities (Sagas), and Kubernetes orchestration overhead. It provides the boundary cleanliness of microservices with the operational simplicity, atomic deployments, and fast local development of a monolith.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q08ModularMonolithBoundaryExample.java"
<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate Questions

### 9. How are Driving and Driven Ports implemented and tested in Hexagonal Architecture?

??? question "Reveal answer"
    In Hexagonal Architecture:
    - **Driving (Inbound) Ports**: Interfaces declaring use cases (e.g., `PlaceOrderUseCase`). Implemented by application services and called by incoming driving adapters (Spring MVC `@RestController`, CLI, or Kafka consumers).
    - **Driven (Outbound) Ports**: Interfaces declared inside the domain layer specifying what the domain requires (e.g., `OrderRepositoryPort`, `PaymentPort`). Implemented by outgoing driven adapters (JPA repository adapter, Stripe HTTP adapter).

    **Testing Strategy**:
    The core domain and application services can be tested purely in memory using fast unit tests by supplying simple mock or in-memory stub implementations of Driven Ports. No Spring test runner or database container is required. Driven adapters are tested independently with integration tests (e.g., Testcontainers for PostgreSQL, WireMock for Stripe).

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q09PortsAndAdaptersExample.java"

### 10. How can ArchUnit fitness functions prevent architectural erosion in CI?

??? question "Reveal answer"
    Architectural erosion occurs when developers take shortcuts—such as controllers injecting repositories directly, domain entities importing JPA annotations, or modules cross-querying foreign database tables. Over time, clean boundaries degrade into a "big ball of mud".

    ArchUnit prevents this by analyzing compiled JVM bytecode during standard JUnit test execution. It allows architects to define declarative rules (e.g., `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAPackage("..infrastructure..")`). Because ArchUnit tests run as part of `./gradlew test` on every pull request, any illegal architectural shortcut fails CI immediately before code reaches production.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q10ArchUnitFitnessRuleExample.java"

### 11. How do Domain Events decouple business side effects within an aggregate lifecycle?

??? question "Reveal answer"
    When a business operation completes inside an Aggregate Root (e.g., `order.cancel()`), secondary side effects are often required: sending a customer confirmation email, restocking inventory, and updating an analytics dashboard.

    Executing these side effects synchronously inside the aggregate violates the Single Responsibility Principle and couples the aggregate to external systems. By recording a **Domain Event** (`OrderCancelledEvent`) inside the aggregate and dispatching it after the transaction commits, side effects are decoupled. Downstream listeners react independently and asynchronously without impacting the primary transaction.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q11DomainEventPublishingExample.java"

### 12. What is a Bounded Context, and how does an Anti-Corruption Layer (ACL) protect a new domain?

??? question "Reveal answer"
    A **Bounded Context** is an explicit boundary within which a specific domain model applies. The meaning of words and models outside that boundary may differ completely.

    When integrating a clean, modern bounded context with a legacy system, third-party vendor API, or external service, the external system often uses confusing, obsolete, or poorly structured data models. An **Anti-Corruption Layer (ACL)** acts as a bidirectional translation adapter. It translates external data structures and protocols into the clean Ubiquitous Language of the internal domain, preventing foreign concepts from contaminating core domain models.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q12AntiCorruptionLayerExample.java"

### 13. How does Event Sourcing rehydrate entity state, and what are its core advantages?

??? question "Reveal answer"
    In Event Sourcing, instead of storing the latest state as a mutable row in a database, all changes are stored as an append-only stream of immutable domain events.

    **Rehydration**: To reconstruct the current state of an aggregate, the persistence framework queries the event store for all events matching that aggregate's ID ordered by sequence number. It initializes a blank aggregate and sequentially invokes internal event-apply handlers (`apply(event)`), calculating the current state deterministically.

    **Advantages**:
    1. Complete, non-destructive audit trail with exact historical timestamps.
    2. Ability to perform temporal queries ("What was the customer balance on March 1st?").
    3. Seamless projection replaying: if business requirements change, new read models can be computed by replaying past events.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q13EventSourcingHydrationExample.java"

### 14. In CQRS, how are read-model projections updated, and how do you handle eventual consistency?

??? question "Reveal answer"
    In CQRS, whenever the Command side appends an event or commits a state mutation, it publishes a domain event. An asynchronous projector listens to these events and updates a denormalized read model (e.g., updating an Elasticsearch document or a PostgreSQL reporting table).

    **Handling Eventual Consistency**:
    Because the projection is updated asynchronously, there is a sub-second lag between command commit and projection update. Mitigations include:
    1. Optimistic UI updates on the client.
    2. Returning the newly created resource or updated version directly in the command response.
    3. Read-after-write version validation: the client passes the mutation sequence number, and the read API waits or verifies the projection version before returning.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q14CqrsProjectionExample.java"

### 15. How does Spring Modulith detect module boundaries and guarantee event delivery?

??? question "Reveal answer"
    **Boundary Detection**:
    Spring Modulith scans the package tree rooted at the `@SpringBootApplication` class. Each top-level package is registered as an Application Module. Packages nested within a module are treated as internal implementations unless explicitly exported. `ApplicationModules.of(Application.class).verify()` verifies that cross-module dependencies do not violate declared visibility.

    **Guaranteed Event Delivery**:
    Spring Modulith provides an **Event Publication Registry**. When an event is published, Modulith writes the serialized event into a database table (`event_publication`) in the same local transaction. When asynchronous `@ApplicationModuleListener` consumers complete processing, the event is marked completed. If the application crashes midway, unacknowledged events are re-dispatched upon startup, ensuring at-least-once inter-module event delivery.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q15SpringModulithVerificationExample.java"

### 16. How does Optimistic Concurrency Control protect Aggregate Roots in high-concurrency environments?

??? question "Reveal answer"
    In high-concurrency systems, multiple threads or nodes may attempt to modify the same Aggregate Root simultaneously. If both threads load version $v=1$, perform local modifications, and write back, the second write silently overwrites the first (Lost Update anomaly).

    With **Optimistic Concurrency Control (OCC)**, the aggregate maintains a `version` attribute. When saving, the database executes an update with a version check:
    ```sql
    UPDATE orders SET status = 'PAID', version = version + 1 WHERE id = 'ORD-1' AND version = 1;
    ```
    If the row was modified by another thread, zero rows are updated, triggering an `OptimisticLockingFailureException`. The application aborts or reloads the latest state and retries the business operation cleanly.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q16AggregateOptimisticLockingExample.java"
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Questions

### 17. How do you safely migrate a legacy monolith to microservices using the Strangler Fig pattern?

??? question "Reveal answer"
    The **Strangler Fig pattern** gradually replaces specific functionality of a monolithic system with new microservices until the legacy monolith is completely decommissioned, avoiding risky "big bang" rewrites.

    **Execution Strategy**:
    1. **Edge Interception**: Place an API Gateway (Envoy, Spring Cloud Gateway, AWS ALB) in front of the legacy monolith.
    2. **Identify Bounded Context**: Pick a cohesive, high-value, or frequently changing bounded context (e.g., Payments).
    3. **Implement Microservice**: Build the new microservice using clean hexagonal boundaries and an independent database.
    4. **Dual-Run / Dark Launch**: Route a fraction of traffic to the new service in parallel, comparing outputs to verify behavioral parity.
    5. **Incremental Cutover**: Update the API Gateway route to direct 100% of live traffic to the new service.
    6. **Decommission & Repeat**: Remove legacy payment code from the monolith and proceed to the next bounded context.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q17StranglerFigPatternExample.java"

### 18. What is the Dual-Write Hazard in distributed systems and how does the Transactional Outbox pattern solve it?

??? question "Reveal answer"
    **The Dual-Write Hazard**:
    Occurs when an application needs to update a database and publish an event to a message broker (Kafka/RabbitMQ) in the same business transaction:
    - If the database commit succeeds but the network to Kafka drops, the message is never sent (event loss).
    - If the Kafka publish succeeds but the database transaction rolls back, external services act on phantom data.
    - Two-Phase Commit (XA/2PC) across relational databases and Kafka is either unsupported, fragile, or introduces severe latency.

    **The Transactional Outbox Solution**:
    1. An `outbox` table is created in the same database as business entities.
    2. The business mutation and the outbox event record are written in a **single local ACID transaction**.
    3. A separate asynchronous process (e.g., Transaction Log Tailing with Debezium CDC or a background polling poller) reads committed outbox records and publishes them to the message broker.
    4. Upon broker acknowledgment, the outbox record is marked published or deleted, guaranteeing at-least-once delivery.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q18TransactionalOutboxPatternExample.java"

### 19. How do you handle schema evolution, event upcasting, and snapshotting in Event Sourcing?

??? question "Reveal answer"
    Because events in an event store are immutable historical facts, modifying existing event records in place is strictly forbidden.

    1. **Schema Evolution & Upcasting**:
       - When an event structure changes (e.g., `UserRegistered` needs a mandatory `countryCode`), an **Upcaster** is inserted into the event deserialization pipeline.
       - The Upcaster intercepts old $v1$ event JSON, migrates it in memory to the $v2$ format with sensible defaults, and hands the upcasted event to the aggregate. The stored historical database payload remains untouched.
    2. **Snapshotting**:
       - Aggregates with thousands of events (e.g., long-lived bank accounts) suffer high hydration latency.
       - A snapshotting job periodically serializes the aggregate's current state and version into a snapshot table.
       - During rehydration, the engine loads the latest snapshot and replays only subsequent events where `sequence > snapshot.version`.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q19EventSourcingSnapshottingExample.java"

### 20. In CQRS with asynchronous projections, how do you solve the Read-After-Write Consistency problem?

??? question "Reveal answer"
    When a user submits a command (e.g., updates their profile) and is immediately redirected to the profile view, an asynchronous CQRS projection may not have processed the event yet, causing the user to see stale data.

    **Architectural Solutions**:
    1. **Command Returns Updated View**: The command endpoint returns the newly mutated state directly in the HTTP 200/201 response payload, allowing the client frontend to update local state immediately.
    2. **Version Tracking / Causality Tokens**: The command response returns a revision token (e.g., `version = 42`). The subsequent read query includes this token. If the read projection has not caught up (`projection_version < 42`), the query either waits for a brief timeout (poll with backoff) or reads directly from the write model fallback.
    3. **Client-Side Optimistic Rendering**: The UI applies the change locally and updates when confirmation arrives.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q20ReadAfterWriteCqrsExample.java"

### 21. Why is the Shared Database anti-pattern fatal to microservices and modular monoliths?

??? question "Reveal answer"
    The **Shared Database anti-pattern** occurs when multiple autonomous services or modules read and write directly to the same underlying database tables.

    **Failure Modes**:
    1. **Broken Team Autonomy**: Any schema migration (renaming a column, altering a type, splitting a table) requires coordinated, simultaneous deployments across multiple engineering teams.
    2. **Bypassed Business Logic**: External services modify data directly in SQL, bypassing domain validation, state machines, and audit event emission.
    3. **Resource Starvation & Deadlocks**: A slow, un-indexed analytics query from one service acquires locks that block high-priority transactional writes from the core service.
    4. **Impossible Boundary Enforcement**: Makes future service extraction, horizontal sharding, or polyglot persistence impossible.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q21SharedDatabaseAntiPatternExample.java"
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Scenario Questions

### 22. Scenario: A 5,000-line procedural `OrderService` with 30 database dependencies suffers frequent regressions. Redesign it into a Hexagonal DDD architecture.

??? question "Reveal answer"
    **Diagnosis**:
    The service is a classic "God Class" coupling web controllers, database persistence, external email sending, discount calculations, and payment gateways into procedural spaghetti. Invariants are leaking, testing requires hundreds of mocks, and modifying one feature breaks another.

    **Refactoring Roadmap**:
    1. **Extract Bounded Domain Model**:
       - Create an `Order` Aggregate Root in a pure domain package.
       - Move invariant validation, status state transitions, and price calculations (`calculateFinalTotal`) inside `Order`.
       - Convert primitive fields into immutable Value Objects (`Money`, `OrderId`, `CustomerEmail`).
    2. **Define Driven Ports**:
       - Extract interfaces for external dependencies: `OrderRepositoryPort`, `PaymentPort`, `NotificationPort`.
    3. **Implement Thin Application Service**:
       - Rewrite `OrderApplicationService` to coordinate the workflow: load order from repository port, invoke domain methods, call payment port, and save.
    4. **Adapt Existing Infrastructure**:
       - Wrap existing JPA repositories and HTTP clients in outer adapter classes implementing the new driven ports.
    5. **Enforce with ArchUnit**:
       - Add ArchUnit rules ensuring `domain` classes import nothing outside `java.*`, preventing regressions.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q22GodServiceRefactoringScenarioExample.java"

### 23. Scenario: A microservices platform suffers severe latency and cascade timeouts due to synchronous distributed joins across 5 services. How do you redesign it?

??? question "Reveal answer"
    **Incident Context**:
    A customer dashboard endpoint calls the `OrderService`, which sequentially calls `CustomerService`, `InventoryService`, `ShippingService`, and `BillingService` over HTTP REST to assemble a single response. P99 latency is 3.5 seconds; if any downstream service slows down, thread pools exhaust and the entire dashboard crashes.

    **Architectural Redesign**:
    1. **Adopt CQRS & Asynchronous Projections**:
       - Instead of dynamic runtime distributed joins, build a dedicated read model (e.g., in PostgreSQL or Elasticsearch) for the customer dashboard.
    2. **Event-Driven Aggregation**:
       - Services publish domain events (`OrderCreatedEvent`, `CustomerUpdatedEvent`, `ShipmentStatusChangedEvent`) to a Kafka topic.
       - A lightweight Projector service consumes these events and updates the pre-joined `CustomerDashboardView` asynchronously.
    3. **Sub-Millisecond Read Queries**:
       - The dashboard endpoint queries the pre-materialized view directly via a single indexed read ($< 10\text{ ms}$).
    4. **Resilience & Fault Isolation**:
       - If `ShippingService` is down, dashboard views continue serving existing data with zero disruption.

    ??? example "Example"
        --8<-- "modules/28-architecture/src/examples/java/lab/architecture/questions/Q23DistributedJoinResolutionScenarioExample.java"
<!-- --8<-- [end:scenarios] -->

## Related

- [Architecture concepts](concepts.md)
- [Architecture internals](internals.md)
- [Architecture code review](code-review.md)
- [Architecture solutions](solutions.md)
- [Architecture tests](tests.md)
