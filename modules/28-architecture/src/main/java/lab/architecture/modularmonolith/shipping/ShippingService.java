package lab.architecture.modularmonolith.shipping;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lab.architecture.modularmonolith.billing.OrderBilledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Shipping bounded context service. Responds to billing events without direct database coupling to
 * Billing or Ordering.
 */
@Service
public class ShippingService {

    private final Map<String, String> shipments = new ConcurrentHashMap<>();

    @EventListener
    public void onOrderBilled(OrderBilledEvent event) {
        if (event.success()) {
            shipments.put(event.orderId(), "PREPARING_DISPATCH");
        } else {
            shipments.put(event.orderId(), "CANCELLED_PAYMENT_FAILURE");
        }
    }

    public String getShipmentStatus(String orderId) {
        return shipments.getOrDefault(orderId, "NOT_FOUND");
    }
}
