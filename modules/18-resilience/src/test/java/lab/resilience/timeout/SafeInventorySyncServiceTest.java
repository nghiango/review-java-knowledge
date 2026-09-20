package lab.resilience.timeout;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SafeInventorySyncServiceTest {

    @Test
    @DisplayName("Should return UNAVAILABLE fallback when connection target fails")
    void checkStock_handlesDownstreamFailureGracefully() {
        // Point to a non-listening local port to trigger fast connection failure
        try (SafeInventorySyncService service =
                new SafeInventorySyncService(RestClient.builder(), "http://127.0.0.1:54321")) {
            SafeInventorySyncService.InventoryStatus status = service.checkStock("SKU-999");

            assertThat(status.sku()).isEqualTo("SKU-999");
            assertThat(status.availableQuantity()).isEqualTo(0);
            assertThat(status.status()).isEqualTo("UNAVAILABLE");
        }
    }
}
