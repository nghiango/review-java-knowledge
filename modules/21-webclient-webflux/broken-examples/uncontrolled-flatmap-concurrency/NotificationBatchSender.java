package lab.webflux.broken.flatmapconcurrency;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NotificationBatchSender {

  private final WebClient webClient;

  public NotificationBatchSender(WebClient.Builder webClientBuilder) {
    this.webClient = webClientBuilder.baseUrl("https://push-notification.internal").build();
  }

  public Flux<NotificationResult> sendBatch(List<NotificationMessage> messages) {
    return Flux.fromIterable(messages)
        .flatMap(msg -> {
          return webClient.post()
              .uri("/api/notify")
              .bodyValue(msg)
              .retrieve()
              .bodyToMono(NotificationResult.class);
        });
  }

  public record NotificationMessage(String userId, String title, String body) {}
  public record NotificationResult(String userId, boolean delivered) {}
}
