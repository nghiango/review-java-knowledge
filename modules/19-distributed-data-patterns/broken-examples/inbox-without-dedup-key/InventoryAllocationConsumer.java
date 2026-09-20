package lab.distributeddata.broken.inbox;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryAllocationConsumer {

    private final InventoryRepository inventoryRepository;

    public InventoryAllocationConsumer(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @KafkaListener(topics = "orders.events", groupId = "inventory-allocation-group")
    @Transactional
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Directly decrement stock without checking if event.eventId() was already processed
        inventoryRepository.decrementStock(event.sku(), event.quantity());
    }

    public record OrderPlacedEvent(String eventId, String orderId, String sku, int quantity) {}

    public interface InventoryRepository {
        void decrementStock(String sku, int quantity);
    }
}
