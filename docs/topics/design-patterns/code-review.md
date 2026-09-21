# Design Patterns Code Review Exercises

Real-world code review scenarios demonstrating common pattern anti-patterns, maintainability traps, and ordering vulnerabilities.

---

## 1. Procedural Switch-on-Type Growth

### Context
A payment processing service handles transactions across multiple payment rails (`CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, `CRYPTO`, `BANK_TRANSFER`).
Review `PaymentProcessor.java` below for Open/Closed Principle violations and testability bottlenecks.

```java
--8<-- "modules/29-design-patterns/broken-examples/switch-on-type-growth/PaymentProcessor.java"
```

??? question "Reveal code review findings"
    - **Open/Closed Violation**: Adding a new payment rail (e.g. `APPLE_PAY`) requires modifying multiple methods (`processPayment`, `calculateFee`, `refund`).
    - **Testing Bottleneck**: All third-party dependencies are coupled inside a single class, preventing isolated unit testing.
    - **Fix**: Refactor using the **Strategy Pattern** paired with a Spring-managed **Strategy Factory Registry**.
    - Full analysis: [Detailed Solution for Switch-on-Type Growth](solutions.md#1-switch-on-type-growth-solution).

---

## 2. Pattern Over-Engineering

### Context
A developer implemented a utility to export user records to CSV format. To make it "future-proof", they introduced nested Bridge, Factory, and Visitor abstractions. Review the implementation below:

```java
--8<-- "modules/29-design-patterns/broken-examples/pattern-over-engineering/UserExportService.java"
```

??? question "Reveal code review findings"
    - **Premature Abstraction & YAGNI**: An overly complex object graph (Visitor, Builder, Bridge) was created to solve a trivial 3-field string concatenation.
    - **High Cognitive Overhead**: Reading and maintaining the code requires traversing 5 layers of indirection.
    - **Fix**: Refactor to idiomatic Java 21 records and straightforward string formatting.
    - Full analysis: [Detailed Solution for Pattern Over-Engineering](solutions.md#2-pattern-over-engineering-solution).

---

## 3. Decorator Order Bug (Authorization Leak)

### Context
A query service implements caching and authorization via decorators. Review the composition order in `OrderQueryServiceFactory.java`:

```java
--8<-- "modules/29-design-patterns/broken-examples/decorator-order-bug/OrderQueryServiceFactory.java"
```

??? question "Reveal code review findings"
    - **Security Vulnerability (Privilege Escalation)**: The caching decorator wraps the authorizing decorator. Once an Admin retrieves a confidential order, it is stored in cache. Subsequent requests from unauthorized guests hit the cache and retrieve the confidential record without executing authorization!
    - **Fix**: Invert decorator ordering: Authorization must wrap Caching (`Client -> AuthorizingDecorator -> CachingDecorator -> TargetService`).
    - Full analysis: [Detailed Solution for Decorator Order Bug](solutions.md#3-decorator-order-bug-solution).
