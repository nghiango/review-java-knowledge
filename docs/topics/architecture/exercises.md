# Architecture Hands-On Exercises

Practical engineering exercises to master Domain-Driven Design aggregate refactoring, ArchUnit fitness rules, and CQRS read model projections.

---

## Exercise 1: Refactoring an Anemic Model into a Rich DDD Aggregate

### Objective
Transform an anemic, procedural order cancellation workflow into an encapsulated Domain-Driven Design Aggregate Root that protects its invariants and emits a domain event.

### Scenario
An existing service contains this procedural method:
```java
public void cancelOrder(Long orderId, String reason) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    if ("SHIPPED".equals(order.getStatus())) {
        throw new IllegalStateException("Already shipped");
    }
    order.setStatus("CANCELLED");
    orderRepository.save(order);
    emailService.sendCancellationEmail(order.getCustomerEmail(), reason);
}
```

### Requirements
1. Encapsulate state transition and invariant validation directly inside `Order`.
2. Introduce an immutable `CancellationReason` Value Object.
3. Decouple email sending by having the aggregate register an `OrderCancelledEvent`.
4. Ensure `Order` cannot be cancelled twice.

### Step-by-Step Implementation

??? question "View solution"
    #### 1. Value Object: `CancellationReason`
    ```java
    public record CancellationReason(String text) {
        public CancellationReason {
            if (text == null || text.trim().length() < 5) {
                throw new IllegalArgumentException("Cancellation reason must be at least 5 characters");
            }
        }
    }
    ```

    #### 2. Rich Aggregate Root: `Order`
    ```java
    public class Order {
        private final OrderId id;
        private OrderStatus status;
        private final List<Object> domainEvents = new ArrayList<>();

        public void cancel(CancellationReason reason) {
            if (this.status == OrderStatus.SHIPPED) {
                throw new IllegalStateException("Cannot cancel an order that has already shipped");
            }
            if (this.status == OrderStatus.CANCELLED) {
                throw new IllegalStateException("Order is already cancelled");
            }
            this.status = OrderStatus.CANCELLED;
            this.domainEvents.add(new OrderCancelledEvent(this.id, reason.text()));
        }

        public List<Object> pullDomainEvents() {
            List<Object> events = List.copyOf(this.domainEvents);
            this.domainEvents.clear();
            return events;
        }
    }
    ```

    #### 3. Lean Application Service
    ```java
    public class OrderApplicationService {
        private final OrderRepositoryPort orderRepository;
        private final EventPublisherPort eventPublisher;

        public void cancelOrder(OrderId orderId, CancellationReason reason) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            order.cancel(reason); // Aggregate protects invariant
            orderRepository.save(order);

            // Dispatch domain events after state change
            for (Object event : order.pullDomainEvents()) {
                eventPublisher.publish(event);
            }
        }
    }
    ```

---

## Exercise 2: Authoring Custom ArchUnit Rules for Hexagonal Boundaries

### Objective
Write an automated ArchUnit architectural fitness test verifying that the core domain is free of framework dependencies and that controllers only call use cases.

### Requirements
1. Ensure classes in package `..domain..` do not import Spring, JPA, Hibernate, or HTTP libraries.
2. Ensure classes in `..controller..` depend only on `..port.in..` (Use Cases) and never directly on repositories.
3. Ensure no field injection (`@Autowired` on fields) exists anywhere in the application.

### Step-by-Step Implementation

??? question "View solution"
    ```java
    import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
    import com.tngtech.archunit.core.domain.JavaClasses;
    import com.tngtech.archunit.core.importer.ClassFileImporter;
    import org.junit.jupiter.api.Test;

    public class StrictArchitectureRulesTest {

        private final JavaClasses classes = new ClassFileImporter().importPackages("lab.app");

        @Test
        void domainMustBePure() {
            noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta.persistence..", "org.hibernate..")
                .because("Domain core must remain pure and framework-agnostic")
                .check(classes);
        }

        @Test
        void controllersMustOnlyCallUseCases() {
            classes()
                .that().resideInAPackage("..controller..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage("..controller..", "..port.in..", "..dto..", "java..", "org.springframework..")
                .because("Controllers must interact only through inbound use case ports")
                .check(classes);
        }

        @Test
        void banFieldInjection() {
            fields()
                .should().notBeAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
                .because("Constructor injection must always be used")
                .check(classes);
        }
    }
    ```

---

## Exercise 3: Designing a CQRS Read Model Materialized View

### Objective
Design an asynchronous CQRS read projection in Spring Boot that listens to `OrderPlacedEvent` and `PaymentConfirmedEvent` and updates a denormalized read model for customer order history.

### Requirements
1. The read model `CustomerOrderSummaryView` contains: `orderId`, `customerId`, `orderDate`, `totalAmount`, `status`.
2. The projector processes events asynchronously and updates the view idempotently.
3. Explain how to handle out-of-order events.

### Step-by-Step Implementation

??? question "View solution"
    #### 1. Denormalized Read View Entity
    ```java
    public record CustomerOrderSummaryView(
        String orderId,
        String customerId,
        Instant orderDate,
        BigDecimal totalAmount,
        String status,
        long eventSequence
    ) {}
    ```

    #### 2. Projector Event Listener with Sequence Check
    ```java
    @Service
    public class OrderHistoryProjector {

        private final OrderSummaryViewRepository viewRepo;

        public OrderHistoryProjector(OrderSummaryViewRepository viewRepo) {
            this.viewRepo = viewRepo;
        }

        @EventListener
        public void on(OrderPlacedEvent event) {
            CustomerOrderSummaryView view = new CustomerOrderSummaryView(
                event.orderId(),
                event.customerId(),
                event.timestamp(),
                event.totalAmount(),
                "PENDING_PAYMENT",
                event.sequence()
            );
            viewRepo.save(view);
        }

        @EventListener
        public void on(PaymentConfirmedEvent event) {
            CustomerOrderSummaryView current = viewRepo.findById(event.orderId()).orElse(null);
            
            // Handle out-of-order event guard: ignore if event sequence is stale
            if (current != null && event.sequence() > current.eventSequence()) {
                CustomerOrderSummaryView updated = new CustomerOrderSummaryView(
                    current.orderId(),
                    current.customerId(),
                    current.orderDate(),
                    current.totalAmount(),
                    "PAID",
                    event.sequence()
                );
                viewRepo.save(updated);
            }
        }
    }
    ```

---

## Exercise 4: Authoring a BDD Specification Driving a DDD Aggregate Root

### Objective
Write an executable BDD acceptance specification using Given-When-Then structure that verifies domain invariant enforcement in a pure DDD Aggregate Root without booting a Spring container.

### Requirements
1. Feature: Account Withdrawal Invariants.
2. Scenario 1: Successful withdrawal updates balance and emits `MoneyWithdrawnEvent`.
3. Scenario 2: Overdraft attempt throws `InsufficientFundsException` and leaves balance unchanged.

### Step-by-Step Implementation

??? question "View solution"
    #### 1. Pure DDD Aggregate Root: `BankAccount`
    ```java
    public class BankAccount {
        private final AccountId id;
        private Money balance;
        private final List<Object> domainEvents = new ArrayList<>();

        public BankAccount(AccountId id, Money initialDeposit) {
            this.id = Objects.requireNonNull(id);
            this.balance = Objects.requireNonNull(initialDeposit);
        }

        public void withdraw(Money amount) {
            if (amount.compareTo(this.balance) > 0) {
                throw new InsufficientFundsException("Insufficient balance for withdrawal: " + amount);
            }
            this.balance = this.balance.subtract(amount);
            this.domainEvents.add(new MoneyWithdrawnEvent(this.id, amount, this.balance));
        }

        public Money getBalance() {
            return balance;
        }

        public List<Object> pullDomainEvents() {
            var events = List.copyOf(this.domainEvents);
            this.domainEvents.clear();
            return events;
        }
    }
    ```

    #### 2. Fast In-Memory BDD Acceptance Test
    ```java
    @DisplayName("Feature: Bank Account Withdrawals")
    class BankAccountBddTest {

        @Test
        @DisplayName("Scenario: Successful withdrawal when funds are sufficient")
        void successfulWithdrawal() {
            // Given
            BankAccount account = new BankAccount(AccountId.of("ACC-1"), Money.usd(500.00));

            // When
            account.withdraw(Money.usd(200.00));

            // Then
            assertThat(account.getBalance()).isEqualTo(Money.usd(300.00));
            // And
            assertThat(account.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(MoneyWithdrawnEvent.class);
        }

        @Test
        @DisplayName("Scenario: Overdraft rejected when balance is insufficient")
        void overdraftRejected() {
            // Given
            BankAccount account = new BankAccount(AccountId.of("ACC-2"), Money.usd(100.00));

            // When / Then
            assertThatThrownBy(() -> account.withdraw(Money.usd(250.00)))
                .isInstanceOf(InsufficientFundsException.class);

            // And balance is unchanged
            assertThat(account.getBalance()).isEqualTo(Money.usd(100.00));
            assertThat(account.pullDomainEvents()).isEmpty();
        }
    }
    ```

---

## Exercise 5: Designing a Resilient Mobile BFF Gateway with Partial Fallbacks

### Objective
Implement a Spring Boot Backend for Frontend (BFF) endpoint for mobile clients that fans out asynchronously to Ordering and Shipping services, returning a compact DTO with graceful partial degradation when Shipping times out.

### Requirements
1. The Mobile BFF exposes `GET /api/mobile/orders/{id}`.
2. It fetches order details from `OrderClient` and tracking status from `ShippingClient` in parallel using `CompletableFuture`.
3. If `ShippingClient` exceeds an 800ms timeout or returns an error, the BFF must degrade gracefully: return HTTP 200 with `shippingStatus = "Tracking Pending"`.
4. Ensure no domain business invariants or calculations reside in the BFF controller.

### Step-by-Step Implementation

??? question "View solution"
    #### 1. Mobile-Optimized Presentation DTO
    ```java
    public record MobileOrderSummaryResponse(
        String orderId,
        String status,
        BigDecimal totalAmount,
        String shippingStatus
    ) {}
    ```

    #### 2. Resilient BFF Controller with Non-Blocking Fan-Out
    ```java
    @RestController
    @RequestMapping("/api/mobile/orders")
    public class MobileOrderBffGateway {

        private final OrderServiceClient orderClient;
        private final ShippingServiceClient shippingClient;
        private final Executor bffExecutor;

        public MobileOrderBffGateway(OrderServiceClient orderClient, 
                                     ShippingServiceClient shippingClient,
                                     @Qualifier("bffTaskExecutor") Executor bffExecutor) {
            this.orderClient = orderClient;
            this.shippingClient = shippingClient;
            this.bffExecutor = bffExecutor;
        }

        @GetMapping("/{id}")
        public CompletableFuture<MobileOrderSummaryResponse> getMobileOrder(@PathVariable String id) {
            CompletableFuture<OrderDto> orderFuture = CompletableFuture.supplyAsync(
                () -> orderClient.getOrder(id), bffExecutor
            );

            CompletableFuture<String> shippingFuture = CompletableFuture.supplyAsync(
                () -> shippingClient.getTrackingStatus(id), bffExecutor
            )
            .completeOnTimeout("Tracking Pending", 800, TimeUnit.MILLISECONDS)
            .exceptionally(ex -> "Tracking Pending");

            return orderFuture.thenCombine(shippingFuture, (order, shippingStatus) ->
                new MobileOrderSummaryResponse(
                    order.id(),
                    order.status(),
                    order.grandTotal(),
                    shippingStatus
                )
            );
        }
    }
    ```
