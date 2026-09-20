package lab.testing.broken.mockingawaytheintegration;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

/** Looks up stock levels from the inventory service. */
public final class InventoryClient {

    private final RestClient restClient;
    private final String baseUrl;

    public InventoryClient(RestClient restClient, String baseUrl) {
        this.restClient = restClient;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> get(String sku) {
        return restClient
                .get()
                .uri(baseUrl + "/stock/{sku}", sku)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
