package lab.distributeddata.outbox;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private final JdbcClient jdbcClient;

    public JdbcOutboxRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(OutboxEvent event) {
        jdbcClient
                .sql(
                        """
                INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status, created_at)
                VALUES (:id, :aggregateType, :aggregateId, :eventType, :payload, :status, :createdAt)
                """)
                .param("id", event.id())
                .param("aggregateType", event.aggregateType())
                .param("aggregateId", event.aggregateId())
                .param("eventType", event.eventType())
                .param("payload", event.payload())
                .param("status", event.status())
                .param("createdAt", Timestamp.from(event.createdAt()))
                .update();
    }

    @Override
    public List<OutboxEvent> lockPendingBatch(int limit) {
        return jdbcClient
                .sql(
                        """
                SELECT id, aggregate_type, aggregate_id, event_type, payload, status, created_at
                FROM outbox_events
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
                """)
                .param("limit", limit)
                .query(
                        (rs, rowNum) ->
                                new OutboxEvent(
                                        rs.getString("id"),
                                        rs.getString("aggregate_type"),
                                        rs.getString("aggregate_id"),
                                        rs.getString("event_type"),
                                        rs.getString("payload"),
                                        rs.getString("status"),
                                        rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    @Override
    public void markProcessed(String eventId) {
        jdbcClient
                .sql(
                        """
                UPDATE outbox_events
                SET status = 'PROCESSED'
                WHERE id = :id
                """)
                .param("id", eventId)
                .update();
    }

    @Override
    public Optional<OutboxEvent> findById(String eventId) {
        return jdbcClient
                .sql(
                        """
                SELECT id, aggregate_type, aggregate_id, event_type, payload, status, created_at
                FROM outbox_events
                WHERE id = :id
                """)
                .param("id", eventId)
                .query(
                        (rs, rowNum) ->
                                new OutboxEvent(
                                        rs.getString("id"),
                                        rs.getString("aggregate_type"),
                                        rs.getString("aggregate_id"),
                                        rs.getString("event_type"),
                                        rs.getString("payload"),
                                        rs.getString("status"),
                                        rs.getTimestamp("created_at").toInstant()))
                .optional();
    }
}
