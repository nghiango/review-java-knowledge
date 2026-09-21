package lab.java25boot4.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeterministicVirtualThreadWorkflowTest {

    @Mock private ModernOrderWorkflowEngine.InventoryClient inventoryClient;

    @Mock private ModernOrderWorkflowEngine.FraudCheckClient fraudCheckClient;

    @Mock private ModernOrderWorkflowEngine.AuditNotifier auditNotifier;

    @Test
    @DisplayName(
            "should execute subtasks concurrently and deterministically with StructuredConcurreny")
    void shouldExecuteSubtasksConcurrentlyAndDeterministically() throws Exception {
        ModernOrderWorkflowEngine engine =
                new ModernOrderWorkflowEngine(inventoryClient, fraudCheckClient, auditNotifier);

        when(inventoryClient.reserve(anyString(), anyList()))
                .thenAnswer(
                        invocation -> {
                            Thread.sleep(30);
                            return true;
                        });
        when(fraudCheckClient.evaluate(anyString(), anyLong()))
                .thenAnswer(
                        invocation -> {
                            Thread.sleep(20);
                            return true;
                        });

        var items = List.of(new ModernOrderWorkflowEngine.OrderItem("SKU-100", 2, 5000));
        var result = engine.executeOrderWorkflow("ord-123", items, Duration.ofSeconds(2));

        assertThat(result.orderId()).isEqualTo("ord-123");
        assertThat(result.inventoryReserved()).isTrue();
        assertThat(result.fraudPassed()).isTrue();
        assertThat(result.notificationQueued()).isTrue();

        // Verify using Awaitility for poll-based deterministic verification
        await().atMost(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> {
                            assertThat(engine.getRecordedEventsCount()).isGreaterThanOrEqualTo(3);
                        });

        verify(auditNotifier).recordAudit("inventory_reserved:ord-123:true");
        verify(auditNotifier).recordAudit("fraud_checked:ord-123:true");
    }

    @Test
    @DisplayName("should fail fast when subtask encounters exception in StructuredTaskScope")
    void shouldFailFastWhenSubtaskFails() {
        ModernOrderWorkflowEngine engine =
                new ModernOrderWorkflowEngine(inventoryClient, fraudCheckClient, auditNotifier);

        when(inventoryClient.reserve(anyString(), anyList()))
                .thenThrow(new IllegalStateException("Inventory service unavailable"));
        when(fraudCheckClient.evaluate(anyString(), anyLong())).thenReturn(true);

        var items = List.of(new ModernOrderWorkflowEngine.OrderItem("SKU-200", 1, 1000));

        assertThatThrownBy(
                        () -> engine.executeOrderWorkflow("ord-fail", items, Duration.ofSeconds(2)))
                .isInstanceOf(StructuredTaskScope.FailedException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Inventory service unavailable");
    }
}
