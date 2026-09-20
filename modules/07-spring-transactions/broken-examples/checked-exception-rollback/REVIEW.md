# Code Review: Checked Exception Rollback Assumption

## Background
`OrderPlacementService` throws `OrderValidationException` when validation fails. The developer assumed that any thrown exception automatically triggers a transaction rollback. In production, when `OrderValidationException` was thrown, invalid order records were committed and persisted.

## Questions to Consider
1. What is Spring's default rollback rule for `@Transactional` methods?
2. Which exception classes trigger automatic rollback by default?
3. How can rollback behavior be configured explicitly for checked exceptions?
