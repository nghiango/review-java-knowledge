package lab.designpatterns.questions;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates the Null Object Pattern eliminating null checks across domain logic
 * by providing a neutral, default no-op behavior object implementing the common interface.
 */
public class Q25NullObjectPatternExample {

    public interface AuditLogger {
        void log(String message);
        boolean isEnabled();
    }

    public static class RealAuditLogger implements AuditLogger {
        private final List<String> logs = new ArrayList<>();

        @Override
        public void log(String message) {
            logs.add(message);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        public List<String> getLogs() {
            return logs;
        }
    }

    public static class NoOpAuditLogger implements AuditLogger {
        @Override
        public void log(String message) {
            // Intentionally no-op
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    public static class OrderService {
        private final AuditLogger logger;

        public OrderService(AuditLogger logger) {
            // Null object default eliminates null checks in processOrder
            this.logger = (logger != null) ? logger : new NoOpAuditLogger();
        }

        public void processOrder(String orderId) {
            logger.log("Processed " + orderId);
        }

        public boolean isLoggingActive() {
            return logger.isEnabled();
        }
    }

    public static void main(String[] args) {
        OrderService serviceWithNull = new OrderService(null);
        serviceWithNull.processOrder("ORD-100"); // No NullPointerException thrown

        boolean safeWithoutNullCheck = !serviceWithNull.isLoggingActive(); // true
        System.out.println("Null Object pattern safely absorbed call without NPE: " + safeWithoutNullCheck);
    }
}
