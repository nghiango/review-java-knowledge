package lab.architecture.bdd;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Behavior-Driven Development (BDD) Executable Acceptance Specification.
 *
 * <p>Demonstrates the synergy between BDD and DDD in Hexagonal Architecture: - Gherkin
 * Given-When-Then steps map directly to Driving Inbound Ports and Aggregate Roots. - Acceptance
 * criteria run against the pure domain core in microseconds without framework containers.
 */
@DisplayName("Feature: Customer Order Placement & Payment")
class OrderPlacementBddTest {

    private InMemoryOrderRepositoryAdapter orderRepository;
    private MockPaymentAdapter paymentAdapter;
    private OrderApplicationService orderUseCase;

    @BeforeEach
    void setUpContext() {
        orderRepository = new InMemoryOrderRepositoryAdapter();
        paymentAdapter = new MockPaymentAdapter(new BigDecimal("5000.00"));
        orderUseCase = new OrderApplicationService(orderRepository, paymentAdapter);
    }

    @Nested
    @DisplayName("Scenario: Successful order placement with authorized payment")
    class SuccessfulOrderPlacement {

        private PlaceOrderCommand command;
        private PlaceOrderResult result;

        @Test
        @DisplayName(
                "Given a customer with 2 items, When checkout is submitted, Then order is PAID and persisted")
        void executeScenario() {
            givenACustomerWithCartItems();
            whenTheCustomerSubmitsCheckout();
            thenTheOrderShouldBePaidAndPersisted();
            andTheTotalAmountShouldMatchLineItems();
        }

        private void givenACustomerWithCartItems() {
            command =
                    new PlaceOrderCommand(
                            "customer-alpha",
                            Currency.getInstance("USD"),
                            List.of(
                                    new ItemCommand("LAPTOP-PRO", 1, new BigDecimal("1200.00")),
                                    new ItemCommand("WIRELESS-MOUSE", 2, new BigDecimal("25.00"))),
                            "valid-payment-token");
        }

        private void whenTheCustomerSubmitsCheckout() {
            result = orderUseCase.placeOrder(command);
        }

        private void thenTheOrderShouldBePaidAndPersisted() {
            assertThat(result.success()).isTrue();
            assertThat(result.orderId()).isNotNull();

            Order persisted = orderRepository.findById(result.orderId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        private void andTheTotalAmountShouldMatchLineItems() {
            Order persisted = orderRepository.findById(result.orderId()).orElseThrow();
            // 1 * 1200.00 + 2 * 25.00 = 1250.00
            assertThat(persisted.getTotalAmount().amount())
                    .isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(persisted.getItems()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Scenario: Order payment rejection due to credit card decline")
    class PaymentRejectionScenario {

        private PlaceOrderCommand command;
        private PlaceOrderResult result;

        @Test
        @DisplayName(
                "Given an invalid payment token, When checkout is submitted, Then order is CANCELLED")
        void executeScenario() {
            givenAnOrderWithAnInvalidPaymentToken();
            whenTheCustomerSubmitsCheckout();
            thenTheOrderPlacementFailsAndStatusIsCancelled();
        }

        private void givenAnOrderWithAnInvalidPaymentToken() {
            command =
                    new PlaceOrderCommand(
                            "customer-beta",
                            Currency.getInstance("USD"),
                            List.of(new ItemCommand("SMARTPHONE-Z", 1, new BigDecimal("800.00"))),
                            "invalid-token");
        }

        private void whenTheCustomerSubmitsCheckout() {
            result = orderUseCase.placeOrder(command);
        }

        private void thenTheOrderPlacementFailsAndStatusIsCancelled() {
            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("Payment failed");

            Order persisted = orderRepository.findById(result.orderId()).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
