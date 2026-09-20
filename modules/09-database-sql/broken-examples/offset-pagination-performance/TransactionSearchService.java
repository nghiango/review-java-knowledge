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
                                + "LIMIT :pageSize OFFSET :offset")
                .param("accountId", accountId)
                .param("pageSize", pageSize)
                .param("offset", offset)
                .query(TransactionRecord.class)
                .list();
    }
}
