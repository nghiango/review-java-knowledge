# Solution — Blocking in Request Flow

## Annotated code

```java
package lab.webflux.broken.blockinflow;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ReactivePaymentGateway {

  private final WebClient webClient;

  public ReactivePaymentGateway(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl("https://bank-api.internal").build();
  }

  public Mono<PaymentResult> processPayment(PaymentRequest request) {
    // Concurrency issue: Calling .block() inside a reactive method halts the calling Netty event loop thread.
    // In Spring WebFlux, only a small number of event loops (1 per CPU core) handle all concurrent requests.
    // When downstream token verification encounters latency or packet loss, the event loop thread is frozen,
    // starving all other concurrent HTTP connections multiplexed onto that same core.
    // Performance issue: Synchronous blocking converts non-blocking pipeline into thread-per-request latency trap.
    // Design issue: Breaking the reactive chain prevents backpressure propagation, cancellation, and context flow.
    TokenValidationResponse tokenResponse =
        webClient.get()
            .uri("/api/tokens/{id}", request.tokenId())
            .retrieve()
            .bodyToMono(TokenValidationResponse.class)
            .block();

    if (tokenResponse == null || !tokenResponse.valid()) {
      return Mono.just(new PaymentResult(request.orderId(), "REJECTED", "Invalid token"));
    }

    // Execute charge against remote bank
    return webClient.post()
        .uri("/api/charges")
        .bodyValue(new ChargePayload(tokenResponse.accountId(), request.amount()))
        .retrieve()
        .bodyToMono(ChargeResponse.class)
        .map(resp -> new PaymentResult(request.orderId(), "CONFIRMED", resp.chargeId()));
  }

  public record PaymentRequest(String orderId, String tokenId, double amount) {}
  public record TokenValidationResponse(String accountId, boolean valid) {}
  public record ChargePayload(String accountId, double amount) {}
  public record ChargeResponse(String chargeId, String status) {}
  public record PaymentResult(String orderId, String status, String details) {}
}
```

## Issue list

### Concurrency issue: Invoking `.block()` inside WebFlux request flow freezes Netty event loops

- **Location:** `ReactivePaymentGateway.java:24`
- **Description:** `bodyToMono(...).block()` forces synchronous blocking execution on the calling thread.
- **Impact:** In WebFlux/Netty, there are typically only as many worker threads as CPU cores (e.g. 8 threads). When 8 concurrent requests hit this blocking call during a downstream delay, all Netty event loops are frozen. The server stops reading sockets, scheduling timeouts, or processing traffic for any route.
- **Remediation:** Compose operations using reactive operators (`.flatMap()`) to keep execution entirely non-blocking.

### Design issue: Breaking the reactive chain forfeits backpressure and cancellation

- **Location:** `ReactivePaymentGateway.java:24`
- **Description:** Splitting the reactive flow with `.block()` destroys Reactive Streams subscriber signaling.
- **Impact:** Client disconnects or upstream timeouts cannot cancel the downstream HTTP call, resulting in leaked downstream work and wasted resources.
- **Remediation:** Chain `Mono<TokenValidationResponse>` into `Mono<PaymentResult>` via `.flatMap()`.

## Correct implementation

See [`lab.webflux.blockinflow.CorrectReactivePaymentGateway`](../../src/main/java/lab/webflux/blockinflow/CorrectReactivePaymentGateway.java).

Detailed discussion in [Solutions](../../../docs/topics/webclient-webflux/solutions.md#non-blocking-request-flows-avoiding-block).
