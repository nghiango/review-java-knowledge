package lab.resilience.idempotentretry;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
public class SafeBillingService {

    private final RestClient restClient;
    private final Retry retry;

    public SafeBillingService(RestClient.Builder restClientBuilder, String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(1));
        factory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = restClientBuilder.requestFactory(factory).baseUrl(baseUrl).build();

        RetryConfig retryConfig =
                RetryConfig.custom()
                        .maxAttempts(3)
                        .intervalFunction(
                                IntervalFunction.ofExponentialRandomBackoff(
                                        Duration.ofMillis(50), 2.0, 0.5))
                        .retryExceptions(ResourceAccessException.class)
                        .build();

        this.retry = Retry.of("billingRetry", retryConfig);
    }

    public ChargeConfirmation chargeCustomer(ChargeRequest request) {
        String idempotencyKey = UUID.randomUUID().toString();

        return Retry.decorateSupplier(
                        retry,
                        () ->
                                restClient
                                        .post()
                                        .uri("/api/v1/charges")
                                        .header("Idempotency-Key", idempotencyKey)
                                        .body(request)
                                        .retrieve()
                                        .body(ChargeConfirmation.class))
                .get();
    }

    public record ChargeConfirmation(String chargeId, String status) {}
}
