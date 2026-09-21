package lab.designpatterns.strategy;

import java.math.BigDecimal;

/**
 * Strategy contract for disparate payment methods. Enables the Open/Closed Principle: new payment
 * rails are introduced by adding new Spring beans implementing this interface without modifying
 * existing code.
 */
public interface PaymentStrategy {

    PaymentType getSupportedType();

    boolean processPayment(PaymentRequest request);

    BigDecimal calculateFee(PaymentRequest request);

    void refund(PaymentRequest request);
}
