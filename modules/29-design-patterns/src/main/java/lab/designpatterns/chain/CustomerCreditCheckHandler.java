package lab.designpatterns.chain;

import java.math.BigDecimal;

public class CustomerCreditCheckHandler extends OrderValidationHandler {

    private static final BigDecimal MAX_UNVERIFIED_CREDIT = new BigDecimal("1000.00");

    @Override
    protected void doHandle(OrderValidationContext context) {
        if (context.getCustomerId().startsWith("UNVERIFIED-")
                && context.getOrderAmount().compareTo(MAX_UNVERIFIED_CREDIT) > 0) {
            context.addError("Order exceeds maximum credit limit for unverified customer");
        }
    }
}
