package lab.designpatterns.state;

public class ShippedOrderState implements OrderState {

    @Override
    public String getStateName() {
        return "SHIPPED";
    }

    @Override
    public void pay(OrderContext context) {
        throw new IllegalStateException("Cannot pay for shipped order: " + context.getOrderId());
    }

    @Override
    public void ship(OrderContext context) {
        throw new IllegalStateException("Order is already shipped: " + context.getOrderId());
    }

    @Override
    public void cancel(OrderContext context) {
        throw new IllegalStateException(
                "Cannot cancel an order that has already shipped: " + context.getOrderId());
    }
}
