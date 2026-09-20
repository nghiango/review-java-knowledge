# Code Review: Deep Offset Pagination Performance

## Context
A financial ledger API provides paginated transaction history for customer accounts using standard `LIMIT :pageSize OFFSET :offset` query parameters.

## Code Under Review
- `TransactionSearchService.java` — Service method `getTransactionsPage(accountId, pageNumber, pageSize)` executing SQL with `OFFSET`.

## Review Questions
1. How does the database engine execute an `OFFSET 500000 LIMIT 20` query internally?
2. What are the two major flaws of offset pagination in high-traffic applications (performance degradation and data drift)?
3. How does Keyset / Cursor-based pagination achieve constant $O(1)$ query time regardless of page depth?
