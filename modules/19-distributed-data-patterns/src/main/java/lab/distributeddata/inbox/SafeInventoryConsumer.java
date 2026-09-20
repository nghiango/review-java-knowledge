package lab.distributeddata.inbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafeInventoryConsumer {

    private final InboxRepository inboxRepository;
    private final InventoryRepository inventoryRepository;

    public SafeInventoryConsumer(
            InboxRepository inboxRepository, InventoryRepository inventoryRepository) {
        this.inboxRepository = inboxRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public boolean processOrderPlaced(String messageId, String sku, int quantity) {
        // Atomic inbox lease acquisition: true if first time, false if duplicate redelivery
        boolean isFirstDelivery =
                inboxRepository.tryAcquireLease(messageId, "inventory-consumer-group");
        if (!isFirstDelivery) {
            // Idempotent no-op: message already processed, acknowledge without mutating business
            // state
            return false;
        }

        inventoryRepository.decrementStock(sku, quantity);
        return true;
    }

    public interface InventoryRepository {
        void decrementStock(String sku, int quantity);

        int getAvailableStock(String sku);
    }
}
