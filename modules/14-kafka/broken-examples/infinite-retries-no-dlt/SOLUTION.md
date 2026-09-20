# Solution — Infinite Retries Without Dead Letter Topic

## Annotated code

```java
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

            // Reliability issue: Unbounded retries with UNLIMITED_ATTEMPTS and no Dead Letter Topic.
            // When a deterministic business or deserialization failure occurs (poison pill),
            // this listener will loop forever every 500ms, permanently halting all progress on the partition.
            DefaultErrorHandler errorHandler =
                    new DefaultErrorHandler(new FixedBackOff(500L, FixedBackOff.UNLIMITED_ATTEMPTS));
            // Reliability issue: No DeadLetterPublishingRecoverer or classification of fatal vs retryable exceptions.
            factory.setCommonErrorHandler(errorHandler);

            return factory;
        }
    }

    public interface WarehouseDispatchService {
        void dispatch(String orderId, String sku, int quantity);
    }
}
```

## Issue list

### Reliability issue: Infinite retry loop on poison pill blocks partition processing

- **Location:** `OrderFulfillmentConsumer.java:46-48`
- **Description:** `DefaultErrorHandler` is configured with `FixedBackOff.UNLIMITED_ATTEMPTS` and no `ConsumerRecordRecoverer`.
- **Impact:** If an upstream producer sends a payload with negative quantity or missing zip code, this deterministic `IllegalArgumentException` will fail on every attempt. Because the error handler never gives up and never moves the offset forward or forwards the record to a Dead Letter Topic (DLT), the consumer thread is stuck in an infinite retry crash loop. All valid orders queued behind this poison message in the same partition are starved indefinitely.
- **Remediation:** 
  1. Bound the retry attempts (e.g. `new FixedBackOff(1000L, 3)`).
  2. Configure a `DeadLetterPublishingRecoverer` with `KafkaTemplate` to publish exhausted records to `order-fulfillments.DLT`.
  3. Classify deterministic exceptions (`IllegalArgumentException`, `MethodArgumentNotValidException`, `DeserializationException`) as not retryable via `errorHandler.addNotRetryableExceptions(...)`.
