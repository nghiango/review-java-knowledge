package lab.resilience.broken.swallowerror;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

@Service
public class CustomerRiskService {

    private final ExternalCreditBureauClient creditClient;

    public CustomerRiskService(ExternalCreditBureauClient creditClient) {
        this.creditClient = creditClient;
    }

    @CircuitBreaker(name = "creditBureau", fallbackMethod = "defaultRiskScore")
    public RiskAssessment evaluateRisk(String customerId) {
        try {
            int score = creditClient.fetchCreditScore(customerId);
            return new RiskAssessment(customerId, score, "ACCURATE");
        } catch (Exception ex) {
            System.err.println("Warning: Credit bureau check threw exception: " + ex.getMessage());
            return new RiskAssessment(customerId, 650, "FALLBACK_CAUGHT");
        }
    }

    public RiskAssessment defaultRiskScore(String customerId, Throwable t) {
        return new RiskAssessment(customerId, 600, "CIRCUIT_FALLBACK");
    }

    public record RiskAssessment(String customerId, int score, String tier) {}

    public interface ExternalCreditBureauClient {
        int fetchCreditScore(String customerId);
    }
}
