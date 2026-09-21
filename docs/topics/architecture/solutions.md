# Architecture Solutions & Design Trade-offs

Detailed architectural walkthroughs, trade-off analyses, and production-grade implementations correcting the anti-patterns identified in [Code Review](code-review.md).

---

## 1. Hexagonal Architecture: Pure Domain Core & Dependency Inversion

### Correct Implementation: Domain Aggregate Root (`Order.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/cleanarchitecture/domain/model/Order.java"

### Correct Implementation: Driven Port (`PaymentPort.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/cleanarchitecture/domain/port/out/PaymentPort.java"

### Correct Implementation: Application Service (`OrderApplicationService.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/cleanarchitecture/application/service/OrderApplicationService.java"

### Architectural Rationale & Trade-offs

1. **Zero Framework Dependencies in Domain**:
   - `Order` is a pure Java class. It imports nothing from Spring, Jakarta Persistence, or external infrastructure.
   - Domain invariants (e.g. currency consistency, non-negative totals, lifecycle states) are enforced entirely in memory.
2. **Ports Define Dependencies as Contracts**:
   - Outbound dependencies are declared as interfaces (`OrderRepositoryPort`, `PaymentPort`) inside the domain/application layer.
   - The infrastructure adapter depends on the domain port; the domain has zero knowledge of the adapter.
3. **Decoupled Application Orchestration**:
   - `OrderApplicationService` orchestrates the checkout process: instantiates the aggregate, coordinates external payment through `PaymentPort`, triggers `order.markPaid()`, and saves state via `OrderRepositoryPort`.
4. **Trade-offs**:
   - **Object Mapping Overhead**: Requires mapping between JPA persistence entities, domain aggregates, and API DTOs.
   - **Increased Initial Boilerplate**: More interfaces, ports, and packages compared to a quick three-tier CRUD application. Justified as domain complexity and team size scale.

---

## 2. DDD Rich Domain Aggregate Root & Value Objects

### Correct Implementation: Aggregate Root (`Order.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/richdomain/Order.java"

### Correct Implementation: Value Object (`Money.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/richdomain/Money.java"

### Architectural Rationale & Trade-offs

1. **Encapsulated Invariant Protection**:
   - The Aggregate Root strictly controls all mutation points. Calling `addOrUpdateItem` automatically updates the internal total and verifies that the order is still in `DRAFT` status.
   - `getItems()` returns `Collections.unmodifiableList(...)`, preventing callers from modifying child collections behind the aggregate's back.
2. **Elimination of Primitive Obsession**:
   - `Money` and `Quantity` encapsulate self-validation upon instantiation. Passing negative quantities or mixed currencies causes an immediate validation failure at compile/runtime boundary.
3. **Thin Application Service**:
   - `OrderApplicationService` contains no business calculations; it merely coordinates persistence and transaction boundaries.
4. **Trade-offs**:
   - **ORM Mapping Friction**: Hibernate and JPA prefer mutable Java beans with default no-arg constructors and setters. Mapping rich aggregates with value objects requires `@Embedded`, custom value converters (`AttributeConverter`), or separate JPA entity mappers.

---

## 3. Modular Monolith: Public API Contracts & Domain Events

### Correct Implementation: Public Interface (`InventoryApi.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/modularmonolith/inventory/InventoryApi.java"

### Correct Implementation: Decoupled Service (`BillingService.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/modularmonolith/billing/BillingService.java"

### Correct Implementation: Event Consumer (`ShippingService.java`)

--8<-- "modules/28-architecture/src/main/java/lab/architecture/modularmonolith/shipping/ShippingService.java"

### Architectural Rationale & Trade-offs

1. **Strict Public vs Internal Encapsulation**:
   - `InventoryApi` is the sole public contract exposed by the `inventory` module. Concrete implementation classes (`InventoryServiceImpl`) reside in an `internal` package and are inaccessible to outside modules.
2. **Event-Driven Decoupling**:
   - Instead of `BillingService` executing SQL updates against the `shipping` table, billing publishes an `OrderBilledEvent`.
   - `ShippingService` listens to the event and executes its own internal business logic in its own bounded context.
3. **ArchUnit Verification**:
   - Architectural boundaries are codified as automated unit tests to ensure no class in `billing` imports anything from `inventory.internal` or `shipping.internal`.
4. **Trade-offs**:
   - **Eventual Consistency**: Decoupled event-driven flows introduce asynchronous completion. Systems must handle transient states and retries gracefully.
