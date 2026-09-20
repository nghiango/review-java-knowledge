package lab.webflux.noerrorhandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CorrectCartPricingEngineTest {

    @Test
    @DisplayName(
            "Should successfully calculate cart price with 0 discounts when downstream services fail")
    void calculateCartPrice_handlesServiceFailureWithZeroDiscounts() {
        CorrectCartPricingEngine engine =
                new CorrectCartPricingEngine(
                        WebClient.builder(), "http://127.0.0.1:54321", "http://127.0.0.1:54322");

        Mono<CorrectCartPricingEngine.FinalCartPrice> priceMono =
                engine.calculateCartPrice("cust-1", 100.0);

        StepVerifier.create(priceMono)
                .assertNext(
                        price -> {
                            assertThat(price.userId()).isEqualTo("cust-1");
                            assertThat(price.originalTotal()).isEqualTo(100.0);
                            assertThat(price.discount()).isEqualTo(0.0);
                            assertThat(price.finalTotal()).isEqualTo(100.0);
                        })
                .verifyComplete();
    }
}
