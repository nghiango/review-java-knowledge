# Code Review: Cross-Module Database Access in Modular Monolith

## Context
In a modular monolith e-commerce system divided conceptually into `ordering`, `billing`, `inventory`, and `shipping`, a developer implemented `BillingService` to calculate and finalize customer bills.

## Files Under Review
- `BillingService.java` — Core billing service
- `InventoryRepository.java` — Inventory module data access
- `ShippingRepository.java` — Shipping module data access
- `OrderRepository.java` — Ordering module data access

## Review Questions
1. Why does `BillingService` directly injecting `InventoryRepository` and `ShippingRepository` violate Bounded Context isolation?
2. What happens to database schema migrations (e.g., refactoring the `shipping` or `inventory` tables) when external modules query and update them directly?
3. How does direct cross-module data access bypass the domain rules, validation, and lifecycle invariants of the owning module?
4. How should modules in a modular monolith communicate (e.g., published Java API contracts, Spring application events / domain events) to preserve architectural modularity?
