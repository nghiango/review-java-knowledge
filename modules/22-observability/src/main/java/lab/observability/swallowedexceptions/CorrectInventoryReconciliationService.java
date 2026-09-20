package lab.observability.swallowedexceptions;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CorrectInventoryReconciliationService {

    private static final Logger log =
            LoggerFactory.getLogger(CorrectInventoryReconciliationService.class);

    private final MeterRegistry meterRegistry;
    private final InventoryRepository inventoryRepository;

    public CorrectInventoryReconciliationService(
            MeterRegistry meterRegistry, InventoryRepository inventoryRepository) {
        this.meterRegistry = meterRegistry;
        this.inventoryRepository = inventoryRepository;
    }

    public ReconciliationResult reconcileStock(String sku, int delta) {
        try {
            inventoryRepository.adjustStock(sku, delta);
            // Record symmetric success metrics
            meterRegistry
                    .counter(
                            "inventory.reconcile.total",
                            "status",
                            "SUCCESS",
                            "sku",
                            sanitizeSku(sku))
                    .increment();
            log.info("Successfully reconciled inventory sku={} delta={}", sku, delta);
            return new ReconciliationResult(sku, delta, true, null);
        } catch (Exception e) {
            // Pass full Throwable to preserve causal stack trace and line numbers
            log.error("Failed to reconcile inventory stock for sku={} delta={}", sku, delta, e);

            // Record symmetric failure metric with error type tag for dashboard visibility
            meterRegistry
                    .counter(
                            "inventory.reconcile.total",
                            "status",
                            "FAILED",
                            "exception",
                            e.getClass().getSimpleName(),
                            "sku",
                            sanitizeSku(sku))
                    .increment();

            // Return structured result or propagate domain error instead of silently returning
            // false
            return new ReconciliationResult(sku, delta, false, e.getMessage());
        }
    }

    private String sanitizeSku(String sku) {
        return (sku != null && sku.startsWith("CAT-")) ? sku : "GENERAL";
    }

    public record ReconciliationResult(
            String sku, int delta, boolean success, String errorMessage) {}

    public interface InventoryRepository {
        void adjustStock(String sku, int delta);
    }
}
