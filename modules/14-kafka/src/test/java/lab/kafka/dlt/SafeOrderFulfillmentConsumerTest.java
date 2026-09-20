package lab.kafka.dlt;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SafeOrderFulfillmentConsumerTest {

    @Mock private SafeOrderFulfillmentConsumer.WarehouseDispatchService warehouseService;

    @Mock private SafeOrderFulfillmentConsumer.DeadLetterAuditService dltAuditService;

    @InjectMocks private SafeOrderFulfillmentConsumer consumer;

    @Test
    @DisplayName("Valid fulfillment payload dispatches warehouse shipment successfully")
    void processFulfillment_validPayload_dispatchesSuccessfully() {
        FulfillmentPayload payload = new FulfillmentPayload("ord-1", "SKU-ABC", 5, "94105");

        assertThatNoException().isThrownBy(() -> consumer.processFulfillment(payload));

        verify(warehouseService).dispatch("ord-1", "SKU-ABC", 5);
    }

    @Test
    @DisplayName(
            "Non-positive quantity throws IllegalArgumentException for non-retryable DLT routing")
    void processFulfillment_invalidQuantity_throwsIllegalArgumentException() {
        FulfillmentPayload payload = new FulfillmentPayload("ord-2", "SKU-ABC", 0, "94105");

        assertThatThrownBy(() -> consumer.processFulfillment(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid fulfillment quantity");
    }

    @Test
    @DisplayName("DLT handler records failed order to dead letter audit log")
    void handleDeadLetter_recordsAudit() {
        FulfillmentPayload payload = new FulfillmentPayload("ord-3", "SKU-BAD", 1, "94105");

        consumer.handleDeadLetter(payload, "order-fulfillments", "Invalid SKU format");

        verify(dltAuditService)
                .recordDeadLetter("ord-3", "order-fulfillments", "Invalid SKU format");
    }
}
