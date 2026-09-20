package lab.restapi.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class IdempotentPaymentControllerTest {

    private IdempotentPaymentService service;
    private IdempotentPaymentController controller;

    @BeforeEach
    void setUp() {
        IdempotencyStorage storage = new InMemoryIdempotencyStorage();
        service = new IdempotentPaymentService(storage);
        controller = new IdempotentPaymentController(service);
    }

    @Test
    @DisplayName("First request with Idempotency-Key returns 201 Created and saves response")
    void charge_firstAttempt_returns201Created() {
        PaymentRequest request = new PaymentRequest("acc-1", new BigDecimal("100.00"), "USD");
        ResponseEntity<PaymentResponse> response = controller.charge("key-abc-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("false");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName(
            "Duplicate request with same Idempotency-Key returns 200 OK and same transaction ID")
    void charge_duplicateAttempt_returns200OkWithCachedResult() {
        PaymentRequest request = new PaymentRequest("acc-1", new BigDecimal("100.00"), "USD");
        ResponseEntity<PaymentResponse> first = controller.charge("key-xyz-789", request);
        ResponseEntity<PaymentResponse> second = controller.charge("key-xyz-789", request);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getHeaders().getFirst("Idempotency-Replayed")).isEqualTo("true");
        assertThat(second.getBody().transactionId()).isEqualTo(first.getBody().transactionId());
    }
}
