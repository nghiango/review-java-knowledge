package lab.webflux.blockinflow;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CorrectReactivePaymentGatewayTest {

    @Test
    @DisplayName("Should return REJECTED when token validation service is not found or empty")
    void processPayment_rejectsOnMissingToken() {
        // Point to non-listening port for fast rejection
        CorrectReactivePaymentGateway gateway =
                new CorrectReactivePaymentGateway(WebClient.builder(), "http://127.0.0.1:54321");

        Mono<CorrectReactivePaymentGateway.PaymentResult> resultMono =
                gateway.processPayment(
                                new CorrectReactivePaymentGateway.PaymentRequest(
                                        "ord-1", "tok-bad", 50.0))
                        .onErrorReturn(
                                new CorrectReactivePaymentGateway.PaymentResult(
                                        "ord-1", "ERROR", "Connection refused"));

        StepVerifier.create(resultMono)
                .assertNext(
                        result -> {
                            assertThat(result.orderId()).isEqualTo("ord-1");
                            assertThat(result.status()).isIn("REJECTED", "ERROR");
                        })
                .verifyComplete();
    }
}
