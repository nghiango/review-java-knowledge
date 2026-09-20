package lab.resilience.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SafeCustomerRiskServiceTest {

    @Test
    @DisplayName("Should return accurate risk score when bureau is healthy")
    void evaluateRisk_healthy() {
        SafeCustomerRiskService.ExternalCreditBureauClient client =
                mock(SafeCustomerRiskService.ExternalCreditBureauClient.class);
        when(client.fetchCreditScore("cust-100")).thenReturn(750);

        SafeCustomerRiskService service = new SafeCustomerRiskService(client);
        CustomerRiskProfile profile = service.evaluateRisk("cust-100");

        assertThat(profile.creditScore()).isEqualTo(750);
        assertThat(profile.tier()).isEqualTo("ACCURATE");
        assertThat(service.getCircuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName(
            "Should trip circuit breaker to OPEN when failure threshold is exceeded and fast-fail subsequent calls")
    void evaluateRisk_tripsCircuitBreaker() {
        SafeCustomerRiskService.ExternalCreditBureauClient client =
                mock(SafeCustomerRiskService.ExternalCreditBureauClient.class);
        when(client.fetchCreditScore("cust-failing"))
                .thenThrow(
                        new SafeCustomerRiskService.BureauUnavailableException(
                                "Credit bureau connection refused"));

        SafeCustomerRiskService service = new SafeCustomerRiskService(client);

        // Send 5 failing calls to exceed minimumNumberOfCalls (5) with 100% failure rate (> 50%
        // threshold)
        for (int i = 0; i < 5; i++) {
            CustomerRiskProfile profile = service.evaluateRisk("cust-failing");
            assertThat(profile.tier()).isEqualTo("DEGRADED_FALLBACK");
        }

        // CircuitBreaker must now be in OPEN state
        assertThat(service.getCircuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Next call must be blocked immediately by circuit breaker without invoking downstream
        // client
        CustomerRiskProfile fastFailProfile = service.evaluateRisk("cust-failing");
        assertThat(fastFailProfile.tier()).isEqualTo("CIRCUIT_OPEN_FALLBACK");

        // Downstream client was called exactly 5 times, never on the 6th call
        verify(client, times(5)).fetchCreditScore("cust-failing");
    }
}
