package lab.designpatterns.chain;

public class FraudCheckHandler extends OrderValidationHandler {

    @Override
    protected void doHandle(OrderValidationContext context) {
        if (context.getCustomerId().contains("SUSPICIOUS")
                || context.getOrderId().contains("FRAUD")) {
            context.addError("Order blocked by automated fraud heuristic");
        }
    }
}
