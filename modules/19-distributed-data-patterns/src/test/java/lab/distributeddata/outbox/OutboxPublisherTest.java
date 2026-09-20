package lab.distributeddata.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class OutboxPublisherTest {

    @Test
    @DisplayName("Should publish pending outbox events and mark them processed upon broker ACK")
    void publishPendingEvents_success() {
        OutboxRepository repository = mock(OutboxRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        OutboxEvent event1 =
                new OutboxEvent(
                        "evt-1", "Order", "ord-1", "ORDER_CREATED", "{}", "PENDING", Instant.now());
        when(repository.lockPendingBatch(10)).thenReturn(List.of(event1));

        CompletableFuture<SendResult<String, String>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send(eq("orders.topic"), eq("ord-1"), any())).thenReturn(future);

        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate);
        int published = publisher.publishPendingEvents(10, "orders.topic");

        assertThat(published).isEqualTo(1);
        verify(repository, times(1)).markProcessed("evt-1");
    }

    @Test
    @DisplayName("Should do nothing when no pending outbox events exist")
    void publishPendingEvents_emptyBatch() {
        OutboxRepository repository = mock(OutboxRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        when(repository.lockPendingBatch(10)).thenReturn(Collections.emptyList());

        OutboxPublisher publisher = new OutboxPublisher(repository, kafkaTemplate);
        int published = publisher.publishPendingEvents(10, "orders.topic");

        assertThat(published).isEqualTo(0);
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
