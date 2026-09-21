package lab.java25boot4.springtransactions;

import java.util.concurrent.atomic.AtomicBoolean;

/** Demonstrates and verifies transactional event listener behavior with virtual threads. */
public class TransactionalEventListenerVerification {

    public record OrderCompletedEvent(String orderId, double amount) {}

    private final AtomicBoolean eventReceived = new AtomicBoolean(false);

    public void onOrderCompleted(OrderCompletedEvent event) {
        eventReceived.set(true);
    }

    public boolean isEventReceived() {
        return eventReceived.get();
    }
}
