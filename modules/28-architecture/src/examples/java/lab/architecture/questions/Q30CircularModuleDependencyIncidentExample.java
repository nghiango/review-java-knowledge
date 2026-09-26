package lab.architecture.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates a production scenario where circular module dependencies in a monolithic codebase
 * caused un-testable tight coupling and deployment deadlock, resolved via architectural inversion and ArchUnit.
 */
public class Q30CircularModuleDependencyIncidentExample {

    public interface OrderNotificationPort {
        void notifyOrderPlaced(String orderId);
    }

    public static class OrderModule {
        private final OrderNotificationPort notificationPort;

        public OrderModule(OrderNotificationPort notificationPort) {
            this.notificationPort = notificationPort;
        }

        public void placeOrder(String orderId) {
            // Emits to port instead of directly depending on NotificationModule implementation
            notificationPort.notifyOrderPlaced(orderId);
        }
    }

    public static class NotificationModule implements OrderNotificationPort {
        private final List<String> notifications = new ArrayList<>();

        @Override
        public void notifyOrderPlaced(String orderId) {
            notifications.add("Notified: " + orderId);
        }

        public boolean hasNotification(String orderId) {
            return notifications.contains("Notified: " + orderId);
        }
    }

    public static void main(String[] args) {
        NotificationModule notificationModule = new NotificationModule();
        OrderModule orderModule = new OrderModule(notificationModule);

        orderModule.placeOrder("ORD-999");

        boolean dependencyInverted = notificationModule.hasNotification("ORD-999"); // true
        System.out.println("Circular dependency decoupled via port interface: " + dependencyInverted);
    }
}
