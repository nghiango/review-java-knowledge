package lab.databasesql.indexing;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class IndexingOptimizationRepository {

    private final JdbcClient jdbcClient;

    public IndexingOptimizationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public record OrderItemDto(
            Long id, Long orderId, String productName, int quantity, double unitPrice) {}

    public record AuditLogDto(
            Long id,
            String tenantId,
            String status,
            Instant createdAt,
            String action,
            String details) {}

    /**
     * Optimized query supported by B-Tree Index on foreign key: CREATE INDEX
     * idx_order_items_order_id ON order_items(order_id);
     */
    public List<OrderItemDto> findItemsByOrderId(Long orderId) {
        return jdbcClient
                .sql(
                        "SELECT id, order_id, product_name, quantity, unit_price "
                                + "FROM order_items WHERE order_id = :orderId")
                .param("orderId", orderId)
                .query(OrderItemDto.class)
                .list();
    }

    /**
     * Optimized query supported by Composite Index with equality-first, sort-second ordering:
     * CREATE INDEX idx_audit_tenant_created ON audit_logs(tenant_id, created_at DESC);
     */
    public List<AuditLogDto> findRecentLogsByTenant(String tenantId, Instant since, int limit) {
        return jdbcClient
                .sql(
                        "SELECT id, tenant_id, status, created_at, action, details "
                                + "FROM audit_logs "
                                + "WHERE tenant_id = :tenantId AND created_at >= :since "
                                + "ORDER BY created_at DESC LIMIT :limit")
                .param("tenantId", tenantId)
                .param("since", since)
                .param("limit", limit)
                .query(AuditLogDto.class)
                .list();
    }
}
