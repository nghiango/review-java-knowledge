# Code Review: Lost Update Without Locking

## Context
A payment gateway service implements account balance debits. The service reads the account balance, checks if sufficient funds exist, computes the updated amount in memory, and writes the new balance back to the database.

## Code Under Review
- `BankAccountService.java` — Transactional method `withdraw(accountId, amount)`.

## Review Questions
1. What database isolation anomaly occurs when two concurrent threads withdraw from the same account simultaneously?
2. Why does the default `READ COMMITTED` isolation level not prevent this race condition?
3. What are the three production patterns to eliminate lost updates (atomic update, optimistic versioning, pessimistic locking)?
