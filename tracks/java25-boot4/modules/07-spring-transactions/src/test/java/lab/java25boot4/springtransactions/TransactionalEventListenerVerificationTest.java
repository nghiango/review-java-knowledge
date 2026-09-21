package lab.java25boot4.springtransactions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransactionalEventListenerVerificationTest {

    @Test
    @DisplayName("Should receive transactional event successfully")
    void onOrderCompleted_recordsEvent() {
        var listener = new TransactionalEventListenerVerification();
        var event =
                new TransactionalEventListenerVerification.OrderCompletedEvent("ORD-77", 199.99);

        listener.onOrderCompleted(event);

        assertThat(listener.isEventReceived()).isTrue();
    }
}
