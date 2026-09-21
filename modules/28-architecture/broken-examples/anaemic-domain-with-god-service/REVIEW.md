# Code Review: Anaemic Domain Model with God Service

## Context
A development team implemented the order processing subsystem using a typical three-tier layered architecture. The entity classes hold data, and a central `OrderManagementService` contains all business operations.

## Files Under Review
- `Order.java` — Data holder for orders
- `OrderItem.java` — Data holder for line items
- `OrderManagementService.java` — Service orchestrating logic and state mutations

## Review Questions
1. Why does this design represent an "Anaemic Domain Model" anti-pattern?
2. What happens to business invariants when any calling class or controller can call `order.setStatus(...)` or `order.getItems().add(...)` directly?
3. Notice `addItemToOrder` and `cancelOrder`: where are the inconsistencies in state validation?
4. How does moving business logic into a DDD Aggregate Root protect invariants and reduce god-service bloat?
