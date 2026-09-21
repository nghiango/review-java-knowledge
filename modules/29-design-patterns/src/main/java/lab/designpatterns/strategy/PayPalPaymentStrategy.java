package lab.designpatterns.strategy;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PayPalPaymentStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(PayPalPaymentStrategy.class);
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.35");
    private static final BigDecimal PERCENT_RATE = new BigDecimal("0.034");

    @Override
    public PaymentType getSupportedType() {
        return PaymentType.PAYPAL;
    }

    @Override
    public boolean processPayment(PaymentRequest request) {
        if (!request.paymentDetails().contains("@")) {
            throw new IllegalArgumentException("Invalid PayPal email: " + request.paymentDetails());
        }
        log.info(
                "Processing PayPal payment of {} for account {}",
                request.amount(),
                request.paymentDetails());
        return true;
    }

    @Override
    public BigDecimal calculateFee(PaymentRequest request) {
        return request.amount().multiply(PERCENT_RATE).add(FIXED_FEE);
    }

    @Override
    public void refund(PaymentRequest request) {
        log.info("Triggering PayPal API refund for transaction {}", request.transactionId());
    }
}
