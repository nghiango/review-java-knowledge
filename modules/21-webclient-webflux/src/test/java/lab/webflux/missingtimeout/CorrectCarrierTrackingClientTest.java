package lab.webflux.missingtimeout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CorrectCarrierTrackingClientTest {

    @Test
    @DisplayName("Should return fallback tracking info when downstream times out or fails")
    void trackShipment_fallsBackGracefully() {
        CorrectCarrierTrackingClient client =
                new CorrectCarrierTrackingClient(
                        WebClient.builder(), "http://127.0.0.1:54321", Duration.ofMillis(200));

        Mono<CorrectCarrierTrackingClient.TrackingInfo> trackingMono =
                client.trackShipment("TRK-12345");

        StepVerifier.create(trackingMono)
                .assertNext(
                        info -> {
                            assertThat(info.trackingNumber()).isEqualTo("TRK-12345");
                            assertThat(info.status()).isEqualTo("UNAVAILABLE");
                            assertThat(info.eta()).isEqualTo("ETA not available");
                        })
                .verifyComplete();
    }
}
