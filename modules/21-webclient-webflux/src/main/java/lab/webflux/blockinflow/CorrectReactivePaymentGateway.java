package lab.webflux.blockinflow;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CorrectReactivePaymentGateway {

    private final WebClient webClient;

    public CorrectReactivePaymentGateway(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Mono<PaymentResult> processPayment(PaymentRequest request) {
        return webClient
                .get()
                .uri("/api/tokens/{id}", request.tokenId())
                .retrieve()
                .bodyToMono(TokenValidationResponse.class)
                .flatMap(
                        tokenResponse -> {
                            if (!tokenResponse.valid()) {
                                return Mono.just(
                                        new PaymentResult(
                                                request.orderId(), "REJECTED", "Invalid token"));
                            }
                            return webClient
                                    .post()
                                    .uri("/api/charges")
                                    .bodyValue(
                                            new ChargePayload(
                                                    tokenResponse.accountId(), request.amount()))
                                    .retrieve()
                                    .bodyToMono(ChargeResponse.class)
                                    .map(
                                            resp ->
                                                    new PaymentResult(
                                                            request.orderId(),
                                                            "CONFIRMED",
                                                            resp.chargeId()));
                        })
                .defaultIfEmpty(
                        new PaymentResult(request.orderId(), "REJECTED", "Token not found"));
    }

    public record PaymentRequest(String orderId, String tokenId, double amount) {}

    public record TokenValidationResponse(String accountId, boolean valid) {}

    public record ChargePayload(String accountId, double amount) {}

    public record ChargeResponse(String chargeId, String status) {}

    public record PaymentResult(String orderId, String status, String details) {}
}
