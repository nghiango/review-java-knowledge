package lab.resilience.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class SafeCustomerRiskService {

    private final ExternalCreditBureauClient creditClient;
    private final CircuitBreaker circuitBreaker;

    public SafeCustomerRiskService(ExternalCreditBureauClient creditClient) {
        this(creditClient, createDefaultCircuitBreaker());
    }

    public SafeCustomerRiskService(
            ExternalCreditBureauClient creditClient, CircuitBreaker circuitBreaker) {
        this.creditClient = creditClient;
        this.circuitBreaker = circuitBreaker;
    }

    public static CircuitBreaker createDefaultCircuitBreaker() {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50.0f)
                        .waitDurationInOpenState(Duration.ofSeconds(1))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .recordExceptions(BureauUnavailableException.class)
                        .build();

        return CircuitBreaker.of("creditBureauCircuitBreaker", config);
    }

    public CustomerRiskProfile evaluateRisk(String customerId) {
        Supplier<CustomerRiskProfile> decorated =
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        () -> {
                            int score = creditClient.fetchCreditScore(customerId);
                            return new CustomerRiskProfile(customerId, score, "ACCURATE");
                        });

        try {
            return decorated.get();
        } catch (CallNotPermittedException ex) {
            // Fast fail when Circuit Breaker is OPEN
            return new CustomerRiskProfile(customerId, 600, "CIRCUIT_OPEN_FALLBACK");
        } catch (BureauUnavailableException ex) {
            // Fallback when remote call fails
            return new CustomerRiskProfile(customerId, 620, "DEGRADED_FALLBACK");
        }
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public interface ExternalCreditBureauClient {
        int fetchCreditScore(String customerId);
    }

    public static class BureauUnavailableException extends RuntimeException {
        public BureauUnavailableException(String message) {
            super(message);
        }
    }
}
