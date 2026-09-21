package lab.designpatterns.state;

public class CancelledOrderState implements OrderState {

    @Override
    public String getStateName() {
        return "CANCELLED";
    }

    @Override
    public void pay(OrderContext context) {
        throw new IllegalStateException("Cannot pay for cancelled order: " + context.getOrderId());
    }

    @Override
    public void ship(OrderContext context) {
        throw new IllegalStateException("Cannot ship cancelled order: " + context.getOrderId());
    }

    @Override
    public void cancel(OrderContext context) {
        throw new IllegalStateException("Order is already cancelled: " + context.getOrderId());
    }
}
