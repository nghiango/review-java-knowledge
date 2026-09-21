# Architecture Code Review Practice

Review the following architectural pull requests, domain models, and service boundary implementations. Identify boundary violations, leaky abstractions, anemic domain models, and cross-bounded-context database access before expanding the solution panels.

---

## 1. Domain Model Depending on Infrastructure & Frameworks

A pull request was submitted introducing an `Order` domain entity. The developer attempted to place all business operations directly on the entity while mixing database persistence and external payment calls into its methods.

### Broken Target: `Order.java`

--8<-- "modules/28-architecture/broken-examples/domain-depending-on-infrastructure/Order.java"

### Broken Target: `PaymentClient.java`

--8<-- "modules/28-architecture/broken-examples/domain-depending-on-infrastructure/PaymentClient.java"

### Broken Target: `OrderRepository.java`

--8<-- "modules/28-architecture/broken-examples/domain-depending-on-infrastructure/OrderRepository.java"

??? question "Reveal issues"
    1. **Framework & Infrastructure Coupling in Domain Model**:
       - `Order` imports `jakarta.persistence.*` and `org.springframework.*`, annotating the core business entity with `@Entity`, `@Table`, and `@Component`.
       - The domain model is tightly coupled to the relational persistence provider and Spring singleton lifecycle, making domain logic impossible to run without framework baggage.
    2. **Active Record Anti-Pattern & Self-Persistence**:
       - Injecting `OrderRepository` into `Order` (`@Autowired private transient OrderRepository orderRepository`) forces the entity to manage its own persistence lifecycle, violating the Single Responsibility Principle.
    3. **Synchronous External I/O Inside Domain Entity**:
       - `processPaymentAndConfirm` invokes `paymentClient.chargeCreditCard(...)` directly inside the domain model, entangling business rules with network communication and remote latency.
    4. **Impaired Testability**:
       - Pure domain logic cannot be tested with fast, isolated unit tests; tests require Spring test context or mock injection.

---

## 2. Anaemic Domain Model with Procedural God Service

An engineering team designed an order management subsystem where entity classes act as passive property bags, and all business rules, total calculations, and state validations live inside a single procedural `OrderManagementService`.

### Broken Target: `Order.java`

--8<-- "modules/28-architecture/broken-examples/anaemic-domain-with-god-service/Order.java"

### Broken Target: `OrderItem.java`

--8<-- "modules/28-architecture/broken-examples/anaemic-domain-with-god-service/OrderItem.java"

### Broken Target: `OrderManagementService.java`

--8<-- "modules/28-architecture/broken-examples/anaemic-domain-with-god-service/OrderManagementService.java"

??? question "Reveal issues"
    1. **Anaemic Domain Model Anti-Pattern**:
       - `Order` and `OrderItem` contain zero business logic and expose public getters and setters for all fields.
       - Any caller across the codebase can directly mutate `order.setStatus("PAID")` or alter items without executing business validations.
    2. **Leaked Encapsulation via Direct Collection Mutation**:
       - Exposing `getItems()` returning a mutable `List<OrderItem>` allows external code to execute `order.getItems().add(item)` directly, completely bypassing item validation and leaving `totalAmount` out of sync.
    3. **Scattered Validation & Duplicated Calculation**:
       - Total order calculation is recomputed procedurally inside `OrderManagementService` rather than encapsulated within the aggregate.
    4. **Inconsistent State Machine Transitions**:
       - `cancelOrder` sets status directly to `CANCELLED` without verifying if the order was already `SHIPPED` or settled, corrupting business state.
    5. **Lack of Value Objects**:
       - Raw `BigDecimal` and `int` primitives allow negative amounts and invalid quantities without domain-level self-validation.

---

## 3. Cross-Module Database Access in Modular Monolith

In a modular monolith system, `BillingService` directly queries and mutates database repositories belonging to other bounded contexts (`inventory` and `shipping`).

### Broken Target: `BillingService.java`

--8<-- "modules/28-architecture/broken-examples/cross-module-database-access/BillingService.java"

### Broken Target: `InventoryRepository.java`

--8<-- "modules/28-architecture/broken-examples/cross-module-database-access/InventoryRepository.java"

### Broken Target: `ShippingRepository.java`

--8<-- "modules/28-architecture/broken-examples/cross-module-database-access/ShippingRepository.java"

### Broken Target: `OrderRepository.java`

--8<-- "modules/28-architecture/broken-examples/cross-module-database-access/OrderRepository.java"

??? question "Reveal issues"
    1. **Violation of Bounded Context Isolation**:
       - `BillingService` injects `InventoryRepository` and `ShippingRepository` directly, treating foreign modules as direct database access libraries rather than independent bounded contexts.
    2. **High Coupling & Fragile Schema Evolution**:
       - If the Shipping team alters the `shipments` table or renames columns, the Billing module breaks unexpectedly at runtime.
    3. **Bypassed Domain Invariants**:
       - Calling `shippingRepository.updateShipmentStatus(orderId, "BILLING_COMPLETED")` writes directly to the database without invoking Shipping domain logic or firing shipping domain events.
    4. **Barrier to Service Extraction**:
       - Direct database sharing permanently prevents any bounded context from ever being extracted into an autonomous microservice.
