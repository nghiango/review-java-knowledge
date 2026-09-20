package lab.kafka.broken.orderprocessing;

import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsecureOrderProcessor {

    public static final String TOPIC = "orders.v1";

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public InsecureOrderProcessor(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public OrderEntity placeOrder(String orderId, String customerId, BigDecimal amount) {
        OrderEntity order = new OrderEntity(orderId, customerId, amount, "PENDING");
        orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, customerId, amount, java.time.Instant.now());
        kafkaTemplate.send(TOPIC, event);

        orderRepository.updateStatus(orderId, "CONFIRMED");
        return order;
    }

    @KafkaListener(topics = TOPIC, groupId = "order-fulfillment-group")
    public void onOrderPlaced(OrderPlacedEvent event) {
        try {
            inventoryClient.reserveStock(event.orderId());
            orderRepository.updateStatus(event.orderId(), "FULFILLED");
        } catch (Exception e) {
            System.err.println("Fulfillment failed for order: " + event.orderId());
        }
    }

    public interface OrderRepository {
        void save(OrderEntity order);

        void updateStatus(String orderId, String status);
    }

    public interface InventoryClient {
        void reserveStock(String orderId);
    }
}
