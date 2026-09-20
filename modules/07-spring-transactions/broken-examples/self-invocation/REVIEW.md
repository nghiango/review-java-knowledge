# Code Review: Self-Invocation Transaction Bypass

## Background
A developer wrote `OrderService` where `processOrder` performs validation and calls `placeOrder` annotated with `@Transactional`. During an outage where inventory deduction failed with an exception, orders remained inserted in the database without rollback.

## Questions to Consider
1. How does Spring proxy `@Transactional` methods at runtime?
2. What happens when a method on a Spring bean invokes another method on the same bean (`this`)?
3. How can transactional boundaries be designed so proxies intercept invocations reliably?
