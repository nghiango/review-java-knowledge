package lab.observability.swallowedexceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CorrectInventoryReconciliationServiceTest {

    private MeterRegistry meterRegistry;
    private CorrectInventoryReconciliationService.InventoryRepository repository;
    private CorrectInventoryReconciliationService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        repository = mock(CorrectInventoryReconciliationService.InventoryRepository.class);
        service = new CorrectInventoryReconciliationService(meterRegistry, repository);
    }

    @Test
    @DisplayName("Should increment success metric on successful reconciliation")
    void reconcileStock_recordsSuccessMetrics() {
        var result = service.reconcileStock("CAT-1234", 10);

        assertThat(result.success()).isTrue();
        assertThat(result.errorMessage()).isNull();

        Counter counter =
                meterRegistry.find("inventory.reconcile.total").tag("status", "SUCCESS").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName(
            "Should increment failure metric with exception tag and return structured error on failure")
    void reconcileStock_recordsFailureMetricsAndException() {
        doThrow(new IllegalStateException("Connection pool timeout"))
                .when(repository)
                .adjustStock(anyString(), anyInt());

        var result = service.reconcileStock("CAT-5678", -5);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Connection pool timeout");

        Counter failureCounter =
                meterRegistry
                        .find("inventory.reconcile.total")
                        .tag("status", "FAILED")
                        .tag("exception", "IllegalStateException")
                        .counter();
        assertThat(failureCounter).isNotNull();
        assertThat(failureCounter.count()).isEqualTo(1.0);
    }
}
