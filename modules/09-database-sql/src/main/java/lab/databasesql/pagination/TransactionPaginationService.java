package lab.databasesql.pagination;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

@Service
public class TransactionPaginationService {

    private final JdbcClient jdbcClient;

    public TransactionPaginationService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record TransactionRecord(
            Long id, String accountId, BigDecimal amount, String status, Instant createdAt) {}

    public record Cursor(Instant createdAt, Long id) {}

    public record KeysetPage<T>(List<T> items, Cursor nextCursor, boolean hasMore) {}

    /**
     * Keyset (Seek) Pagination: Directly traverses B-tree index in O(log N) time, avoiding
     * scanning/discarding previous rows.
     */
    public KeysetPage<TransactionRecord> getTransactionsKeyset(
            String accountId, Cursor cursor, int limit) {
        int fetchSize = limit + 1; // Fetch 1 extra to check hasMore

        List<TransactionRecord> items;
        if (cursor == null) {
            items =
                    jdbcClient
                            .sql(
                                    "SELECT id, account_id, amount, status, created_at "
                                            + "FROM transactions "
                                            + "WHERE account_id = :accountId "
                                            + "ORDER BY created_at DESC, id DESC "
                                            + "LIMIT :fetchSize")
                            .param("accountId", accountId)
                            .param("fetchSize", fetchSize)
                            .query(TransactionRecord.class)
                            .list();
        } else {
            items =
                    jdbcClient
                            .sql(
                                    "SELECT id, account_id, amount, status, created_at "
                                            + "FROM transactions "
                                            + "WHERE account_id = :accountId "
                                            + "  AND (created_at < :cursorCreatedAt "
                                            + "       OR (created_at = :cursorCreatedAt AND id < :cursorId)) "
                                            + "ORDER BY created_at DESC, id DESC "
                                            + "LIMIT :fetchSize")
                            .param("accountId", accountId)
                            .param("cursorCreatedAt", cursor.createdAt())
                            .param("cursorId", cursor.id())
                            .param("fetchSize", fetchSize)
                            .query(TransactionRecord.class)
                            .list();
        }

        boolean hasMore = items.size() > limit;
        List<TransactionRecord> resultList = hasMore ? items.subList(0, limit) : items;

        Cursor nextCursor = null;
        if (!resultList.isEmpty() && hasMore) {
            TransactionRecord lastItem = resultList.get(resultList.size() - 1);
            nextCursor = new Cursor(lastItem.createdAt(), lastItem.id());
        }

        return new KeysetPage<>(resultList, nextCursor, hasMore);
    }
}
