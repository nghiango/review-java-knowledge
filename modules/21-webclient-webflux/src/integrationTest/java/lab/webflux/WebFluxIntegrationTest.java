package lab.webflux;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import java.util.List;
import lab.webflux.blockinflow.CorrectReactivePaymentGateway;
import lab.webflux.flatmapconcurrency.CorrectNotificationBatchSender;
import lab.webflux.missingtimeout.CorrectCarrierTrackingClient;
import lab.webflux.noerrorhandler.CorrectCartPricingEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class WebFluxIntegrationTest {

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
            "CorrectReactivePaymentGateway processes valid token and executes charge asynchronously")
    void paymentGateway_completesChargeFlowNonBlocking() {
        wireMockServer.stubFor(
                get(urlEqualTo("/api/tokens/tok-123"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"accountId\":\"acc-456\",\"valid\":true}")));

        wireMockServer.stubFor(
                post(urlEqualTo("/api/charges"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"chargeId\":\"ch-789\",\"status\":\"SUCCESS\"}")));

        CorrectReactivePaymentGateway gateway =
                new CorrectReactivePaymentGateway(WebClient.builder(), wireMockServer.baseUrl());

        StepVerifier.create(
                        gateway.processPayment(
                                new CorrectReactivePaymentGateway.PaymentRequest(
                                        "ord-100", "tok-123", 49.99)))
                .assertNext(
                        result -> {
                            assertThat(result.orderId()).isEqualTo("ord-100");
                            assertThat(result.status()).isEqualTo("CONFIRMED");
                            assertThat(result.details()).isEqualTo("ch-789");
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("CorrectCarrierTrackingClient recovers from slow response via timeout fallback")
    void carrierTracking_timesOutAndDegrades() {
        wireMockServer.stubFor(
                get(urlEqualTo("/shipments/TRK-SLOW"))
                        .willReturn(
                                aResponse()
                                        .withFixedDelay(1500)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"trackingNumber\":\"TRK-SLOW\",\"status\":\"DELIVERED\",\"eta\":\"TODAY\"}")));

        CorrectCarrierTrackingClient client =
                new CorrectCarrierTrackingClient(
                        WebClient.builder(), wireMockServer.baseUrl(), Duration.ofMillis(300));

        StepVerifier.create(client.trackShipment("TRK-SLOW"))
                .assertNext(
                        info -> {
                            assertThat(info.trackingNumber()).isEqualTo("TRK-SLOW");
                            assertThat(info.status()).isEqualTo("UNAVAILABLE");
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName(
            "CorrectCartPricingEngine combines partial discounts when one service returns 500 error")
    void cartPricing_combinesPartialDiscountsOnPartialFailure() {
        // Loyalty returns 10.0 discount
        wireMockServer.stubFor(
                get(urlEqualTo("/loyalty/user-1/discount"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(
                                                "{\"userId\":\"user-1\",\"discountAmount\":10.0}")));

        // Coupon service fails with 500 Internal Server Error
        wireMockServer.stubFor(
                get(urlEqualTo("/coupons/user-1/best"))
                        .willReturn(aResponse().withStatus(500).withBody("Downstream failure")));

        CorrectCartPricingEngine engine =
                new CorrectCartPricingEngine(
                        WebClient.builder(), wireMockServer.baseUrl(), wireMockServer.baseUrl());

        StepVerifier.create(engine.calculateCartPrice("user-1", 100.0))
                .assertNext(
                        price -> {
                            assertThat(price.userId()).isEqualTo("user-1");
                            assertThat(price.originalTotal()).isEqualTo(100.0);
                            assertThat(price.discount()).isEqualTo(10.0);
                            assertThat(price.finalTotal()).isEqualTo(90.0);
                        })
                .verifyComplete();
    }

    @Test
    @DisplayName("CorrectNotificationBatchSender processes batch concurrently through WireMock")
    void notificationBatch_dispatchesConcurrently() {
        wireMockServer.stubFor(
                post(urlEqualTo("/api/notify"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"userId\":\"u-1\",\"delivered\":true}")));

        CorrectNotificationBatchSender sender =
                new CorrectNotificationBatchSender(WebClient.builder(), wireMockServer.baseUrl());

        List<CorrectNotificationBatchSender.NotificationMessage> messages =
                List.of(
                        new CorrectNotificationBatchSender.NotificationMessage(
                                "u-1", "Notice", "Body"));

        StepVerifier.create(sender.sendBatch(messages, 4))
                .assertNext(
                        res -> {
                            assertThat(res.userId()).isEqualTo("u-1");
                            assertThat(res.delivered()).isTrue();
                        })
                .verifyComplete();
    }
}
