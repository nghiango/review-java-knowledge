package lab.architecture.questions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Q11: Domain Events Pattern and Side-Effect Decoupling. Demonstrates aggregate recording domain
 * events to be published after state change.
 */
public class Q11DomainEventPublishingExample {

    public record OrderCancelledEvent(String orderId, String reason) {}

    public static class OrderAggregate {
        private final String orderId;
        private final List<Object> domainEvents = new ArrayList<>();
        private boolean cancelled = false;

        public OrderAggregate(String orderId) {
            this.orderId = orderId;
        }

        public void cancel(String reason) {
            this.cancelled = true;
            this.domainEvents.add(new OrderCancelledEvent(this.orderId, reason));
        }

        public List<Object> pullDomainEvents() {
            List<Object> events = new ArrayList<>(this.domainEvents);
            this.domainEvents.clear();
            return Collections.unmodifiableList(events);
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    public static void main(String[] args) {
        OrderAggregate order = new OrderAggregate("ORD-55");
        order.cancel("User changed mind");

        List<Object> events = order.pullDomainEvents();
        int eventCount = events.size(); // 1
        boolean pulledClearsList =
                order.pullDomainEvents().isEmpty(); // true (events dispatched once)

        System.out.println("Q11 eventCount: " + eventCount + ", cleared: " + pulledClearsList);
    }
}
