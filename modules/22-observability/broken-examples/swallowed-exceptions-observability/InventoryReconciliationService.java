package lab.observability.broken.swallowedexceptions;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InventoryReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(InventoryReconciliationService.class);

    private final Counter successCounter;
    private final InventoryRepository inventoryRepository;

    public InventoryReconciliationService(MeterRegistry meterRegistry, InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
        this.successCounter = meterRegistry.counter("inventory.reconcile.success");
    }

    public boolean reconcileStock(String sku, int delta) {
        try {
            inventoryRepository.adjustStock(sku, delta);
            successCounter.increment();
            return true;
        } catch (Exception e) {
            // Swallowing stack trace by logging only message string without exception object
            log.error("Failed to reconcile stock for " + sku + ": " + e.getMessage());
            // Missing error metric increment - dashboards show 100% success rate!
            return false;
        }
    }

    public interface InventoryRepository {
        void adjustStock(String sku, int delta);
    }
}
