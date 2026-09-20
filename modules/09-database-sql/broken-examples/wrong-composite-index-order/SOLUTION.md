# Solution: Wrong Composite Index Column Order

## Annotated Code

### `schema.sql`
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    action VARCHAR(128) NOT NULL,
    details TEXT
);

-- Performance issue: Violates leftmost prefix rule; queries filtering on tenant_id without status cannot utilize B-tree root/branch seeking, falling back to full index/table scans
CREATE INDEX idx_audit_status_created_tenant ON audit_logs(status, created_at, tenant_id);

-- Remediation: Place equality columns first (tenant_id), followed by range / sort columns (created_at DESC):
CREATE INDEX idx_audit_tenant_created ON audit_logs(tenant_id, created_at DESC);
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Leftmost Prefix Rule Violation | High | Performance | B-tree composite indexes can only be traversed starting from the leading (leftmost) column. Because `status` is column 1 and omitted in the `WHERE` clause, the index cannot be used for direct range seek. |
| Inefficient Equality-Range Ordering | Medium | Performance | In composite indexes, columns queried by exact equality (`tenant_id = ?`) must precede columns queried by inequality/range or sorting (`created_at >= ? ORDER BY created_at DESC`). |

---

## Remediation Strategy

1. **Follow the Equality-Then-Range Rule:**
   Order composite index columns as follows:
   - **Step 1 (Equality):** All columns with exact `=` filters in the query (`tenant_id`).
   - **Step 2 (Sort / Range):** Columns used in `ORDER BY` or range inequality (`created_at DESC`).
   - **Step 3 (Secondary Filters / Projections):** Secondary columns or `INCLUDE (...)` payload attributes.
2. **Correct Index Definition:**
   ```sql
   CREATE INDEX CONCURRENTLY idx_audit_tenant_created ON audit_logs(tenant_id, created_at DESC);
   ```
