package lab.java25boot4.restapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

class NativeVersionedOrderApiControllerTest {

    private NativeVersionedOrderApiController controller;

    @BeforeEach
    void setUp() {
        controller = new NativeVersionedOrderApiController();
    }

    @Test
    @DisplayName("V1 endpoint returns deprecated headers and legacy format")
    void v1EndpointShouldReturnDeprecationHeaders() {
        ResponseEntity<NativeVersionedOrderApiController.OrderV1Representation> response =
                controller.getOrderV1ByHeader("ORD-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Deprecation")).isNotNull();
        assertThat(response.getHeaders().getFirst("Sunset")).isNotNull();
        assertThat(response.getHeaders().getFirst(HttpHeaders.LINK))
                .contains("rel=\"deprecation\"");

        NativeVersionedOrderApiController.OrderV1Representation body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.orderId()).isEqualTo("ORD-001");
        assertThat(body.totalAmount()).isEqualTo(99.50);
    }

    @Test
    @DisplayName("V2 endpoint returns modern schema and no sunset header")
    void v2EndpointShouldReturnModernRepresentation() {
        ResponseEntity<NativeVersionedOrderApiController.OrderV2Representation> response =
                controller.getOrderV2ByHeader("ORD-002");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().containsKey("Sunset")).isFalse();

        NativeVersionedOrderApiController.OrderV2Representation body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.orderId()).isEqualTo("ORD-002");
        assertThat(body.amountCents()).isEqualTo(9950L);
        assertThat(body.currency()).isEqualTo("USD");
    }

    @Test
    @DisplayName("Invalid version header produces RFC 9457 400 Bad Request ProblemDetail")
    void invalidVersionShouldProduceProblemDetail() {
        ResponseEntity<ProblemDetail> response =
                controller.handleInvalidVersionHeader("ORD-003", "99");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("Unsupported API Version");
        assertThat(problem.getProperties()).containsEntry("requestedVersion", "99");
    }

    @Test
    @DisplayName("POST create order returns 201 Created and Location header")
    void postCreateOrderShouldReturn201WithLocation() {
        var command =
                new NativeVersionedOrderApiController.CreateOrderCommand("CUST-99", 5000L, "USD");
        ResponseEntity<NativeVersionedOrderApiController.OrderV2Representation> response =
                controller.createOrderV2(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION))
                .startsWith("/api/orders/ORD-");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().amountCents()).isEqualTo(5000L);
    }
}
