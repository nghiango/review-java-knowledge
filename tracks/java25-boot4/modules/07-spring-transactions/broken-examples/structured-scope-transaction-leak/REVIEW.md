# Code Review Target: StructuredTaskScope Subtasks Inside @Transactional

## Overview
`BatchOrderService` processes a batch of order records concurrently by creating a `StructuredTaskScope` inside a `@Transactional` method, forking subtasks that insert order items into the database.

## Code Under Review
Review `BatchOrderService.java`.

## Questions for the Reviewer
1. How does Spring's `PlatformTransactionManager` bind database connections and transactional status to the executing thread?
2. When subtasks run on forked virtual threads within `StructuredTaskScope.open()`, do they inherit the parent thread's active database transaction?
3. If the parent `@Transactional` method throws `IllegalStateException("Simulated batch failure")`, what happens to the items inserted by the child virtual threads?
