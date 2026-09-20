package lab.distributeddata.inbox;

import java.time.Instant;

public record InboxMessage(String messageId, String consumerGroup, Instant processedAt) {}
