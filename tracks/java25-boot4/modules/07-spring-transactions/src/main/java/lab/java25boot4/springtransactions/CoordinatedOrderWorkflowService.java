package lab.java25boot4.springtransactions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;
import org.springframework.stereotype.Service;

/**
 * Demonstrates proper transaction boundary orchestration with virtual threads. Database mutations
 * execute in short, sequential, atomic transactions. Concurrent fan-out calls to remote services
 * execute in StructuredTaskScope outside DB transactions.
 */
@Service
public class CoordinatedOrderWorkflowService {

    public record OrderRecord(String id, String status, int amountCents) {}

    private final ConcurrentHashMap<String, OrderRecord> orderStore = new ConcurrentHashMap<>();

    // Short atomic transaction step 1: Record initial pending state
    public OrderRecord createPendingOrder(String id, int amountCents) {
        OrderRecord record = new OrderRecord(id, "PENDING", amountCents);
        orderStore.put(id, record);
        return record;
    }

    // Short atomic transaction step 2: Mark final status
    public OrderRecord finalizeOrder(String id, String finalStatus) {
        OrderRecord existing = orderStore.get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Order not found: " + id);
        }
        OrderRecord updated = new OrderRecord(id, finalStatus, existing.amountCents());
        orderStore.put(id, updated);
        return updated;
    }

    // Orchestrator: Non-transactional boundary coordinating parallel external verification
    public OrderRecord processOrderWithExternalVerification(
            String orderId, int amountCents, boolean failRemote) throws Throwable {
        // Step 1: Atomic DB insert
        createPendingOrder(orderId, amountCents);

        // Step 2: Concurrent verification across external systems (inventory, fraud) outside DB
        // locks
        try (var scope = StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow())) {
            Subtask<Boolean> inventorySubtask =
                    scope.fork(() -> verifyInventory(orderId, failRemote));
            Subtask<Boolean> fraudSubtask = scope.fork(() -> verifyFraud(orderId));

            scope.join();

            // Step 3: Atomic status update
            return finalizeOrder(orderId, "CONFIRMED");
        } catch (Throwable e) {
            finalizeOrder(orderId, "CANCELLED");
            throw e;
        }
    }

    private boolean verifyInventory(String orderId, boolean fail) {
        if (fail) {
            throw new IllegalStateException("Inventory allocation failed for " + orderId);
        }
        return true;
    }

    private boolean verifyFraud(String orderId) {
        return true;
    }

    public OrderRecord getOrder(String id) {
        return orderStore.get(id);
    }
}
