package lab.testing.fulfilment;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Contract test for the inventory HTTP boundary.
 *
 * <p>The service is exercised against a real stub server rather than a mocked client, so the
 * request path, the {@code Accept} header and the JSON body that Jackson binds are all verified. If
 * the inventory API renames its endpoint or a field, this test fails instead of the mocks staying
 * green.
 */
class OrderFulfilmentWireMockIT {

    @RegisterExtension
    static WireMockExtension inventoryServer =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private OrderFulfilmentService service;

    @BeforeEach
    void setUp() {
        // A real RestClient with explicit timeouts: the stub server is a genuine network hop, and
        // an
        // unbounded request would let a stalled inventory service hang the caller.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(2));

        String baseUrl = inventoryServer.baseUrl();
        RestClient restClient =
                RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        service = new OrderFulfilmentService(new InventoryClient(restClient, baseUrl));
    }

    @Test
    @DisplayName("available stock equal to the requested quantity can be fulfilled")
    void canFulfil_availableEqualsRequested_returnsTrue() {
        stubAvailable("A-1", 3);

        assertThat(service.canFulfil("A-1", 3)).isTrue();

        inventoryServer.verify(
                getRequestedFor(urlEqualTo("/inventory/A-1"))
                        .withHeader("Accept", equalTo(MediaType.APPLICATION_JSON_VALUE)));
    }

    @Test
    @DisplayName("available stock below the requested quantity cannot be fulfilled")
    void canFulfil_availableBelowRequested_returnsFalse() {
        stubAvailable("A-1", 3);

        assertThat(service.canFulfil("A-1", 4)).isFalse();

        inventoryServer.verify(
                getRequestedFor(urlEqualTo("/inventory/A-1"))
                        .withHeader("Accept", equalTo(MediaType.APPLICATION_JSON_VALUE)));
    }

    private static void stubAvailable(String sku, int available) {
        inventoryServer.stubFor(
                get(urlEqualTo("/inventory/" + sku))
                        .willReturn(
                                aResponse()
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(
                                                "{\"sku\":\""
                                                        + sku
                                                        + "\",\"available\":"
                                                        + available
                                                        + "}")));
    }
}
