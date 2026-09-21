# Solution: Domain Model Depending on Infrastructure

## Annotated Code

### `Order.java`

```java
package lab.architecture.broken.domaininfrastructure;

// Maintainability issue: Domain entity coupled directly to database and framework infrastructure annotations
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Maintainability issue: An entity should not be a Spring bean singleton/component
@Entity
@Table(name = "orders")
@Component
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerEmail;
    private BigDecimal totalAmount;
    private String status;

    // Maintainability issue: Domain entity manages its own persistence lifecycle and repository dependency
    @Autowired
    @Transient
    private OrderRepository orderRepository;

    public Order() {
    }

    public Order(String customerEmail, BigDecimal totalAmount) {
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.status = "CREATED";
    }

    // Performance issue: External HTTP call invoked synchronously inside domain state transition
    // Testing issue: Domain logic cannot be tested as a pure POJO without mock framework injection
    public void processPaymentAndConfirm(PaymentClient paymentClient) {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Reliability issue: Direct side-effect execution inside domain model obscures transaction and error boundaries
        boolean success = paymentClient.chargeCreditCard(customerEmail, totalAmount);
        if (success) {
            this.status = "PAID";
            if (orderRepository != null) {
                // Maintainability issue: Active Record anti-pattern in DDD aggregate root violates single responsibility
                orderRepository.save(this);
            }
        } else {
            this.status = "PAYMENT_FAILED";
        }
    }

    public Long getId() {
        return id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setOrderRepository(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

## Issue Catalogue

1. **Framework & Infrastructure Coupling in Domain Model**:
   - Marking `Order` with `@Entity`, `@Table`, and `@Component` leaks JPA and Spring Framework details directly into the core business domain.
   - The domain model becomes bound to the relational schema and Spring container lifecycle.
2. **Domain Entity Self-Persistence (Active Record Anti-Pattern)**:
   - Injecting `OrderRepository` into `Order` violates the Single Responsibility Principle. Domain models represent state and business invariants, not data access coordination.
3. **External Remote I/O Inside Business Logic**:
   - Calling `paymentClient.chargeCreditCard(...)` directly inside `Order` mixes business rules with external network communication.
4. **Poor Testability**:
   - Pure domain logic should be testable with plain JUnit assertions in microseconds without Spring test runners, reflection, or complex mocking.

## Correct Implementation

The production-grade solution decouples the domain model using **Hexagonal Architecture (Ports and Adapters)**:
- Pure POJO Domain: `lab.architecture.cleanarchitecture.domain.model.Order` has zero framework annotations.
- Outgoing Port: `PaymentPort` interface lives in domain layer.
- Use Case / Application Service: `OrderApplicationService` orchestrates fetching the entity, calling external ports, executing domain state transitions, and persisting results.
- Infrastructure Adapter: `StripePaymentAdapter` implements `PaymentPort`.
