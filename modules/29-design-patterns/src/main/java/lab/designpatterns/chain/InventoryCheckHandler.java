package lab.designpatterns.chain;

public class InventoryCheckHandler extends OrderValidationHandler {

    @Override
    protected void doHandle(OrderValidationContext context) {
        if (context.getOrderId().endsWith("-OUT-OF-STOCK")) {
            context.addError("Requested order items are currently out of stock");
        }
    }
}
