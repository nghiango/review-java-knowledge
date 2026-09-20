package lab.kafka.broken.infiniteretries;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

@Component
public class OrderFulfillmentConsumer {

    private final WarehouseDispatchService warehouseService;

    public OrderFulfillmentConsumer(WarehouseDispatchService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @KafkaListener(
            topics = "order-fulfillments",
            groupId = "warehouse-fulfillment-group",
            containerFactory = "infiniteRetryContainerFactory")
    public void processFulfillment(FulfillmentPayload payload) {
        if (payload.quantity() <= 0) {
            throw new IllegalArgumentException("Invalid fulfillment quantity: " + payload.quantity());
        }
        if (payload.destinationZip() == null || payload.destinationZip().isBlank()) {
            throw new IllegalArgumentException("Missing destination postal code for order: " + payload.orderId());
        }

        warehouseService.dispatch(payload.orderId(), payload.sku(), payload.quantity());
    }

    @Configuration
    public static class KafkaConsumerConfig {

        @Bean
        public ConcurrentKafkaListenerContainerFactory<String, FulfillmentPayload> infiniteRetryContainerFactory(
                ConsumerFactory<String, FulfillmentPayload> consumerFactory) {
            ConcurrentKafkaListenerContainerFactory<String, FulfillmentPayload> factory =
                    new ConcurrentKafkaListenerContainerFactory<>();
            factory.setConsumerFactory(consumerFactory);

            // Error handler configured with infinite retries on any exception and no dead-letter topic
            DefaultErrorHandler errorHandler =
                    new DefaultErrorHandler(new FixedBackOff(500L, FixedBackOff.UNLIMITED_ATTEMPTS));
            factory.setCommonErrorHandler(errorHandler);

            return factory;
        }
    }

    public interface WarehouseDispatchService {
        void dispatch(String orderId, String sku, int quantity);
    }
}
