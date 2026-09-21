package lab.architecture.modularmonolith.billing;

import java.math.BigDecimal;
import java.util.Objects;
import lab.architecture.modularmonolith.inventory.InventoryApi;
import lab.architecture.modularmonolith.ordering.OrderPlacedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Billing bounded context service. Interacts with Inventory via public InventoryApi (not raw
 * database tables) and with Ordering via decoupled Domain Events.
 */
@Service
public class BillingService {

    private final InventoryApi inventoryApi;
    private final ApplicationEventPublisher eventPublisher;

    public BillingService(InventoryApi inventoryApi, ApplicationEventPublisher eventPublisher) {
        this.inventoryApi = Objects.requireNonNull(inventoryApi, "InventoryApi must not be null");
        this.eventPublisher =
                Objects.requireNonNull(
                        eventPublisher, "ApplicationEventPublisher must not be null");
    }

    @EventListener
    public void onOrderPlaced(OrderPlacedEvent event) {
        // Safe cross-module communication using public contract
        boolean reserved = inventoryApi.isAvailable("PROD-1", 1);
        boolean success = reserved && event.amount().compareTo(BigDecimal.ZERO) > 0;

        eventPublisher.publishEvent(new OrderBilledEvent(event.orderId(), event.amount(), success));
    }
}
