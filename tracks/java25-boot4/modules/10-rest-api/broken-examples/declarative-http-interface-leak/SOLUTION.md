# Solution: Declarative HTTP Interface Proxy Configuration & Error Propagation

## Annotated Code

```java
package lab.java25boot4.restapi.broken.httpinterface;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Service
public class PaymentOrchestratorService {

    private final PaymentGatewayClient paymentGatewayClient;

    public PaymentOrchestratorService() {
        // Resilience issue: declarative HTTP interface client instantiated without explicit connect and read timeouts
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.payment-gateway.internal")
                .build();

        // Maintainability issue: manual HttpServiceProxyFactory boilerplate bypasses Spring Framework 7 declarative client registration
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        this.paymentGatewayClient = factory.createClient(PaymentGatewayClient.class);
    }

    public String processPayment(String orderId, long amountCents, String currency) {
        try {
            var request = new PaymentGatewayClient.PaymentChargeRequest(orderId, amountCents, currency);
            var response = paymentGatewayClient.charge(request);
            return response.chargeId();
        } catch (RuntimeException ex) {
            // Reliability issue: downstream RFC 9457 ProblemDetail response is discarded, masking remote root cause with generic RuntimeException
            throw new RuntimeException("Payment processing failed for order " + orderId);
        }
    }
}
```

---

## Issues Identified

### 1. Missing Connection and Read Timeouts on Declarative HTTP Client
- **Category:** Resilience
- **Track:** `java25-boot4`
- **Severity:** Critical
- **Description:** Instantiating `RestClient.builder()` without configuring socket connect and read timeouts means default infinite (or OS-level multiple minutes) timeouts apply. When downstream services hang or fail silently, virtual threads or platform threads remain blocked indefinitely, exhausting file descriptors and upstream request queues.
- **Remediation:** Configure explicit connect and read timeouts via `ClientHttpRequestFactory` (e.g., `JdkClientHttpRequestFactory` with `setReadTimeout(Duration.ofSeconds(2))` and `setConnectTimeout(Duration.ofSeconds(2))`).

### 2. Discarding Downstream RFC 9457 Problem Details
- **Category:** Reliability
- **Track:** `java25-boot4`
- **Severity:** High
- **Description:** Catching raw `RuntimeException` and re-throwing a generic exception completely discards the downstream service's RFC 9457 `ProblemDetail` (status code, title, detail, type, invalid fields, idempotency conflict details). The calling service and operations team receive zero actionable diagnostic information.
- **Remediation:** Implement custom `ResponseErrorHandler` or catch `RestClientResponseException` / `RestClientResponseCustomizer` to extract and propagate the remote `ProblemDetail` into structured domain exceptions.

### 3. Outdated Manual `HttpServiceProxyFactory` Boilerplate
- **Category:** Maintainability
- **Track:** `java25-boot4`
- **Severity:** Medium
- **Description:** Manually building `RestClientAdapter` and `HttpServiceProxyFactory` inside constructor beans bypasses modern Spring Framework 7 / Spring Boot 4 declarative HTTP interface registration mechanisms (`@ImportHttpExchange` or centralized bean factory configuration).
- **Remediation:** Centralize declarative client bean creation using framework autoconfiguration or dedicated `@Configuration` factories providing standardized observability, tracing, and timeout configurations.
