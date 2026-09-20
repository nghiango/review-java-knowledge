# Code Review: Asynchronous Execution Across Transaction Boundaries

## Background
`OrderService` calls `NotificationService.sendOrderConfirmation` annotated with `@Async` inside `createOrder`. Customers reported receiving order confirmation emails for orders that ultimately failed and were rolled back. Additionally, notification worker threads occasionally threw `EntityNotFoundException` when looking up the order in the database.

## Questions to Consider
1. How does Spring manage transaction context across threads (`ThreadLocal`)?
2. When does an `@Async` method execute relative to the caller's transaction commit?
3. How does `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` guarantee notifications fire only after successful database commit?
