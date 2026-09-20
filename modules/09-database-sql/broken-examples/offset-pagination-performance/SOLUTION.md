# Solution: Deep Offset Pagination Performance

## Annotated Code

### `TransactionSearchService.java`
```java
package lab.databasesql.broken.pagination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class TransactionSearchService {

    private final JdbcClient jdbcClient;

    public TransactionSearchService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record TransactionRecord(
            Long id,
            String accountId,
            BigDecimal amount,
            String status,
            Instant createdAt) {}

    public List<TransactionRecord> getTransactionsPage(
            String accountId, int pageNumber, int pageSize) {
        int offset = pageNumber * pageSize;
        return jdbcClient
                .sql(
                        "SELECT id, account_id, amount, status, created_at "
                                + "FROM transactions "
                                + "WHERE account_id = :accountId "
                                + "ORDER BY created_at DESC "
                                // Performance issue: OFFSET scans and discards N previous rows off disk, degrading to O(N) response time for deep pages and causing data drift under concurrent inserts
                                + "LIMIT :pageSize OFFSET :offset")
                .param("accountId", accountId)
                .param("pageSize", pageSize)
                .param("offset", offset)
                .query(TransactionRecord.class)
                .list();
    }
}
```

---

## Issues Found

| Issue | Severity | Category | Description |
|---|---|---|---|
| Linear Performance Degradation with `OFFSET` | High | Performance | The database must read, sort, and process all preceding offset rows before returning the requested slice. For deep pages, this causes heavy disk I/O, CPU consumption, and buffer cache eviction. |
| Pagination Data Drift / Duplicate Records | Medium | Reliability | When new records are inserted while a user navigates pages, rows shift position, causing users to see duplicate items or skip records entirely between pages. |

---

## Remediation Strategy

1. **Implement Keyset / Seek Pagination:**
   Filter using an indexed, monotonically increasing or tie-breaker column (`WHERE (created_at, id) < (:lastCreatedAt, :lastId) ORDER BY created_at DESC, id DESC LIMIT :pageSize`).
2. **Deterministic B-Tree Traversal:**
   Keyset pagination directly seeks the next B-tree leaf node in $O(\log N)$ time, yielding constant latency ($O(1)$) regardless of whether querying page 1 or page 10,000.
