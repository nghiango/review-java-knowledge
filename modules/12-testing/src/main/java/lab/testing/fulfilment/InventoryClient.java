package lab.testing.fulfilment;

import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Outbound client for the inventory service.
 *
 * <p>Design decisions worth noting:
 *
 * <ul>
 *   <li>The transport is the injected {@link RestClient}, which carries the connection policy
 *       (timeouts, interceptors) configured by the composition root; {@code baseUrl} identifies the
 *       inventory service instance the client targets.
 *   <li>{@link #lookup} returns a typed {@link InventoryResponse} and declares {@code Accept:
 *       application/json}. The request path and the JSON field names live here and nowhere else, so
 *       a change to the wire format is a one-line change.
 *   <li>The client is a boundary, so tests should verify it against a stub server (WireMock) rather
 *       than mocking it; a mock would never exercise the path, the header or the deserialization.
 * </ul>
 */
public final class InventoryClient {

    private final RestClient client;
    private final String baseUrl;

    public InventoryClient(RestClient client, String baseUrl) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    }

    /**
     * Looks up the current stock level for {@code sku}.
     *
     * @param sku the stock keeping unit; never {@code null}
     * @return the inventory response bound from the JSON body
     */
    public InventoryResponse lookup(String sku) {
        Objects.requireNonNull(sku, "sku must not be null");
        return client.get()
                .uri(baseUrl + "/inventory/{sku}", sku)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(InventoryResponse.class);
    }
}
