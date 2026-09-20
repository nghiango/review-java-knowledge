package lab.testing.questions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;

/**
 * Q14: WireMock stubbing and verification.
 *
 * <p>A stub is a *matching rule*: method, URL pattern, headers, body. A request that matches no
 * stub gets 404 with "Request was not matched", which is why a near miss — a case-different id, a
 * missing {@code Accept} header — fails loudly instead of returning an empty body. Verification is
 * the mirror image: it counts the recorded requests that match a request pattern, so {@code
 * verify(getRequestedFor(urlEqualTo(...)).withHeader("Accept", equalTo(...)))} is what proves the
 * client sent the request the contract describes. Nothing here starts a server: the extension
 * starts it around the test, and the pattern objects below are built without one.
 */
public class Q14WiremockStubbingAndVerification {

    private static final String SKU_PATH = "/inventory/A-1";
    private static final String NEAR_MISS_PATH = "/inventory/a-1";
    private static final String ACCEPT = MediaType.APPLICATION_JSON_VALUE;
    private static final String BODY = "{\"sku\":\"A-1\",\"available\":3}";

    /** The shape this module's {@code OrderFulfilmentWireMockIT} uses: a stub server per class. */
    static class InventoryStubServerTest {

        @RegisterExtension
        static final WireMockExtension inventoryServer =
                WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

        @Test
        @DisplayName("the client is verified against the path, the header and the body")
        void canFulfil_exactPathAndAcceptHeader_isVerified() {
            inventoryServer.stubFor(
                    get(urlEqualTo(SKU_PATH))
                            .withHeader("Accept", equalTo(ACCEPT))
                            .willReturn(
                                    aResponse().withHeader("Content-Type", ACCEPT).withBody(BODY)));

            inventoryServer.verify(
                    getRequestedFor(urlEqualTo(SKU_PATH)).withHeader("Accept", equalTo(ACCEPT)));
        }
    }

    /**
     * The same server managed by hand, for the cases where the extension lifecycle does not fit.
     */
    static final class ManualServer {

        static final WireMockServer INVENTORY = new WireMockServer(wireMockConfig().dynamicPort());

        private ManualServer() {}

        /** Starts and configures the server; a test calls this, the example never does. */
        static void startAndStub() {
            INVENTORY.start();
            INVENTORY.stubFor(
                    get(urlEqualTo(SKU_PATH))
                            .willReturn(aResponse().withStatus(200).withBody(BODY)));
        }
    }

    public static void main(String[] args) {
        // A stub is built without a server: build() is pure object construction.
        MappingBuilder builder =
                get(urlEqualTo(SKU_PATH))
                        .withHeader("Accept", equalTo(ACCEPT))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", ACCEPT)
                                        .withBody(BODY));
        StubMapping stub = builder.build();
        String stubUrl = stub.getRequest().getUrl(); // "/inventory/A-1"
        int stubStatus = stub.getResponse().getStatus(); // 200
        String stubBody = stub.getResponse().getBody(); // the JSON the client will bind

        // The url matcher decides what "the same path" means, so a near miss is not a match.
        UrlPattern urlMatcher = urlEqualTo(SKU_PATH);
        boolean exactPathMatches = urlMatcher.match(SKU_PATH).isExactMatch(); // true
        boolean nearMissMatches = urlMatcher.match(NEAR_MISS_PATH).isExactMatch(); // false

        RequestPattern verification = getRequestedFor(urlEqualTo(SKU_PATH)).build();
        String verifiedUrl = verification.getUrl(); // "/inventory/A-1"

        System.out.println("Stub URL: " + stubUrl); // Stub URL: /inventory/A-1
        System.out.println("Stub status: " + stubStatus); // Stub status: 200
        System.out.println("Stub body: " + stubBody); // Stub body: {"sku":"A-1","available":3}
        System.out.println("Exact path: " + exactPathMatches); // Exact path: true
        System.out.println("Near miss: " + nearMissMatches); // Near miss: false
        System.out.println("Verified URL: " + verifiedUrl); // Verified URL: /inventory/A-1
    }
}
