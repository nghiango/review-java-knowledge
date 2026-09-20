# Code Review: Unindexed Foreign Key and Missing Index

## Context
An e-commerce system manages customer orders and line items. The relational schema defines a foreign key `order_items.order_id REFERENCES orders(id) ON DELETE CASCADE`.

## Code Under Review
- `schema.sql` — Table definitions for `orders` and `order_items`.
- `OrderRepository.java` — Repository querying order items by `order_id` and deleting parent orders.

## Review Questions
1. Why does PostgreSQL not automatically index foreign key columns when a foreign key constraint is declared?
2. What happens to query execution plans when `findItemsByOrderId(orderId)` runs against a table with 20 million rows?
3. What happens to lock acquisition when `deleteOrder(orderId)` executes under high concurrency?
