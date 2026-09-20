package lab.springtransactions.orderworkflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OrderWorkflowTest {

    @Test
    @DisplayName("Should process order successfully, persist status, and publish event")
    void processOrder_validOrder_completesSuccessfully() {
        OrderRepository repository = new OrderRepository();
        PaymentGatewayClient paymentClient = new PaymentGatewayClient();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        OrderProcessingCoordinator coordinator =
                new OrderProcessingCoordinator(repository, paymentClient, publisher);

        OrderWorkflowCommand command =
                new OrderWorkflowCommand("ord-701", "alice@example.com", "acc-valid-11", 500.0);
        OrderWorkflowResult result = coordinator.processOrder(command);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.transactionRef()).isNotNull();

        OrderRepository.Entity entity = repository.findById("ord-701");
        assertThat(entity).isNotNull();
        assertThat(entity.status()).isEqualTo("COMPLETED");
        assertThat(entity.paymentRef()).isEqualTo(result.transactionRef());

        verify(publisher).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    @DisplayName(
            "Should mark order FAILED and not publish completion event when payment is declined")
    void processOrder_declinedPayment_marksOrderFailed() {
        OrderRepository repository = new OrderRepository();
        PaymentGatewayClient paymentClient = new PaymentGatewayClient();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        OrderProcessingCoordinator coordinator =
                new OrderProcessingCoordinator(repository, paymentClient, publisher);

        OrderWorkflowCommand command =
                new OrderWorkflowCommand("ord-702", "bob@example.com", "declined-card", 150.0);
        OrderWorkflowResult result = coordinator.processOrder(command);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.transactionRef()).isNull();

        OrderRepository.Entity entity = repository.findById("ord-702");
        assertThat(entity).isNotNull();
        assertThat(entity.status()).startsWith("FAILED");

        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when amount is non-positive")
    void processOrder_nonPositiveAmount_throwsException() {
        OrderRepository repository = new OrderRepository();
        PaymentGatewayClient paymentClient = new PaymentGatewayClient();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        OrderProcessingCoordinator coordinator =
                new OrderProcessingCoordinator(repository, paymentClient, publisher);

        OrderWorkflowCommand command =
                new OrderWorkflowCommand("ord-703", "carol@example.com", "acc-valid", -50.0);

        assertThatThrownBy(() -> coordinator.processOrder(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");

        verify(publisher, never()).publishEvent(any());
    }
}
