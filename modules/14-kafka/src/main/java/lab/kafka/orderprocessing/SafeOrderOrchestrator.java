package lab.kafka.orderprocessing;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Production-grade combined order orchestrator.
 *
 * <p>Solves all architectural defects identified in {@code order-processing-v1}:
 *
 * <ol>
 *   <li>Dual-write safety: Dispatches to Kafka strictly AFTER the database transaction commits via
 *       {@link TransactionalEventListener} on {@link TransactionPhase#AFTER_COMMIT}.
 *   <li>Causal ordering: Always supplies {@code orderId} as the Kafka partition key.
 *   <li>Idempotent consumer: Atomic deduplication check prevents duplicate inventory reservation.
 *   <li>Resilience: Retries bounded with backoff and routes poison messages to {@code
 *       orders.v1.DLT}.
 * </ol>
 */
@Service
public class SafeOrderOrchestrator {

    public static final String TOPIC = "orders.v1";
    public static final String GROUP_ID = "order-fulfillment-group";

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderDeduplicationStore deduplicationStore;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final DeadLetterAuditService dltAuditService;

    public SafeOrderOrchestrator(
            OrderRepository orderRepository,
            InventoryClient inventoryClient,
            OrderDeduplicationStore deduplicationStore,
            ApplicationEventPublisher eventPublisher,
            KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate,
            DeadLetterAuditService dltAuditService) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.deduplicationStore = deduplicationStore;
        this.eventPublisher = eventPublisher;
        this.kafkaTemplate = kafkaTemplate;
        this.dltAuditService = dltAuditService;
    }

    @Transactional
    public OrderEntity placeOrder(String orderId, String customerId, BigDecimal amount) {
        OrderEntity order = new OrderEntity(orderId, customerId, amount, "PENDING");
        orderRepository.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(orderId, customerId, amount, Instant.now());
        // Emit internal Spring application event; do NOT send to Kafka directly inside the DB tx
        eventPublisher.publishEvent(event);

        orderRepository.updateStatus(orderId, "CONFIRMED");
        return order;
    }

    /**
     * Publishes to Kafka strictly AFTER the database transaction commits successfully. If the
     * database transaction rolls back, this method is never invoked.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlacedAfterCommit(OrderPlacedEvent event) {
        // Enforce orderId as partition key for causal ordering
        var unused =
                kafkaTemplate
                        .send(TOPIC, event.orderId(), event)
                        .whenComplete(
                                (result, ex) -> {
                                    if (ex != null) {
                                        dltAuditService.recordFailure(
                                                event.orderId(), TOPIC, ex.getMessage());
                                    }
                                });
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalArgumentException.class})
    @KafkaListener(topics = TOPIC, groupId = GROUP_ID)
    public boolean onOrderPlaced(OrderPlacedEvent event, Acknowledgment ack) {
        // Idempotency check: deduplicate on orderId
        if (!deduplicationStore.tryMarkFulfillmentStarted(event.orderId())) {
            if (ack != null) {
                ack.acknowledge();
            }
            return false;
        }

        inventoryClient.reserveStock(event.orderId());
        orderRepository.updateStatus(event.orderId(), "FULFILLED");

        if (ack != null) {
            ack.acknowledge();
        }
        return true;
    }

    @DltHandler
    public void handleFulfillmentFailure(
            OrderPlacedEvent event,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String reason) {
        orderRepository.updateStatus(event.orderId(), "FAILED");
        dltAuditService.recordFailure(event.orderId(), originalTopic, reason);
    }

    public interface OrderRepository {
        void save(OrderEntity order);

        void updateStatus(String orderId, String status);

        OrderEntity findById(String orderId);
    }

    public interface InventoryClient {
        void reserveStock(String orderId);
    }

    public interface OrderDeduplicationStore {
        boolean tryMarkFulfillmentStarted(String orderId);
    }

    public interface DeadLetterAuditService {
        void recordFailure(String orderId, String topic, String reason);
    }
}
