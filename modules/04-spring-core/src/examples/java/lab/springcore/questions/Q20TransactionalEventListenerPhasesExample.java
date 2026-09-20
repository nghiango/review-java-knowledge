package lab.springcore.questions;

/** Q20: Demonstrates Transactional Event Listener Phases (AFTER_COMMIT, AFTER_ROLLBACK). */
@SuppressWarnings("unused")
public class Q20TransactionalEventListenerPhasesExample {

    record OrderPlacedEvent(String orderId) {}

    // Transactional events decouple side-effects (e.g. sending emails / publishing to Kafka)
    // so they only fire AFTER the database transaction commits successfully.
    static class EmailNotificationService {
        public void handleOrderPlaced(OrderPlacedEvent event) {
            boolean executedAfterCommit = true; // true
        }
    }

    public static void main(String[] args) {
        EmailNotificationService service = new EmailNotificationService();
        service.handleOrderPlaced(new OrderPlacedEvent("ORD-999"));
        boolean listenerConfigured = true; // true
    }
}
