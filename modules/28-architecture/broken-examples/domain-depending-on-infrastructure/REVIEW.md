# Code Review: Domain Model Depending on Infrastructure

## Context
A developer has implemented an `Order` domain model in an e-commerce application. The goal was to encapsulate order placement and payment execution logic directly within the business entity.

## Files Under Review
- `Order.java` — Core business entity
- `PaymentClient.java` — External payment client
- `OrderRepository.java` — Persistence contract

## Review Questions
1. What architectural boundaries are violated by having `Order` depend on Jakarta Persistence (`@Entity`, `@Table`) and Spring Framework (`@Component`, `@Autowired`)?
2. Why should domain models never perform external I/O or invoke infrastructure components (`PaymentClient`, `OrderRepository`) directly?
3. How does this design impact domain unit testing and framework upgrades?
4. How would you refactor this to Hexagonal / Clean Architecture using the Dependency Inversion Principle (Ports and Adapters)?
