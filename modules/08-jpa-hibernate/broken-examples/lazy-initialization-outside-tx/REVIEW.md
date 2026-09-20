# Code Review: Lazy Initialization Outside Transaction

## Context
The reporting service generates customer contract summaries. Customers are loaded within a transactional method, but the contract count aggregation is performed in a downstream processing loop.

## Code Under Review
- `Customer.java` — Customer entity with lazily loaded `contracts` relationship.
- `Contract.java` — Contract child entity.
- `CustomerExportService.java` — Service loading customers in a transactional helper and iterating contracts in `exportContractSummaryReport()`.

## Review Questions
1. What runtime exception will occur when `exportContractSummaryReport()` executes in production?
2. Why does the transactional boundary around `loadCustomers()` fail to support lazy property traversal in downstream callers?
3. What are the recommended architectural solutions to resolve this issue without enabling anti-patterns like Open Session in View (OSIV)?
