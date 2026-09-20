package lab.webflux.missingtimeout;

import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CorrectCarrierTrackingClient {

    private final WebClient webClient;
    private final Duration requestTimeout;

    public CorrectCarrierTrackingClient(
            WebClient.Builder webClientBuilder, String baseUrl, Duration requestTimeout) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.requestTimeout = requestTimeout;
    }

    public Mono<TrackingInfo> trackShipment(String trackingNumber) {
        // Explicit reactive timeout operator with graceful fallback
        return webClient
                .get()
                .uri("/shipments/{id}", trackingNumber)
                .retrieve()
                .bodyToMono(TrackingInfo.class)
                .timeout(requestTimeout)
                .onErrorReturn(
                        new TrackingInfo(trackingNumber, "UNAVAILABLE", "ETA not available"));
    }

    public record TrackingInfo(String trackingNumber, String status, String eta) {}
}
