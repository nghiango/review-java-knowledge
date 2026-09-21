package lab.architecture.modularmonolith.ordering;

import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OrderingService {

    private final ApplicationEventPublisher eventPublisher;

    public OrderingService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "ApplicationEventPublisher must not be null");
    }

    public void placeOrder(String orderId, String customerId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Domain event published to decouple ordering from billing and shipping
        eventPublisher.publishEvent(new OrderPlacedEvent(orderId, customerId, amount));
    }
}
