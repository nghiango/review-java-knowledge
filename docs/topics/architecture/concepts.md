# Software Architecture Concepts

Deep dive into architectural styles, domain-driven design, modular monolith principles, CQRS, and event sourcing.

---

## 1. Architectural Styles Compared

### Traditional Layered Architecture

```mermaid
flowchart TD
    UI["Web Layer (@RestController)"] --> Service["Service Layer (@Service)"]
    Service --> Persistence["Data Access Layer (@Repository)"]
    Persistence --> DB[("Database")]
```

- **Characteristics**: Top-to-bottom dependency flow. Each layer depends directly on the layer below it.
- **Benefits**: Intuitive, easy to bootstrap in Spring Boot, standard mental model for juniors.
- **Failure Modes & Pitfalls**:
  - **Database-Driven Design**: The database schema is defined first; domain classes mirror tables; business logic becomes anemic and procedural.
  - **Transitive Infrastructure Leaks**: If the persistence layer changes (e.g. switching from JPA to DynamoDB), service and web layers often break.
  - **Bypassed Logic**: Controllers frequently bypass services to query repositories directly for read operations, scattering validation.

---

### Hexagonal Architecture (Ports and Adapters)

Invented by Alistair Cockburn, Hexagonal Architecture places the business domain at the center, isolating it completely from external technology:

```mermaid
flowchart LR
    subgraph Driving["Primary / Driving Adapters"]
        REST["REST Controller"]
        CLI["CLI / Batch"]
        MessageSub["Kafka Consumer"]
    end

    subgraph Hexagon["Application & Domain Core"]
        direction TB
        InPort["Driving Ports<br/>(PlaceOrderUseCase)"]
        AppService["Application Service"]
        Domain["Domain Entities & Aggregates<br/>(Pure POJOs)"]
        OutPort["Driven Ports<br/>(OrderRepositoryPort, PaymentPort)"]
    end

    subgraph Driven["Secondary / Driven Adapters"]
        JPA["PostgreSQL JPA Adapter"]
        PaymentExt["Stripe HTTP Adapter"]
        KafkaPub["Kafka Producer Adapter"]
    end

    REST --> InPort
    CLI --> InPort
    MessageSub --> InPort
    InPort --> AppService
    AppService --> Domain
    AppService --> OutPort
    JPA -.->|Implements| OutPort
    PaymentExt -.->|Implements| OutPort
    KafkaPub -.->|Implements| OutPort
```

- **Driving Ports (Inbound)**: Interfaces exposing what the system can do (e.g. `PlaceOrderUseCase`). Driven by external actors (REST controllers, CLI, message listeners).
- **Driven Ports (Outbound)**: Interfaces declaring what the system needs from the outside world (e.g. `OrderRepositoryPort`, `PaymentPort`). Defined by the domain; implemented by infrastructure adapters.
- **The Golden Rule**: The hexagon knows **nothing** about the outside world. Adapters depend on ports; the domain never depends on adapters.

---

### Clean Architecture & Onion Architecture

Uncle Bob’s Clean Architecture and Jeffrey Palermo’s Onion Architecture formalize Hexagonal Architecture into concentric rings governed by the **Dependency Rule**:

$$\text{Source code dependencies must point strictly inward toward higher-level policies.}$$

```mermaid
flowchart TD
    subgraph Concentric["Clean Architecture Rings"]
        Infra["Frameworks & Drivers (Web, DB, Devices)"]
        InterfaceAdapters["Interface Adapters (Controllers, Gateways, Presenters)"]
        UseCases["Application Business Rules (Use Cases)"]
        Entities["Enterprise Business Rules (Entities / Value Objects)"]

        Infra --> InterfaceAdapters
        InterfaceAdapters --> UseCases
        UseCases --> Entities
    end
```

- **Inner Rings**: Pure domain logic and business rules. Completely framework-agnostic.
- **Outer Rings**: Delivery mechanisms, databases, and third-party frameworks. They can be replaced or upgraded without modifying inner ring business logic.

---

## 2. Modular Monolith vs Microservices

| Architectural Dimension | Modular Monolith | Microservices Architecture |
|---|---|---|
| **Deployment Model** | Single deployable artifact (JAR/WAR, single Docker container). | $N$ independently deployed services across Kubernetes pods. |
| **Communication** | In-memory method invocations, Spring events (sub-microsecond latency). | Network calls: REST/JSON, gRPC, Kafka (1ms–100ms latency). |
| **Data Consistency** | Local database transactions, modular schema schemas, outbox events. | Distributed transactions, Saga patterns, eventual consistency. |
| **Operational Overhead** | Low: single CI/CD pipeline, unified logging, simple local debugging. | High: service mesh, distributed tracing, Kubernetes ingress, multi-repo CI/CD. |
| **Failure Mode** | Process crash affects entire application unless bulkheaded. | Partial failures: graceful degradation, circuit breaking required. |
| **Best Used When** | Domain boundaries are evolving; small-to-medium teams ($< 50$ engineers). | High organizational scale; disparate teams needing independent release cycles. |

---

## 3. Domain-Driven Design (DDD)

### Strategic Design

1. **Ubiquitous Language**: A shared, rigorous vocabulary co-created by domain experts and software engineers, reflected identically in conversations and Java source code.
2. **Bounded Context**: An explicit linguistic and architectural boundary within which a domain model applies consistently. For example, in an e-commerce platform:
   - In the **Ordering Context**, a "Customer" is a buyer with shipping addresses and payment methods.
   - In the **Identity Context**, a "User" is an authenticated principal with passwords and roles.
   - In the **Support Context**, a "Customer" is a ticket submitter with communication logs.
3. **Context Mapping**:
   - **Shared Kernel**: Shared subset of domain code and database tables (high coupling risk).
   - **Customer-Supplier**: Upstream team provides data needed by downstream team.
   - **Anti-Corruption Layer (ACL)**: A translation adapter translating external/upstream models into the clean internal Ubiquitous Language of the downstream context.

---

### Tactical Design

```mermaid
classDiagram
    class AggregateRoot {
        <<Entity>>
        -OrderId id
        -OrderStatus status
        -Money total
        +addItem(Item)
        +submit()
        +cancel()
    }
    class Entity {
        -OrderItemId id
        -Quantity quantity
        -Money unitPrice
    }
    class ValueObject {
        <<Immutable>>
        -BigDecimal amount
        -Currency currency
        +add(Money)
        +isPositive()
    }
    AggregateRoot "1" *-- "many" Entity : encapsulates
    Entity ..> ValueObject : uses
    AggregateRoot ..> ValueObject : uses
```

- **Aggregate & Aggregate Root**:
  - An Aggregate is a cluster of associated objects treated as a single unit for data changes.
  - The **Aggregate Root** is the sole external entry point into the aggregate. Outside callers cannot reference internal child entities directly.
  - **Rule of Thumb**: One transaction should modify exactly **one** aggregate instance. Inter-aggregate coordination happens via Domain Events and eventual consistency.
- **Entity vs Value Object**:
  - **Entity**: Defined by identity that spans time and state mutations (e.g. `Customer`, `Order`).
  - **Value Object**: Defined solely by its attributes. Immutable, concept-rich, and compared by value (e.g. `Money`, `Address`, `Quantity`). Zero side-effects.
- **Domain Service**:
  - Encapsulates domain logic that does not naturally belong to a single entity or value object (e.g. calculating exchange rates or pricing rules across multiple aggregates). Does not perform I/O.
- **Application Service**:
  - Orchestrates the use case: opens transaction, loads aggregate from repository, invokes domain methods, persists changes, and dispatches domain events.

---

## 4. CQRS (Command Query Responsibility Segregation)

CQRS separates the system into two distinct operational models:

```mermaid
flowchart TD
    Client["Client Request"]
    Client -->|"POST /orders (Mutating Command)"| CommandSide["Command Model (Write)"]
    Client -->|"GET /orders/summary (Query)"| QuerySide["Query Model (Read)"]

    subgraph WritePipeline["Write Pipeline"]
        CommandSide --> AppService["OrderApplicationService"]
        AppService --> Agg["Order Aggregate Root"]
        Agg --> EventStore[("Event Store / Write DB")]
    end

    subgraph ProjectionSync["Asynchronous Projection"]
        EventStore -->|"Domain Event Stream"| Projector["Read Model Projector"]
        Projector --> ReadDB[("Read DB / Elasticsearch / Redis")]
    end

    ReadDB --> QuerySide
```

- **Command Model**: Optimized for invariant validation, transactional integrity, and state mutation. Does not return presentation DTOs.
- **Query Model**: Optimized for high-throughput reads, search, and presentation. Uses denormalized, read-optimized tables or search indexes.
- **Trade-offs**: Extreme read scalability and query flexibility at the cost of eventual consistency and dual-model code maintenance.

---

## 5. Event Sourcing

Traditional databases store only the **current state** of an entity, discarding the historical journey of how that state was reached. **Event Sourcing** persists every change as an immutable, append-only sequence of domain events:

$$\text{Current State} = \text{Initial State} + \sum_{i=1}^{n} \text{Event}_i$$

- **Auditability & Compliance**: Complete, tamper-proof historical audit trail out of the box.
- **Temporal Queries**: Ability to reconstruct the exact system state at any point in the past.
- **Replayability**: New read projections can be generated retroactively by replaying the complete historical event log.
- **Trade-offs**: Increased operational complexity, eventual consistency, schema evolution challenges, and the necessity of point-in-time snapshotting for long-lived aggregates.

---

## 6. BDD vs DDD: Separation & Combination

While frequently mentioned together, **Domain-Driven Design (DDD)** and **Behavior-Driven Development (BDD)** operate at different layers of software engineering. Understanding both their separation and their synergy is essential for senior backend architects.

### Separating DDD and BDD

| Dimension | Domain-Driven Design (DDD) | Behavior-Driven Development (BDD) |
|---|---|---|
| **Primary Nature** | **Architectural & Modeling Paradigm** | **Collaboration & Verification Methodology** |
| **Core Question** | *"How do we model business complexity and protect consistency boundaries?"* | *"What exact behavior do stakeholders expect, and how do we prove it works?"* |
| **Core Abstractions** | Bounded Contexts, Aggregate Roots, Entities, Value Objects, Domain Events. | Features, User Scenarios, Given-When-Then steps, Acceptance Criteria. |
| **Execution Boundary** | In-memory domain model, encapsulation boundaries, persistence transactions. | Acceptance test runner (Cucumber, JGiven, JUnit 5 nested specs), living documentation. |
| **Scope** | Structural integrity of the software system across its lifecycle. | Functional verification of specific features and end-to-end user journeys. |

- **DDD without BDD** risks building beautifully modeled aggregates that miss real-world user intent or suffer from communication gaps between engineers and product owners.
- **BDD without DDD** risks producing a sprawling suite of high-level acceptance tests that verify an anemic, procedural codebase riddled with god services and fragile database coupling.

---

### The Combination: Outside-In Synergy in Hexagonal Architecture

When combined, BDD and DDD form a powerful outside-in engineering pipeline:

```mermaid
flowchart TD
    subgraph Discovery["1. Discovery (Ubiquitous Language)"]
        PO["Product Owner + Domain Experts"] <--> Devs["Software Engineers"]
        PO & Devs --> UL["Ubiquitous Language"]
    end

    subgraph BDD["2. Specification (BDD Feature)"]
        UL --> GWT["Gherkin Scenarios<br/>(Given - When - Then)"]
    end

    subgraph Hexagon["3. Execution (Hexagonal DDD Core)"]
        GWT -->|"Drives Inbound Port"| UseCase["Driving Port / Use Case<br/>(PlaceOrderUseCase)"]
        UseCase -->|"Loads / Invokes"| Agg["Aggregate Root<br/>(Order.submit())"]
        Agg -->|"Validates Invariants"| State["State Mutation<br/>(status=PAID)"]
        Agg -->|"Emits"| Events["Domain Events<br/>(OrderPlacedEvent)"]
    end

    GWT -.->|"Asserts State & Events"| State
```

1. **Ubiquitous Language as the Bridge**:
   - The shared vocabulary co-created during Event Storming and domain modeling forms both the terminology of Gherkin scenario steps and the Java method and type names in the domain core.
2. **Given-When-Then Mapping to Aggregate Lifecycles**:
   - **Given [Precondition]**: Aggregate initialized in a specific state (e.g. `Order` in `DRAFT` with line items).
   - **When [Action]**: Inbound Use Case / Driving Port is invoked (`PlaceOrderUseCase.placeOrder(...)`).
   - **Then [Outcome]**: Aggregate state transitions and invariant assertions verified (`order.getStatus() == PAID`).
   - **And [Side Effect]**: Emitted domain events verified (`OrderPlacedEvent`).
3. **Sub-Millisecond In-Memory Execution**:
   - Instead of driving BDD scenarios through slow browser automation (Selenium) or full Spring Boot web server boots, BDD acceptance tests can execute directly against the Hexagonal Driving Port and pure domain core in microseconds.

---

## 7. Backend for Frontend (BFF) Pattern

### The Problem: The Single Generic API Gateway Anti-Pattern

In modern multi-client platforms, a single backend API must serve vastly different client form factors:
- **Mobile Apps (iOS / Android)**: Constrained cellular bandwidth, high-latency radio links, limited battery life, and compact screen layouts. Over-fetching data drains batteries and slows screen loads.
- **Desktop Web Browsers**: High-bandwidth fiber connections, large monitors requiring rich tabular views, deep metadata, and multi-column layouts.
- **Public API Partners / IoT**: Highly standardized, machine-readable payloads with strict rate limiting.

Attempting to build a single "one-size-fits-all" REST or GraphQL endpoint forces compromise: mobile clients download megabytes of unnecessary JSON, or web clients are forced into dozens of consecutive chatty HTTP round-trips to assemble a complete page.

---

### The BFF Solution Topology

The **Backend for Frontend (BFF)** pattern introduces dedicated, lightweight server-side presentation adapters tailored to specific frontend client applications:

```mermaid
flowchart TD
    subgraph Clients["Frontend Clients"]
        Mobile["Mobile App<br/>(iOS / Android)"]
        Web["Desktop Web App<br/>(React / Next.js)"]
        Partner["External Partner API"]
    end

    subgraph BFFTier["Backend for Frontend (BFF) Tier"]
        MobileBFF["Mobile BFF<br/>(Trims payloads, single composite call)"]
        WebBFF["Web BFF<br/>(Rich metadata, full tables, pagination)"]
        PartnerBFF["Partner Gateway<br/>(OAuth2, strict schemas)"]
    end

    subgraph CoreServices["Downstream Core Bounded Contexts"]
        Ordering["Ordering Service"]
        Inventory["Inventory Service"]
        Shipping["Shipping Service"]
    end

    Mobile --> MobileBFF
    Web --> WebBFF
    Partner --> PartnerBFF

    MobileBFF --> Ordering & Inventory & Shipping
    WebBFF --> Ordering & Inventory & Shipping
    PartnerBFF --> Ordering
```

### Core Responsibilities of a BFF
1. **Response Shaping & Payload Trimming**:
   - Mobile BFF strips verbose product descriptions, internal system IDs, and secondary tracking timestamps, returning only the 5 fields needed to render a mobile card.
2. **Fan-Out & Request Aggregation**:
   - A single client request to `/api/mobile/orders/123` fans out asynchronously to Ordering, Inventory, and Shipping downstream services and consolidates them into a single HTTP response, reducing cellular round-trips from 3 to 1.
3. **Client-Specific Authentication & Session Adaptation**:
   - Translates mobile biometric tokens or OAuth2 refresh tokens into internal mTLS microservice tokens.

---

### The Golden Rule of BFF: Presentation Only, Zero Domain Invariants

```mermaid
flowchart LR
    subgraph Allowed["Allowed in BFF"]
        A1["DTO Mapping & Field Trimming"]
        A2["Parallel Request Fan-Out"]
        A3["Response Caching & Formatting"]
    end

    subgraph Forbidden["STRICTLY FORBIDDEN in BFF"]
        F1["Business Invariant Enforcement"]
        F2["Domain Price / Tax Calculation"]
        F3["Direct Database Access"]
    end
```

> [!CAUTION]
> **The BFF Leaky Domain Anti-Pattern**:
> A BFF is an **API presentation adapter**, never a business domain service. If business logic (e.g. discount calculation, tax rules, or order status validation) is placed in the Mobile BFF, it must inevitably be duplicated in the Web BFF. This creates divergent business rules, split-brain state mutations, and regression nightmares.
> **Rule**: All business invariants belong strictly in the downstream DDD Aggregate Roots. The BFF merely formats and orchestrates delivery.

---

## 8. Event-Driven Saga Pattern (Distributed Transactions)

### The Fallacy of Two-Phase Commit (2PC/XA)
In distributed microservices, a single business transaction frequently spans multiple autonomous databases. Traditional distributed transactions using Two-Phase Commit (2PC / XA) suffer fatal production drawbacks:
- **Lock Holding Across Networks**: Coordinators hold database row locks across remote network calls, destroying system throughput.
- **Single Point of Failure**: If the transaction coordinator crashes during the prepare phase, participating databases remain locked indefinitely.
- **CAP Theorem & Availability**: 2PC prioritizes Consistency ($C$) over Availability ($A$), causing cascade failures when any service is unreachable.

### The Saga Solution: Sequence of Local Transactions
A **Saga** is a sequence of local transactions where each transaction updates state within a single service and triggers the next step via messages or events:

```mermaid
flowchart LR
    subgraph Forward["Forward Flow"]
        T1["T1: Create Order<br/>(Ordering Service)"] --> T2["T2: Reserve Stock<br/>(Inventory Service)"]
        T2 --> T3["T3: Charge Card<br/>(Payment Service)"]
    end

    subgraph Failure["Failure at T3 Triggers Compensation"]
        T3 -.->|"Payment Declined"| C2["C2: Restock Inventory<br/>(Compensating Action)"]
        C2 -.-> C1["C1: Cancel Order<br/>(Compensating Action)"]
    end
```

### Orchestration vs. Choreography
| Dimension | Choreography | Orchestration |
|---|---|---|
| **Coordination** | Decentralized: services subscribe to events and emit follow-ups. | Centralized: an explicit coordinator service tells participants what to execute. |
| **Visibility** | Difficult to track overall workflow state; scattered across event handlers. | Simple: central state machine tracks exact progress and status. |
| **Coupling** | Loose coupling; participants don't know who consumes their events. | Participants know the command contracts sent by the orchestrator. |
| **Failure Handling** | Complex cascading rollbacks; high risk of cyclic dependencies. | Straightforward: orchestrator catches errors and triggers compensations in LIFO order. |
| **Best Used When** | Simple 2–3 step workflows across autonomous teams. | Complex, multi-step business transactions ($> 3$ steps) with strict auditability. |

---

## 9. Microkernel / Plugin Architecture

### Core Concept: Separation of Core and Extension Points
The **Microkernel Architecture** (also known as the Plugin Pattern) divides a software system into two primary topologies:
1. **Core Microkernel System**: Minimal, stable foundation containing the essential lifecycle coordination, shared data contexts, and extension registries.
2. **Plugin Components**: Autonomous, specialized modules that plug into the core to extend functionality (e.g. calculation rules, validation strategies, third-party payment adapters) without modifying the core codebase.

```mermaid
flowchart TD
    Core["Microkernel Core Engine<br/>(PricingCoreEngine)"]
    Registry["Plugin Registry<br/>(PricingPluginRegistry)"]
    
    P1["Plugin 1: LoyaltyDiscountPlugin<br/>(Order: 10)"]
    P2["Plugin 2: RegionalCouponPlugin<br/>(Order: 50)"]
    P3["Plugin 3: VatTaxPlugin<br/>(Order: 100)"]

    Registry --> Core
    P1 -.->|"Registers via SPI"| Registry
    P2 -.->|"Registers via SPI"| Registry
    P3 -.->|"Registers via SPI"| Registry
```

### Key Rules for Microkernel Design
- **Open/Closed Principle**: The core engine is closed for modification but open for extension via pluggable SPIs.
- **Contract Stability**: The SPI interface (`PricingRulePlugin`) must remain stable; breaking SPI contracts breaks all third-party plugins.
- **Execution Precedence**: Plugins must declare deterministic priority/ordering (e.g. `@Order`) so calculations execute predictably.

---

## 10. Cell-Based Architecture (Bulkhead at Infrastructure Scale)

### The Blast Radius Problem in Standard Microservices
In standard cloud microservices, an entire global platform typically shares a common pool of compute, database clusters, and message queues. A single poisoned payload, un-indexed query, or runaway background job can cascade across shared resources, triggering an outage that impacts **100% of active users**.

### Cellular Partitioning Mechanics
**Cell-Based Architecture** partitions the system into independent, self-contained, fully functional copies called **Cells**:
- Each cell contains its own complete stack: API gateway, microservice compute, database replicas, and caches.
- Cells share **zero runtime infrastructure** with each other.
- A deterministic **Cell Router** at the edge hashes incoming requests (e.g. `hash(user_id) % cell_count`) and routes users to their assigned cell.

```mermaid
flowchart TD
    Client["Client Traffic"] --> Router["Cell Router (Edge Hashing)"]

    subgraph Cell0["Cell 0 (Tenants 0-25k)"]
        Compute0["Compute Fleet"]
        DB0[("Isolated Database")]
        Compute0 --- DB0
    end

    subgraph Cell1["Cell 1 (Tenants 25k-50k) - [OUTAGE]"]
        Compute1["Compute Fleet"]
        DB1[("Isolated Database")]
        Compute1 --- DB1
    end

    subgraph Cell2["Cell 2 (Tenants 50k-75k)"]
        Compute2["Compute Fleet"]
        DB2[("Isolated Database")]
        Compute2 --- DB2
    end

    Router -->|"Hash(user) == 0"| Cell0
    Router -->|"Hash(user) == 1"| Cell1
    Router -->|"Hash(user) == 2"| Cell2
```

> [!TIP]
> **Blast Radius Containment**:
> If a database deadlock or corrupted deployment causes Cell-1 to crash, **only the users mapped to Cell-1 are impacted**. Cells 0, 2, and all other cells continue operating with 100% availability.

---

## 11. Multi-Tenant Architecture Strategies

Modern enterprise SaaS applications must serve thousands of distinct corporate tenants. The choice of multi-tenancy model is an architectural trade-off between **isolation**, **cost**, and **operational complexity**:

```mermaid
flowchart LR
    subgraph ModelA["1. Database-per-Tenant"]
        T1App["Shared App"] --> DB1[("Tenant 1 DB")]
        T1App --> DB2[("Tenant 2 DB")]
    end

    subgraph ModelB["2. Schema-per-Tenant"]
        T2App["Shared App"] --> S1["Schema: tenant_1"]
        T2App --> S2["Schema: tenant_2"]
        S1 & S2 --- SingleDB[("Single Database Instance")]
    end

    subgraph ModelC["3. Shared Table (Row-Level Security)"]
        T3App["Shared App"] --> SharedTable["Table: orders (WHERE tenant_id = ?)"]
    end
```

| Strategy | Data Isolation | Operational Overhead | Resource Efficiency | Failure Risk |
|---|---|---|---|---|
| **Database-per-Tenant** | **Highest**: Physical database boundary; compliance friendly (HIPAA/GDPR). | High: thousands of database migrations, high connection pool limits. | Low: underutilized database idle capacity. | Zero risk of cross-tenant query leakage. |
| **Schema-per-Tenant** | **Medium**: Logical schema separation within same database server. | Medium: schema-aware connection switching and migration tooling. | Medium: shared CPU and RAM across schemas. | Low: cross-schema query errors rare. |
| **Shared Table (Discriminator / RLS)** | **Lowest**: Logical row-level segregation via `tenant_id` column. | **Lowest**: Single migration script updates all tenant data instantly. | **Highest**: Maximum density; shared connection pools and caching. | High: Developer forgetting `WHERE tenant_id = :id` leaks data unless guarded by DB RLS. |

