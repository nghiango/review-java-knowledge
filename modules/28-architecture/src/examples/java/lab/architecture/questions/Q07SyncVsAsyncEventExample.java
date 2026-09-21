package lab.architecture.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Q07: Synchronous RPC vs Asynchronous Event-Driven Messaging.
 * Demonstrates how event publishing decouples the caller from downstream side effects.
 */
public class Q07SyncVsAsyncEventExample {

    public record UserRegisteredEvent(String userId, String email) {}

    public static class EventBus {
        private final List<Object> eventQueue = new ArrayList<>();

        public void publish(Object event) {
            eventQueue.add(event);
        }

        public int queuedCount() {
            return eventQueue.size();
        }
    }

    public static void main(String[] args) {
        EventBus bus = new EventBus();

        // Instead of synchronously invoking EmailService and AnalyticsService:
        bus.publish(new UserRegisteredEvent("U-1", "user@example.com"));

        int pendingEvents = bus.queuedCount(); // 1 (producer decoupled from consumer execution)
        boolean hasPending = pendingEvents > 0; // true

        System.out.println("Q07 pending: " + pendingEvents + ", hasPending: " + hasPending);
    }
}
