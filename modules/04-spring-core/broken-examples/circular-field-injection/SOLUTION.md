# Solution: Circular dependency and field injection

## Annotated code

```java
@Service
public class OrderService {

    // Maintainability issue: Field injection with @Autowired on private fields hides dependencies,
    // makes instances impossible to instantiate immutably in unit tests without reflection or Spring test runners,
    // and bypasses compiler null-safety checks.
    // Architecture issue: OrderService and BillingService have a direct bidirectional circular dependency,
    // violating clean layering and creating tight coupling.
    @Autowired
    private BillingService billingService;

    public void createOrder(String orderId, double amount) {
        billingService.processInvoice(orderId, amount);
    }

    public void markOrderPaid(String orderId) {
        // Update order status to paid
    }
}

@Service
public class BillingService {

    // Maintainability issue: Field injection hides circular dependency from constructor signature.
    // Architecture issue: Direct reverse dependency on OrderService creates cyclic graph (OrderService -> BillingService -> OrderService).
    @Autowired
    private OrderService orderService;

    public void processInvoice(String orderId, double amount) {
        orderService.markOrderPaid(orderId);
    }
}
```

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Maintainability issue | High | `OrderService.billingService` | Field injection masks dependencies and prevents fast unit testing |
| 2 | Architecture issue | High | `OrderService` $\leftrightarrow$ `BillingService` | Circular dependency creates tight bidirectional coupling |
| 3 | Maintainability issue | High | `BillingService.orderService` | Field injection bypasses explicit constructor contracts |
| 4 | Reliability issue | Medium | Container Startup | Spring Boot 2.6+ / 3.x rejects circular dependencies by default |

## Issue details

### Field injection anti-pattern

**Type:** Maintainability issue · **Severity:** High · **Difficulty:** Basic

Injecting dependencies into private fields via `@Autowired`:
1. Prevents declaring fields as `final`, losing immutability guarantees.
2. Makes unit testing difficult because mock objects cannot be passed directly via constructor `new OrderService(mockBilling)` without resorting to reflection or `@SpringBootTest`.
3. Obscures class complexity (a class with 10 `@Autowired` fields appears deceptively simple without a large constructor).

### Circular dependency between domain services

**Type:** Architecture issue · **Severity:** High · **Difficulty:** Intermediate

When `OrderService` calls `BillingService` which in turn calls `OrderService`, neither service can exist or be reasoned about in isolation. Spring Boot 2.6+ forbids circular dependencies by default (`BeanCurrentlyInCreationException`).

Fix: Decouple the bidirectional call by using an event-driven model: `OrderService` issues an `OrderCreatedEvent` or `PaymentCompletedEvent` published via `ApplicationEventPublisher`, and `BillingService` listens via `@EventListener`.

## Correct implementation

The production-ready fix lives in `lab.springcore.circular`:
- `OrderService.java` using explicit constructor injection with `final` fields.
- `PaymentNotificationPublisher.java` / `BillingService.java` communicating asynchronously or synchronously via Spring `ApplicationEventPublisher`.
