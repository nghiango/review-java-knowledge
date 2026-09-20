package lab.webflux.broken.missingtimeout;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class CarrierTrackingClient {

  private final WebClient webClient;

  public CarrierTrackingClient(WebClient.Builder webClientBuilder) {
    // Default WebClient builder without HttpClient timeout customization
    this.webClient = webClientBuilder.baseUrl("https://carrier.shipping.external").build();
  }

  public Mono<TrackingInfo> trackShipment(String trackingNumber) {
    // No timeout operator attached to reactive Mono
    return webClient.get()
        .uri("/shipments/{id}", trackingNumber)
        .retrieve()
        .bodyToMono(TrackingInfo.class);
  }

  public record TrackingInfo(String trackingNumber, String status, String eta) {}
}
