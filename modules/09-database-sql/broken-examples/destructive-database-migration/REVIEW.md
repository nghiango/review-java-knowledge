# Code Review: Destructive Database Migration

## Context
A customer service schema evolves by renaming `full_address` to `delivery_address`. The database migration script `V2__rename_customer_address_column.sql` renames the column directly during deployment.

## Code Under Review
- `V2__rename_customer_address_column.sql` — Migration script with `ALTER TABLE customers RENAME COLUMN full_address TO delivery_address;`.
- `CustomerRepository.java` — Existing application code reading `full_address`.

## Review Questions
1. What happens to existing running application instances when this migration runs in a rolling (or blue/green) deployment environment?
2. What are the four phases of the **Expand and Contract (Parallel Run)** zero-downtime migration pattern?
3. How can database triggers or generated virtual columns facilitate backward-compatible data synchronization?
