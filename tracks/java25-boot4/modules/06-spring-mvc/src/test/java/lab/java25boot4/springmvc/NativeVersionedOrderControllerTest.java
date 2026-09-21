package lab.java25boot4.springmvc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class NativeVersionedOrderControllerTest {

    private final NativeVersionedOrderController controller = new NativeVersionedOrderController();

    @Test
    @DisplayName("Should return V1 order format when querying V1 handler")
    void getOrderV1_returnsV1Payload() {
        ResponseEntity<NativeVersionedOrderController.OrderV1> response =
                controller.getOrderV1("ORD-101");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo("ORD-101");
        assertThat(response.getBody().status()).isEqualTo("PROCESSED_V1");
    }

    @Test
    @DisplayName("Should return V2 order format with amount and optional note")
    void getOrderV2_returnsV2Payload() {
        ResponseEntity<NativeVersionedOrderController.OrderV2> response =
                controller.getOrderV2("ORD-102", "Express Delivery");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo("ORD-102");
        assertThat(response.getBody().status()).isEqualTo("FULFILLED_V2");
        assertThat(response.getBody().amountCents()).isEqualTo(15000);
        assertThat(response.getBody().note()).isEqualTo("Express Delivery");
    }

    @Test
    @DisplayName("Should create order in V2 format")
    void createOrderV2_returnsCreatedOrder() {
        var request = new NativeVersionedOrderController.CreateOrderRequest("ITEM-77", 3);
        ResponseEntity<NativeVersionedOrderController.OrderV2> response =
                controller.createOrderV2(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo("ORD-ITEM-77");
        assertThat(response.getBody().amountCents()).isEqualTo(300);
    }
}
