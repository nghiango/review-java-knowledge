package lab.designpatterns.strategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StrategyPatternTest {

    private PaymentStrategyFactory factory;

    @BeforeEach
    void setUp() {
        PaymentStrategy creditCard = new CreditCardPaymentStrategy();
        PaymentStrategy payPal = new PayPalPaymentStrategy();
        factory = new PaymentStrategyFactory(List.of(creditCard, payPal));
    }

    @Test
    @DisplayName("Factory successfully resolves credit card strategy and processes payment")
    void getStrategy_creditCard_processesSuccessfully() {
        PaymentStrategy strategy = factory.getStrategy(PaymentType.CREDIT_CARD);
        PaymentRequest request =
                new PaymentRequest(
                        "TX-100",
                        "CUST-1",
                        new BigDecimal("100.00"),
                        PaymentType.CREDIT_CARD,
                        "4111222233334444");

        boolean result = strategy.processPayment(request);
        BigDecimal fee = strategy.calculateFee(request);

        assertThat(result).isTrue();
        assertThat(fee).isEqualByComparingTo("3.20"); // 100 * 0.029 + 0.30
    }

    @Test
    @DisplayName("Factory successfully resolves PayPal strategy and processes payment")
    void getStrategy_payPal_processesSuccessfully() {
        PaymentStrategy strategy = factory.getStrategy(PaymentType.PAYPAL);
        PaymentRequest request =
                new PaymentRequest(
                        "TX-200",
                        "CUST-2",
                        new BigDecimal("50.00"),
                        PaymentType.PAYPAL,
                        "buyer@example.com");

        boolean result = strategy.processPayment(request);
        BigDecimal fee = strategy.calculateFee(request);

        assertThat(result).isTrue();
        assertThat(fee).isEqualByComparingTo("2.05"); // 50 * 0.034 + 0.35
    }

    @Test
    @DisplayName(
            "Factory throws UnsupportedOperationException when requesting unregistered strategy")
    void getStrategy_unregisteredType_throwsException() {
        assertThatThrownBy(() -> factory.getStrategy(PaymentType.CRYPTO))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("No payment strategy registered for type: CRYPTO");
    }
}
