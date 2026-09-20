package lab.springcore.circular;

import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    private final ApplicationEventPublisher eventPublisher;

    public BillingService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher =
                Objects.requireNonNull(eventPublisher, "eventPublisher must not be null");
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // Process charging logic
        boolean success = event.amount() > 0;
        eventPublisher.publishEvent(new InvoiceProcessedEvent(event.orderId(), success));
    }
}
