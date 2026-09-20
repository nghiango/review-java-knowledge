package lab.restapi.safemethods;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class SafeOrderControllerTest {

    private OrderService orderService;
    private SafeOrderController controller;

    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        controller = new SafeOrderController(orderService);
    }

    @Test
    @DisplayName("POST /orders creates order and returns 201 Created with Location header")
    void createOrder_validRequest_returns201WithLocation() {
        ResponseEntity<Order> response =
                controller.createOrder(new SafeOrderController.CreateOrderRequest("cust-123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().customerId()).isEqualTo("cust-123");
        assertThat(response.getBody().status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("GET /orders/{id} is safe and does not mutate order state")
    void getOrder_existingOrder_returnsOrderWithoutSideEffects() {
        Order created = orderService.createOrder("cust-123");

        ResponseEntity<Order> response1 = controller.getOrder(created.id());
        ResponseEntity<Order> response2 = controller.getOrder(created.id());

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody()).isEqualTo(created);
        assertThat(response2.getBody()).isEqualTo(created);
    }

    @Test
    @DisplayName("POST /orders/{id}/cancel mutates order to CANCELLED idempotently")
    void cancelOrder_existingOrder_transitionsToCancelled() {
        Order created = orderService.createOrder("cust-123");

        ResponseEntity<Order> response1 = controller.cancelOrder(created.id());
        ResponseEntity<Order> response2 = controller.cancelOrder(created.id());

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody().status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getBody().status()).isEqualTo(OrderStatus.CANCELLED);
    }
}
