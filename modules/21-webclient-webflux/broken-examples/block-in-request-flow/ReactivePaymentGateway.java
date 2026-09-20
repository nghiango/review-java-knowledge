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
    // Check account validation via remote token endpoint
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
