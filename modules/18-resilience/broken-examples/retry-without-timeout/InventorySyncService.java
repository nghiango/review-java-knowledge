package lab.resilience.broken.notimeout;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class InventorySyncService {

    private final RestClient restClient;

    public InventorySyncService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://warehouse.internal.company.com")
                .build();
    }

    public InventoryStatus checkStock(String sku) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return restClient.get()
                        .uri("/api/v1/inventory/{sku}", sku)
                        .retrieve()
                        .body(InventoryStatus.class);
            } catch (Exception ex) {
                if (attempt == maxRetries) {
                    throw new RuntimeException("Failed to sync inventory after " + maxRetries + " attempts", ex);
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry backoff", ie);
                }
            }
        }
        return new InventoryStatus(sku, 0, "UNAVAILABLE");
    }

    public record InventoryStatus(String sku, int availableQuantity, String status) {}
}
