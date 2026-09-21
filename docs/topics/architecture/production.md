# Architecture Production Operations & Incident Guide

Real-world architectural postmortems, structural health telemetry, and production architectural readiness checklists.

---

## 1. Production Incident Postmortems

### Incident 1: The Leaky Domain Outage

#### Incident Timeline
- **11:00 UTC**: Release $v3.2$ deploys to production. The team introduced a new order history endpoint directly returning the JPA entity `Order` from the Spring MVC `@RestController`.
- **11:05 UTC**: Traffic peaks as users refresh order status. Jackson begins serializing the `Order` entity.
- **11:05:15 UTC**: Jackson encounters a lazily-loaded `@OneToMany` relationship: `order.getItems()`. Because the Open-Session-in-View (OSIV) pattern was enabled, Jackson triggers transparent secondary database queries for every item on every order ($N+1$ query avalanche).
- **11:05:30 UTC**: One of the child entities (`OrderItem`) contained a back-reference `item.getOrder()`. Jackson enters an infinite serialization recursion, allocating millions of byte arrays per second.
- **11:06:00 UTC**: HikariCP connection pool on all 30 application pods hits 100% saturation. Garbage collection threads enter continuous stop-the-world pauses attempting to reclaim circular JSON strings.
- **11:07:30 UTC**: Pods fail readiness probes (`/actuator/health/readiness`), Kubernetes begins mass-restarting pods, and the application collapses under total service outage.
- **Total Customer Outage**: **22 minutes of complete e-commerce checkout downtime**.

#### Root Cause Analysis
1. Violation of the **Dependency Inversion and Separation of Concerns Invariant**: Domain/JPA entities were exposed directly as web API presentation DTOs.
2. Relying on Open-Session-in-View (OSIV) kept database connections open during HTTP response streaming, causing connection pool exhaustion.
3. Lack of explicit API response contracts and boundary mappers.

#### Permanent Remediation
1. Mandatory Hexagonal Architecture rule: Domain models and JPA entities are strictly forbidden from appearing in `@RestController` method signatures.
2. Web controllers must accept and return dedicated, immutable record DTOs.
3. Disabled OSIV globally: `spring.jpa.open-in-view=false`.
4. Codified an ArchUnit rule: `noClasses().that().resideInAPackage("..domain..").should().beAnnotatedWith(RestController.class)`.

---

### Incident 2: The Cross-Module Schema Lockup

#### Incident Timeline
- **14:00 UTC**: Black Friday shopping begins. The Shipping team deploys an automated batch job to update tracking numbers in the `shipments` table.
- **14:15 UTC**: Simultaneously, the Billing team’s service executes `SELECT * FROM shipments WHERE order_id = ? FOR UPDATE` to calculate shipping tax adjustments before charging customer credit cards.
- **14:15:30 UTC**: Deadlock detected: The Shipping batch job acquired row locks on `shipments` and attempted to update `orders`, while Billing acquired row locks on `orders` and attempted to acquire row locks on `shipments`.
- **14:16:00 UTC**: PostgreSQL transactions deadlock and abort. Billing service retries failed transactions immediately without backoff.
- **14:17:00 UTC**: Cascading connection pool starvation spreads to both the Billing and Shipping microservice pods. Customer checkouts fail with HTTP 500 errors.
- **Total Customer Outage**: **15 minutes of checkout failures, affecting 1,800 active purchases**.

#### Root Cause Analysis
1. Violation of **Bounded Context Isolation**: The Billing module directly queried and locked tables owned by the Shipping bounded context.
2. Uncoordinated cross-module locking order created circular lock wait graphs in PostgreSQL.
3. Absence of asynchronous event decoupling between billing and shipping.

#### Permanent Remediation
1. **Strict Table Ownership**: The `shipments` table is owned exclusively by the Shipping bounded context. All direct SQL queries from Billing were eliminated.
2. **Event-Driven Decoupling**: Billing publishes an `OrderBilledEvent` via the Transactional Outbox pattern. Shipping listens asynchronously, completely eliminating cross-table lock contention.
3. **ArchUnit Slices Test**: Added an ArchUnit test verifying that modules in the monolith cannot access repositories of other modules.

---

## 2. Architectural Health Telemetry & Metrics

| Telemetry Metric | Target / SLO | Senior Diagnostic Meaning |
|---|---|---|
| **ArchUnit Violations in CI** | $0$ (Strict blocking) | Any non-zero count indicates boundary erosion or illegal cross-layer shortcut. |
| **Domain Test Suite Duration** | $< 500\text{ ms}$ | Pure domain unit tests must execute in milliseconds; slow tests indicate leaked I/O. |
| **Inter-Module Cyclic Dependencies** | $0$ (Acyclic Graph) | Cycles between modules indicate blurred bounded contexts and high coupling. |
| **Afferent Coupling ($C_a$) of Domain** | High | Number of external classes depending on domain ports (high is expected for core business). |
| **Efferent Coupling ($C_e$) of Domain** | $0$ | Number of external infrastructure libraries domain depends on (must be strictly zero). |
| **Instability Metric ($I = \frac{C_e}{C_a + C_e}$)** | $0.0$ for Domain Core | A value of $0.0$ indicates maximal stability (pure domain depends on nothing). |

---

## 3. Production Readiness Checklist

- [ ] **Dependency Inversion Enforced**: Core domain entities and value objects import zero framework dependencies (`org.springframework.*`, `jakarta.persistence.*`, `com.fasterxml.jackson.*`).
- [ ] **Bounded Context Table Isolation**: Each bounded context exclusively owns its database tables. Zero cross-module SQL joins or foreign repository injections.
- [ ] **Aggregate Invariant Protection**: Entities do not expose public setters; collection getters return unmodifiable views.
- [ ] **Automated ArchUnit CI Gate**: ArchUnit rules run on every `./gradlew test` invocation and fail CI on any layer violation.
- [ ] **No Open-Session-in-View (OSIV)**: `spring.jpa.open-in-view=false` is explicitly set to prevent connection pool exhaustion during HTTP rendering.
- [ ] **Explicit API Contracts (DTOs)**: Controllers map incoming requests to commands and outgoing responses to DTOs; domain entities are never serialized directly to HTTP clients.
- [ ] **Eventual Consistency Decoupling**: Inter-module side effects communicate via Domain Events backed by the Transactional Outbox pattern.
