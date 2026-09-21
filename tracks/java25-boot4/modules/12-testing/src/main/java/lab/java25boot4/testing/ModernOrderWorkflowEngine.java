package lab.java25boot4.testing;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Subtask;
import java.util.concurrent.TimeoutException;

/**
 * Modern workflow coordinator leveraging Java 25 Structured Concurrency (StructuredTaskScope) to
 * orchestrate concurrent subtasks on virtual threads deterministically without thread leakages.
 */
public class ModernOrderWorkflowEngine {

    public record OrderItem(String sku, int quantity, long unitPriceCents) {}

    public record OrderWorkflowResult(
            String orderId,
            boolean inventoryReserved,
            boolean fraudPassed,
            boolean notificationQueued,
            List<String> logs) {}

    public interface InventoryClient {
        boolean reserve(String orderId, List<OrderItem> items);
    }

    public interface FraudCheckClient {
        boolean evaluate(String orderId, long totalAmountCents);
    }

    public interface AuditNotifier {
        void recordAudit(String event);
    }

    private final InventoryClient inventoryClient;
    private final FraudCheckClient fraudCheckClient;
    private final AuditNotifier auditNotifier;
    private final ConcurrentLinkedQueue<String> recordedEvents = new ConcurrentLinkedQueue<>();

    public ModernOrderWorkflowEngine(
            InventoryClient inventoryClient,
            FraudCheckClient fraudCheckClient,
            AuditNotifier auditNotifier) {
        this.inventoryClient = inventoryClient;
        this.fraudCheckClient = fraudCheckClient;
        this.auditNotifier = auditNotifier;
    }

    public OrderWorkflowResult executeOrderWorkflow(
            String orderId, List<OrderItem> items, Duration timeout)
            throws InterruptedException, TimeoutException, ExecutionException {

        long totalAmount = items.stream().mapToLong(i -> i.unitPriceCents() * i.quantity()).sum();

        try (var scope =
                StructuredTaskScope.open(
                        StructuredTaskScope.Joiner.<Boolean>awaitAllSuccessfulOrThrow())) {
            Subtask<Boolean> inventorySubtask =
                    scope.fork(
                            () -> {
                                recordedEvents.add("inventory_started:" + orderId);
                                boolean reserved = inventoryClient.reserve(orderId, items);
                                auditNotifier.recordAudit(
                                        "inventory_reserved:" + orderId + ":" + reserved);
                                return reserved;
                            });

            Subtask<Boolean> fraudSubtask =
                    scope.fork(
                            () -> {
                                recordedEvents.add("fraud_started:" + orderId);
                                boolean passed = fraudCheckClient.evaluate(orderId, totalAmount);
                                auditNotifier.recordAudit(
                                        "fraud_checked:" + orderId + ":" + passed);
                                return passed;
                            });

            scope.join();

            boolean invResult = inventorySubtask.get();
            boolean fraudResult = fraudSubtask.get();

            recordedEvents.add("workflow_completed:" + orderId);
            return new OrderWorkflowResult(
                    orderId, invResult, fraudResult, true, List.copyOf(recordedEvents));
        }
    }

    public int getRecordedEventsCount() {
        return recordedEvents.size();
    }

    public List<String> getRecordedEvents() {
        return List.copyOf(recordedEvents);
    }
}
