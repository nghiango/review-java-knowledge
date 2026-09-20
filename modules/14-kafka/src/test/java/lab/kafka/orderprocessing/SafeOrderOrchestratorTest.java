package lab.kafka.orderprocessing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

@ExtendWith(MockitoExtension.class)
class SafeOrderOrchestratorTest {

    @Mock private SafeOrderOrchestrator.OrderRepository orderRepository;

    @Mock private SafeOrderOrchestrator.InventoryClient inventoryClient;

    @Mock private SafeOrderOrchestrator.OrderDeduplicationStore deduplicationStore;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Mock private SafeOrderOrchestrator.DeadLetterAuditService dltAuditService;

    @Mock private Acknowledgment acknowledgment;

    @InjectMocks private SafeOrderOrchestrator orchestrator;

    @Test
    @DisplayName(
            "placeOrder saves order in DB and publishes internal event without direct Kafka send")
    void placeOrder_savesAndEmitsInternalEvent() {
        OrderEntity result = orchestrator.placeOrder("ord-777", "cust-1", new BigDecimal("199.99"));

        assertThat(result.orderId()).isEqualTo("ord-777");
        verify(orderRepository).save(any(OrderEntity.class));
        verify(eventPublisher).publishEvent(any(OrderPlacedEvent.class));
        verify(orderRepository).updateStatus("ord-777", "CONFIRMED");
        // Crucial: verify KafkaTemplate was NOT called inside placeOrder
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("onOrderPlacedAfterCommit publishes to Kafka keyed by orderId")
    void onOrderPlacedAfterCommit_publishesWithOrderIdKey() {
        OrderPlacedEvent event =
                new OrderPlacedEvent("ord-777", "cust-1", new BigDecimal("199.99"), Instant.now());

        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        orchestrator.onOrderPlacedAfterCommit(event);

        ArgumentCaptor<OrderPlacedEvent> eventCaptor =
                ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate)
                .send(eq(SafeOrderOrchestrator.TOPIC), eq("ord-777"), eventCaptor.capture());

        assertThat(eventCaptor.getValue().orderId()).isEqualTo("ord-777");
    }

    @Test
    @DisplayName("handleFulfillmentFailure updates order status to FAILED and records audit")
    void handleFulfillmentFailure_updatesStatusAndRecordsAudit() {
        OrderPlacedEvent event =
                new OrderPlacedEvent("ord-999", "cust-3", new BigDecimal("99.99"), Instant.now());

        orchestrator.handleFulfillmentFailure(event, "orders.v1", "Out of stock");

        verify(orderRepository).updateStatus("ord-999", "FAILED");
        verify(dltAuditService).recordFailure("ord-999", "orders.v1", "Out of stock");
    }

    @Test
    @DisplayName("Consumer deduplicates on orderId and avoids double reservation on redelivery")
    void onOrderPlaced_duplicateRedelivery_skipsReservation() {
        OrderPlacedEvent event =
                new OrderPlacedEvent("ord-777", "cust-1", new BigDecimal("199.99"), Instant.now());

        when(deduplicationStore.tryMarkFulfillmentStarted("ord-777")).thenReturn(false);

        boolean processed = orchestrator.onOrderPlaced(event, acknowledgment);

        assertThat(processed).isFalse();
        verify(inventoryClient, never()).reserveStock(any());
        verify(orderRepository, never()).updateStatus(any(), eq("FULFILLED"));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Consumer fulfills new order and updates status when deduplication succeeds")
    void onOrderPlaced_newOrder_fulfillsSuccessfully() {
        OrderPlacedEvent event =
                new OrderPlacedEvent("ord-888", "cust-2", new BigDecimal("49.99"), Instant.now());

        when(deduplicationStore.tryMarkFulfillmentStarted("ord-888")).thenReturn(true);

        boolean processed = orchestrator.onOrderPlaced(event, acknowledgment);

        assertThat(processed).isTrue();
        verify(inventoryClient).reserveStock("ord-888");
        verify(orderRepository).updateStatus("ord-888", "FULFILLED");
        verify(acknowledgment).acknowledge();
    }
}
