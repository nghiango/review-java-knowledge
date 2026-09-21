package lab.architecture.cleanarchitecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import lab.architecture.cleanarchitecture.application.service.OrderApplicationService;
import lab.architecture.cleanarchitecture.domain.model.Order;
import lab.architecture.cleanarchitecture.domain.model.OrderStatus;
import lab.architecture.cleanarchitecture.domain.port.in.PlaceOrderUseCase.ItemCommand;
import lab.architecture.cleanarchitecture.domain.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import lab.architecture.cleanarchitecture.domain.port.in.PlaceOrderUseCase.PlaceOrderResult;
import lab.architecture.cleanarchitecture.infrastructure.adapter.out.payment.MockPaymentAdapter;
import lab.architecture.cleanarchitecture.infrastructure.adapter.out.persistence.InMemoryOrderRepositoryAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderApplicationServiceTest {

    private InMemoryOrderRepositoryAdapter orderRepository;
    private MockPaymentAdapter paymentAdapter;
    private OrderApplicationService service;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepositoryAdapter();
        paymentAdapter = new MockPaymentAdapter(new BigDecimal("1000.00"));
        service = new OrderApplicationService(orderRepository, paymentAdapter);
    }

    @Test
    @DisplayName("placeOrder with valid items and payment token succeeds and marks order PAID")
    void placeOrder_validCommand_succeedsAndMarksPaid() {
        PlaceOrderCommand command =
                new PlaceOrderCommand(
                        "cust-123",
                        Currency.getInstance("USD"),
                        List.of(
                                new ItemCommand("PROD-1", 2, new BigDecimal("49.99")),
                                new ItemCommand("PROD-2", 1, new BigDecimal("20.00"))),
                        "valid-token-xyz");

        PlaceOrderResult result = service.placeOrder(command);

        assertThat(result.success()).isTrue();
        assertThat(result.orderId()).isNotNull();

        Order saved = orderRepository.findById(result.orderId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getTotalAmount().amount()).isEqualByComparingTo(new BigDecimal("119.98"));
    }

    @Test
    @DisplayName("placeOrder with rejected payment cancels order")
    void placeOrder_rejectedPayment_cancelsOrder() {
        PlaceOrderCommand command =
                new PlaceOrderCommand(
                        "cust-123",
                        Currency.getInstance("USD"),
                        List.of(new ItemCommand("PROD-1", 1, new BigDecimal("100.00"))),
                        "invalid-token");

        PlaceOrderResult result = service.placeOrder(command);

        assertThat(result.success()).isFalse();

        Order saved = orderRepository.findById(result.orderId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("placeOrder with negative quantity throws validation exception")
    void placeOrder_negativeQuantity_throwsException() {
        PlaceOrderCommand command =
                new PlaceOrderCommand(
                        "cust-123",
                        Currency.getInstance("USD"),
                        List.of(new ItemCommand("PROD-1", -5, new BigDecimal("10.00"))),
                        "token");

        assertThatThrownBy(() -> service.placeOrder(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }
}
