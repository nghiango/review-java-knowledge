package lab.java25boot4.springtransactions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CoordinatedOrderWorkflowServiceTest {

    private final CoordinatedOrderWorkflowService service = new CoordinatedOrderWorkflowService();

    @Test
    @DisplayName("Should confirm order when external verifications succeed")
    void processOrder_success_confirmsOrder() throws Throwable {
        var order = service.processOrderWithExternalVerification("ORD-001", 5000, false);
        assertThat(order.status()).isEqualTo("CONFIRMED");
        assertThat(order.amountCents()).isEqualTo(5000);
    }

    @Test
    @DisplayName("Should mark order CANCELLED when external verification fails")
    void processOrder_failure_cancelsOrder() {
        assertThatThrownBy(
                        () -> service.processOrderWithExternalVerification("ORD-002", 7000, true))
                .isInstanceOf(Throwable.class);

        var order = service.getOrder("ORD-002");
        assertThat(order).isNotNull();
        assertThat(order.status()).isEqualTo("CANCELLED");
    }
}
