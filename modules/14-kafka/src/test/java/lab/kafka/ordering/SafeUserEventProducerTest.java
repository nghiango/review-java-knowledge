package lab.kafka.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class SafeUserEventProducerTest {

    @Mock private KafkaTemplate<String, UserEvent> kafkaTemplate;

    @InjectMocks private SafeUserEventProducer producer;

    @Test
    @DisplayName("Publishes user events with userId as the partition key")
    void publishEvents_keyedByUserId() {
        String userId = "user-999";

        var u1 = producer.publishUserCreated(userId, "{\"name\":\"Alice\"}");
        var u2 = producer.publishUserUpdated(userId, "{\"name\":\"Alice Smith\"}");
        var u3 = producer.publishUserDeleted(userId);

        ArgumentCaptor<UserEvent> captor = ArgumentCaptor.forClass(UserEvent.class);

        verify(kafkaTemplate, times(3))
                .send(eq(SafeUserEventProducer.TOPIC), eq(userId), captor.capture());

        assertThat(captor.getAllValues()).hasSize(3);
        assertThat(captor.getAllValues().get(0).eventType()).isEqualTo("USER_CREATED");
        assertThat(captor.getAllValues().get(1).eventType()).isEqualTo("USER_UPDATED");
        assertThat(captor.getAllValues().get(2).eventType()).isEqualTo("USER_DELETED");
    }
}
