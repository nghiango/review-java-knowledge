package lab.springcore.circular;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, String> orderStatuses = new ConcurrentHashMap<>();

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher =
                Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    public void createOrder(String orderId, double amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        orderStatuses.put(orderId, "PENDING");
        eventPublisher.publishEvent(new OrderCreatedEvent(orderId, amount));
    }

    @EventListener
    public void onInvoiceProcessed(InvoiceProcessedEvent event) {
        if (event.success()) {
            orderStatuses.put(event.orderId(), "PAID");
        } else {
            orderStatuses.put(event.orderId(), "PAYMENT_FAILED");
        }
    }

    public String getOrderStatus(String orderId) {
        return orderStatuses.get(orderId);
    }
}
