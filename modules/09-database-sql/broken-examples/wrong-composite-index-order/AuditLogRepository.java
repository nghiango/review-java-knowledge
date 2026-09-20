package lab.databasesql.broken.indexing;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogRepository {

    private final JdbcClient jdbcClient;

    public AuditLogRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record AuditLogEntry(
            Long id,
            String tenantId,
            String status,
            Instant createdAt,
            String action,
            String details) {}

    public List<AuditLogEntry> findRecentLogsByTenant(
            String tenantId, Instant since, int limit) {
        return jdbcClient
                .sql(
                        "SELECT id, tenant_id, status, created_at, action, details "
                                + "FROM audit_logs "
                                + "WHERE tenant_id = :tenantId AND created_at >= :since "
                                + "ORDER BY created_at DESC LIMIT :limit")
                .param("tenantId", tenantId)
                .param("since", since)
                .param("limit", limit)
                .query(AuditLogEntry.class)
                .list();
    }
}
