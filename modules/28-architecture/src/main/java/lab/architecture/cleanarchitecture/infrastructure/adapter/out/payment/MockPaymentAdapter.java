package lab.architecture.cleanarchitecture.infrastructure.adapter.out.payment;

import java.math.BigDecimal;
import lab.architecture.cleanarchitecture.domain.model.Money;
import lab.architecture.cleanarchitecture.domain.model.OrderId;
import lab.architecture.cleanarchitecture.domain.port.out.PaymentPort;

/**
 * Infrastructure adapter implementing PaymentPort.
 * Simulates external payment gateway interaction.
 */
public class MockPaymentAdapter implements PaymentPort {

    private final BigDecimal maxAllowedAmount;

    public MockPaymentAdapter(BigDecimal maxAllowedAmount) {
        this.maxAllowedAmount = maxAllowedAmount;
    }

    public MockPaymentAdapter() {
        this(new BigDecimal("5000.00"));
    }

    @Override
    public PaymentResult processPayment(OrderId orderId, Money amount, String paymentToken) {
        if (paymentToken == null || paymentToken.isBlank() || paymentToken.equals("invalid-token")) {
            return PaymentResult.failure("Invalid payment token");
        }
        if (amount.amount().compareTo(maxAllowedAmount) > 0) {
            return PaymentResult.failure("Amount exceeds maximum limit: " + maxAllowedAmount);
        }
        return PaymentResult.success("TXN-" + orderId.value().toString().substring(0, 8));
    }
}
