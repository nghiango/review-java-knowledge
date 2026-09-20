# Solution: Unindexed Foreign Key and Missing Index

## Annotated Code

### `schema.sql`
```sql
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    -- Performance issue: Foreign key column without explicit index causes sequential table scans on parent join/delete and acquires table-level SHARE ROW EXCLUSIVE locks in parent updates/deletes
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL
);

-- Remediation:
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Missing Index on Foreign Key Column | High | Performance | Unlike primary keys and unique constraints, PostgreSQL does **not** automatically create an index on referencing foreign key columns (`order_items.order_id`). |
| Cascading Deletion Table Locks & Full Scans | Critical | Scalability | Deleting a row from `orders` requires checking/cascading to `order_items`. Without an index on `order_id`, PostgreSQL executes a full sequential scan on `order_items` and acquires extensive table locks, causing severe locking contention and deadlocks under concurrent load. |

---

## Remediation Strategy

1. **Add Explicit B-Tree Index on Foreign Key Columns:**
   ```sql
   CREATE INDEX CONCURRENTLY idx_order_items_order_id ON order_items(order_id);
   ```
2. **Covering Index for Item Queries (Optional):**
   If `findItemsByOrderId` is on the critical performance path, include projected payload columns to enable Index-Only Scans:
   ```sql
   CREATE INDEX idx_order_items_order_id_incl ON order_items(order_id) INCLUDE (product_name, quantity, unit_price);
   ```
