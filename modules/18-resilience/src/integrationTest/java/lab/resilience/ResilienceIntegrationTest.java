package lab.resilience;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lab.resilience.idempotentretry.ChargeRequest;
import lab.resilience.idempotentretry.SafeBillingService;
import lab.resilience.timeout.SafeInventorySyncService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ResilienceIntegrationTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    @DisplayName(
            "SafeInventorySyncService handles delayed downstream response via TimeLimiter fallback")
    void inventorySync_recoversFromTimeoutViaFallback() {
        wireMockServer.stubFor(
                get(urlEqualTo("/api/v1/inventory/SKU-HANG"))
                        .willReturn(
                                aResponse()
                                        .withFixedDelay(3500) // greater than the 2s timeout
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"sku\":\"SKU-HANG\",\"availableQuantity\":50,\"status\":\"AVAILABLE\"}")));

        try (SafeInventorySyncService service =
                new SafeInventorySyncService(RestClient.builder(), wireMockServer.baseUrl())) {
            SafeInventorySyncService.InventoryStatus status = service.checkStock("SKU-HANG");

            assertThat(status.sku()).isEqualTo("SKU-HANG");
            assertThat(status.availableQuantity()).isEqualTo(0);
            assertThat(status.status()).isEqualTo("UNAVAILABLE");
        }
    }

    @Test
    @DisplayName("SafeBillingService reuses idempotency key on transient network failure")
    void billingService_attachesIdempotencyKeyAcrossRetries() {
        wireMockServer.stubFor(
                post(urlEqualTo("/api/v1/charges"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"chargeId\":\"ch_12345\",\"status\":\"CONFIRMED\"}")));

        SafeBillingService billingService =
                new SafeBillingService(RestClient.builder(), wireMockServer.baseUrl());
        ChargeRequest request = new ChargeRequest("cust-42", 99.99, "USD");

        SafeBillingService.ChargeConfirmation confirmation = billingService.chargeCustomer(request);

        assertThat(confirmation.chargeId()).isEqualTo("ch_12345");
        assertThat(confirmation.status()).isEqualTo("CONFIRMED");

        wireMockServer.verify(
                1,
                postRequestedFor(urlEqualTo("/api/v1/charges"))
                        .withHeader(
                                "Idempotency-Key",
                                com.github.tomakehurst.wiremock.client.WireMock.matching(".+")));
    }
}
