package lab.designpatterns.strategy;

import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(CreditCardPaymentStrategy.class);
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.30");
    private static final BigDecimal PERCENT_RATE = new BigDecimal("0.029");

    @Override
    public PaymentType getSupportedType() {
        return PaymentType.CREDIT_CARD;
    }

    @Override
    public boolean processPayment(PaymentRequest request) {
        if (request.paymentDetails() == null || request.paymentDetails().length() < 16) {
            throw new IllegalArgumentException("Invalid credit card format");
        }
        log.info(
                "Processing Credit Card payment for {} using card ending in {}",
                request.amount(),
                request.paymentDetails().substring(request.paymentDetails().length() - 4));
        return true;
    }

    @Override
    public BigDecimal calculateFee(PaymentRequest request) {
        return request.amount().multiply(PERCENT_RATE).add(FIXED_FEE);
    }

    @Override
    public void refund(PaymentRequest request) {
        log.info(
                "Processing Credit Card charge reversal for transaction {}",
                request.transactionId());
    }
}
