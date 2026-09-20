# Code Review: Comprehensive Order Processing Workflow PR

## Background
A junior engineer submitted a pull request implementing the core order processing workflow. The class combines order creation, remote payment charging, status updates, and email notifications. In production staging, several anomalies were discovered: database connection pools were exhausted during payment gateway slowness, transactions did not roll back when validation failed, and emails were dispatched for failed orders.

## Questions to Consider
1. What happens when `processOrder` calls `executeTransaction` internally?
2. Why is executing `chargePaymentGateway` inside `@Transactional` detrimental to connection pool availability?
3. How does throwing `java.lang.Exception` affect Spring transaction rollback?
4. How should external side effects (like email notifications) be orchestrated around database commits?
