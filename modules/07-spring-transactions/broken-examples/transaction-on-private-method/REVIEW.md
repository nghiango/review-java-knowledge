# Code Review: Transaction on Private Method

## Background
`InventoryService` defines `deductStockTransactional` as a `private` method annotated with `@Transactional`. During an inventory update error, the partial updates were not rolled back.

## Questions to Consider
1. Why does Spring's standard proxy-based AOP ignore `@Transactional` on `private` methods?
2. What compiler or runtime warnings/errors are issued (if any) when `@Transactional` is placed on `private` methods?
3. How should public component boundaries be designed to ensure transactions are properly enforced?
