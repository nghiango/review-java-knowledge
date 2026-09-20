package lab.webflux.flatmapconcurrency;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class CorrectNotificationBatchSender {

    private static final int DEFAULT_MAX_CONCURRENCY = 16;
    private final WebClient webClient;

    public CorrectNotificationBatchSender(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public Flux<NotificationResult> sendBatch(List<NotificationMessage> messages) {
        return sendBatch(messages, DEFAULT_MAX_CONCURRENCY);
    }

    public Flux<NotificationResult> sendBatch(
            List<NotificationMessage> messages, int maxConcurrency) {
        // Explicit concurrency limiter (maxConcurrency) + per-item error isolation
        return Flux.fromIterable(messages)
                .flatMap(
                        msg ->
                                webClient
                                        .post()
                                        .uri("/api/notify")
                                        .bodyValue(msg)
                                        .retrieve()
                                        .bodyToMono(NotificationResult.class)
                                        .onErrorReturn(new NotificationResult(msg.userId(), false)),
                        maxConcurrency);
    }

    public record NotificationMessage(String userId, String title, String body) {}

    public record NotificationResult(String userId, boolean delivered) {}
}
