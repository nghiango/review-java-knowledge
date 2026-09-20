package lab.resilience.timeout;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SafeInventorySyncService implements AutoCloseable {

    private final RestClient restClient;
    private final TimeLimiter timeLimiter;
    private final ScheduledExecutorService scheduler;

    public SafeInventorySyncService(RestClient.Builder restClientBuilder, String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(1));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        this.restClient = restClientBuilder.requestFactory(requestFactory).baseUrl(baseUrl).build();

        this.timeLimiter =
                TimeLimiter.of(
                        "inventorySyncTimeLimiter",
                        TimeLimiterConfig.custom()
                                .timeoutDuration(Duration.ofSeconds(2))
                                .cancelRunningFuture(true)
                                .build());

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public InventoryStatus checkStock(String sku) {
        try {
            return timeLimiter
                    .executeCompletionStage(
                            scheduler,
                            () ->
                                    CompletableFuture.supplyAsync(
                                            () ->
                                                    restClient
                                                            .get()
                                                            .uri("/api/v1/inventory/{sku}", sku)
                                                            .retrieve()
                                                            .body(InventoryStatus.class)))
                    .toCompletableFuture()
                    .join();
        } catch (Exception ex) {
            return new InventoryStatus(sku, 0, "UNAVAILABLE");
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
    }

    public record InventoryStatus(String sku, int availableQuantity, String status) {}
}
