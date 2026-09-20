package lab.springtransactions.externalcall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExternalCallTransactionBoundaryTest {

    @Test
    @DisplayName("Should successfully checkout and mark order PAID when remote payment succeeds")
    void checkout_successfulPayment_marksOrderPaid() {
        OrderRepository repository = new OrderRepository();
        PaymentClient paymentClient = new PaymentClient();
        CheckoutOrchestrator orchestrator = new CheckoutOrchestrator(repository, paymentClient);

        orchestrator.checkout("ord-201", "acc-valid-88", 250.0);

        OrderRecord order = repository.findById("ord-201");
        assertThat(order).isNotNull();
        assertThat(order.status()).isEqualTo("PAID");
        assertThat(order.amount()).isEqualTo(250.0);
    }

    @Test
    @DisplayName("Should mark order PAYMENT_FAILED and throw exception when payment declined")
    void checkout_declinedPayment_marksOrderFailed() {
        OrderRepository repository = new OrderRepository();
        PaymentClient paymentClient = new PaymentClient();
        CheckoutOrchestrator orchestrator = new CheckoutOrchestrator(repository, paymentClient);

        assertThatThrownBy(() -> orchestrator.checkout("ord-202", "declined-account", 50.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment processing failed");

        OrderRecord order = repository.findById("ord-202");
        assertThat(order).isNotNull();
        assertThat(order.status()).isEqualTo("PAYMENT_FAILED");
    }
}
