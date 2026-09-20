package lab.distributeddata.inbox;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcInboxRepository implements InboxRepository {

    private final JdbcClient jdbcClient;

    public JdbcInboxRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean tryAcquireLease(String messageId, String consumerGroup) {
        try {
            int rows =
                    jdbcClient
                            .sql(
                                    """
                    INSERT INTO inbox_messages (message_id, consumer_group, processed_at)
                    VALUES (:messageId, :consumerGroup, :processedAt)
                    ON CONFLICT (message_id, consumer_group) DO NOTHING
                    """)
                            .param("messageId", messageId)
                            .param("consumerGroup", consumerGroup)
                            .param("processedAt", Timestamp.from(Instant.now()))
                            .update();
            return rows > 0;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    @Override
    public boolean isProcessed(String messageId, String consumerGroup) {
        Integer count =
                jdbcClient
                        .sql(
                                """
                SELECT COUNT(*) FROM inbox_messages
                WHERE message_id = :messageId AND consumer_group = :consumerGroup
                """)
                        .param("messageId", messageId)
                        .param("consumerGroup", consumerGroup)
                        .query(Integer.class)
                        .single();
        return count != null && count > 0;
    }
}
