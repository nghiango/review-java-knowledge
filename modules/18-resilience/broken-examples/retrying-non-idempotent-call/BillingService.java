package lab.resilience.broken.nonidempotent;

import java.time.Duration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class BillingService {

    private final RestClient restClient;

    public BillingService(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));
        this.restClient = restClientBuilder
                .requestFactory(factory)
                .baseUrl("https://billing.internal.company.com")
                .build();
    }

    public ChargeConfirmation chargeCustomer(String customerId, double amount, String currency) {
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return restClient.post()
                        .uri("/api/v1/charges")
                        .body(new ChargePayload(customerId, amount, currency))
                        .retrieve()
                        .body(ChargeConfirmation.class);
            } catch (ResourceAccessException ex) {
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Charge failed after " + maxAttempts + " attempts", ex);
                }
                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry backoff", ie);
                }
            }
        }
        throw new IllegalStateException("Unreachable code reached in chargeCustomer");
    }

    public record ChargePayload(String customerId, double amount, String currency) {}
    public record ChargeConfirmation(String chargeId, String status) {}
}
