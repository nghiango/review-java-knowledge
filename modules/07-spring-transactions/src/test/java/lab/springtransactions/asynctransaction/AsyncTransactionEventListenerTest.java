package lab.springtransactions.asynctransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AsyncTransactionEventListenerTest {

    @Test
    @DisplayName("Should publish OrderCreatedEvent when order creation succeeds")
    void createOrder_validAmount_publishesEvent() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        OrderService orderService = new OrderService(publisher);

        orderService.createOrder("ord-501", "alice@example.com", 250.0);

        assertThat(orderService.getOrderAmount("ord-501")).isEqualTo(250.0);
        verify(publisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("Should throw exception and not publish event when order exceeds limit")
    void createOrder_excessiveAmount_throwsExceptionAndDoesNotPublish() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        OrderService orderService = new OrderService(publisher);

        assertThatThrownBy(() -> orderService.createOrder("ord-502", "bob@example.com", 50000.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit limit");

        assertThat(orderService.getOrderAmount("ord-502")).isNull();
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should record dispatched email when listener handles OrderCreatedEvent")
    void onOrderCreated_validEvent_recordsEmail() {
        OrderNotificationListener listener = new OrderNotificationListener();
        OrderCreatedEvent event = new OrderCreatedEvent("ord-503", "carol@example.com", 80.0);

        listener.onOrderCreated(event);

        assertThat(listener.getDispatchedEmails()).contains("carol@example.com:ord-503");
    }
}
