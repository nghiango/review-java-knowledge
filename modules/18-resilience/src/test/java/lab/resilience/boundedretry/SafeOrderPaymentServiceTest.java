package lab.resilience.boundedretry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SafeOrderPaymentServiceTest {

    @Test
    @DisplayName("Should succeed on transient failure recovery within retry limit")
    void processPayment_recoversOnSecondAttempt() {
        OrderPaymentClient client = mock(OrderPaymentClient.class);
        when(client.charge("order-1", 100.0))
                .thenThrow(new SafeOrderPaymentService.TransientGatewayException("Temporary 503"))
                .thenReturn(new OrderPaymentClient.PaymentResponse("tx-99", "COMPLETED"));

        SafeOrderPaymentService service = new SafeOrderPaymentService(client);
        SafeOrderPaymentService.PaymentResult result = service.processPayment("order-1", 100.0);

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).isEqualTo("tx-99");
        verify(client, times(2)).charge("order-1", 100.0);
    }

    @Test
    @DisplayName("Should stop retrying after max attempts (3) on continuous transient failures")
    void processPayment_exhaustsMaxRetries() {
        OrderPaymentClient client = mock(OrderPaymentClient.class);
        when(client.charge("order-2", 200.0))
                .thenThrow(
                        new SafeOrderPaymentService.TransientGatewayException("503 Gateway Down"));

        SafeOrderPaymentService service = new SafeOrderPaymentService(client);

        assertThatThrownBy(() -> service.processPayment("order-2", 200.0))
                .isInstanceOf(SafeOrderPaymentService.TransientGatewayException.class)
                .hasMessageContaining("503 Gateway Down");

        verify(client, times(3)).charge("order-2", 200.0);
    }

    @Test
    @DisplayName("Should fail fast on non-retryable invalid payment exception")
    void processPayment_failsFastOnNonRetryable() {
        OrderPaymentClient client = mock(OrderPaymentClient.class);
        when(client.charge("order-3", 300.0))
                .thenThrow(
                        new SafeOrderPaymentService.InvalidPaymentException(
                                "402 Insufficient Funds"));

        SafeOrderPaymentService service = new SafeOrderPaymentService(client);

        assertThatThrownBy(() -> service.processPayment("order-3", 300.0))
                .isInstanceOf(SafeOrderPaymentService.InvalidPaymentException.class)
                .hasMessageContaining("402 Insufficient Funds");

        verify(client, times(1)).charge("order-3", 300.0);
    }
}
