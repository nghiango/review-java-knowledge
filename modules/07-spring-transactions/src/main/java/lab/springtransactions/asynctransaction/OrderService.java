package lab.springtransactions.asynctransaction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Double> orderStore = new ConcurrentHashMap<>();

    public OrderService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void createOrder(String orderId, String customerEmail, double amount) {
        orderStore.put(orderId, amount);

        if (amount > 10000) {
            orderStore.remove(orderId);
            throw new IllegalArgumentException("Amount exceeds credit limit - rolling back");
        }

        // Publishing event within transaction boundary; TransactionalEventListener defers
        // notification until commit
        eventPublisher.publishEvent(new OrderCreatedEvent(orderId, customerEmail, amount));
    }

    public Double getOrderAmount(String orderId) {
        return orderStore.get(orderId);
    }
}
