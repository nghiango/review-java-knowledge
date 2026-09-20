package lab.distributeddata.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SagaCoordinatorTest {

    @Test
    @DisplayName("Should successfully execute all saga steps when none fail")
    void executeSaga_allStepsSucceed() {
        SagaStep step1 = mock(SagaStep.class);
        when(step1.getName()).thenReturn("Step1");
        when(step1.execute()).thenReturn(true);

        SagaStep step2 = mock(SagaStep.class);
        when(step2.getName()).thenReturn("Step2");
        when(step2.execute()).thenReturn(true);

        SagaCoordinator coordinator = new SagaCoordinator();
        SagaCoordinator.SagaExecutionResult result = coordinator.executeSaga(List.of(step1, step2));

        assertThat(result.status()).isEqualTo(SagaCoordinator.SagaStatus.SUCCESS);
        assertThat(result.failedStep()).isNull();
        verify(step1, never()).compensate();
        verify(step2, never()).compensate();
    }

    @Test
    @DisplayName("Should rollback completed steps in reverse order when intermediate step fails")
    void executeSaga_compensatesOnFailure() {
        SagaStep step1 = mock(SagaStep.class);
        when(step1.getName()).thenReturn("CreateOrder");
        when(step1.execute()).thenReturn(true);

        SagaStep step2 = mock(SagaStep.class);
        when(step2.getName()).thenReturn("ReserveCredit");
        when(step2.execute()).thenReturn(false); // Fails

        SagaStep step3 = mock(SagaStep.class);
        when(step3.getName()).thenReturn("AllocateInventory");

        SagaCoordinator coordinator = new SagaCoordinator();
        SagaCoordinator.SagaExecutionResult result =
                coordinator.executeSaga(List.of(step1, step2, step3));

        assertThat(result.status()).isEqualTo(SagaCoordinator.SagaStatus.COMPENSATED);
        assertThat(result.failedStep()).isEqualTo("ReserveCredit");
        assertThat(result.executedCompensations()).containsExactly("CreateOrder");

        verify(step1, times(1)).compensate();
        verify(step3, never()).execute();
    }
}
