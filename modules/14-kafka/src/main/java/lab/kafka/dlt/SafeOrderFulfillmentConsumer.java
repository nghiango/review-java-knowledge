package lab.kafka.dlt;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Production-grade fulfillment consumer with bounded retries and Dead Letter Topic (DLT) routing.
 *
 * <p>Deterministic validation exceptions (e.g. {@link IllegalArgumentException}) are classified as
 * non-retryable and routed immediately to the DLT. Transient failures retry with bounded backoff.
 */
@Component
public class SafeOrderFulfillmentConsumer {

    public static final String TOPIC = "order-fulfillments";
    public static final String DLT_TOPIC = "order-fulfillments.DLT";

    private final WarehouseDispatchService warehouseService;
    private final DeadLetterAuditService dltAuditService;

    public SafeOrderFulfillmentConsumer(
            WarehouseDispatchService warehouseService, DeadLetterAuditService dltAuditService) {
        this.warehouseService = warehouseService;
        this.dltAuditService = dltAuditService;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            exclude = {IllegalArgumentException.class})
    @KafkaListener(topics = TOPIC, groupId = "warehouse-fulfillment-group")
    public void processFulfillment(FulfillmentPayload payload) {
        if (payload.quantity() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid fulfillment quantity: " + payload.quantity());
        }
        if (payload.destinationZip() == null || payload.destinationZip().isBlank()) {
            throw new IllegalArgumentException(
                    "Missing destination postal code for order: " + payload.orderId());
        }

        warehouseService.dispatch(payload.orderId(), payload.sku(), payload.quantity());
    }

    @DltHandler
    public void handleDeadLetter(
            FulfillmentPayload payload,
            @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
            @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
        dltAuditService.recordDeadLetter(payload.orderId(), originalTopic, errorMessage);
    }

    @Configuration
    public static class KafkaConsumerConfig {

        @Bean
        public DefaultErrorHandler fulfillmentErrorHandler(
                KafkaOperations<Object, Object> kafkaOperations) {
            DeadLetterPublishingRecoverer recoverer =
                    new DeadLetterPublishingRecoverer(
                            kafkaOperations,
                            (cr, ex) -> new TopicPartition(cr.topic() + ".DLT", cr.partition()));

            // 2 retry attempts with 1-second backoff
            DefaultErrorHandler errorHandler =
                    new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));

            // Mark deterministic exceptions as non-retryable to route directly to DLT
            errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
            return errorHandler;
        }
    }

    public interface WarehouseDispatchService {
        void dispatch(String orderId, String sku, int quantity);
    }

    public interface DeadLetterAuditService {
        void recordDeadLetter(String orderId, String originalTopic, String reason);
    }
}
