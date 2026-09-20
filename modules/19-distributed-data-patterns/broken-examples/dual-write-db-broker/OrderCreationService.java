package lab.distributeddata.broken.dualwrite;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCreationService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderCreationService(OrderRepository orderRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public OrderRecord createOrder(String orderId, String customerId, double totalAmount) {
        OrderRecord order = new OrderRecord(orderId, customerId, totalAmount, "CREATED");
        orderRepository.save(order);

        // Publish event directly to Kafka broker inside database transaction
        kafkaTemplate.send("orders.events", orderId, "{\"orderId\":\"" + orderId + "\",\"status\":\"CREATED\"}");

        return order;
    }

    public record OrderRecord(String orderId, String customerId, double totalAmount, String status) {}

    public interface OrderRepository {
        void save(OrderRecord order);
    }
}
