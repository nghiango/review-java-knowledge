package lab.architecture.questions;

import java.util.HashMap;
import java.util.Map;

/**
 * Q09: Hexagonal Architecture Ports & Adapters Implementation.
 * Demonstrates the separation between primary (driving) port, secondary (driven) port, and adapters.
 */
public class Q09PortsAndAdaptersExample {

    // Secondary (Driven) Port: Outbound dependency required by domain
    public interface NotificationPort {
        void sendAlert(String message);
    }

    // Secondary Adapter: Implements the outbound port
    public static class ConsoleNotificationAdapter implements NotificationPort {
        private String lastAlert;

        @Override
        public void sendAlert(String message) {
            this.lastAlert = message;
        }

        public String getLastAlert() {
            return lastAlert;
        }
    }

    // Core Domain Use Case
    public static class InventoryAlertUseCase {
        private final NotificationPort notificationPort;

        public InventoryAlertUseCase(NotificationPort notificationPort) {
            this.notificationPort = notificationPort;
        }

        public void checkThreshold(int stock) {
            if (stock < 5) {
                notificationPort.sendAlert("Low stock alert: " + stock);
            }
        }
    }

    public static void main(String[] args) {
        ConsoleNotificationAdapter adapter = new ConsoleNotificationAdapter();
        InventoryAlertUseCase useCase = new InventoryAlertUseCase(adapter);

        useCase.checkThreshold(2);
        boolean alertSent = "Low stock alert: 2".equals(adapter.getLastAlert()); // true

        System.out.println("Q09 alertSent: " + alertSent);
    }
}
