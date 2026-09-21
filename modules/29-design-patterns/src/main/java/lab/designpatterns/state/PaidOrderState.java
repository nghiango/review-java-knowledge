package lab.designpatterns.state;

public class PaidOrderState implements OrderState {

    @Override
    public String getStateName() {
        return "PAID";
    }

    @Override
    public void pay(OrderContext context) {
        throw new IllegalStateException("Order is already paid: " + context.getOrderId());
    }

    @Override
    public void ship(OrderContext context) {
        context.setState(new ShippedOrderState());
    }

    @Override
    public void cancel(OrderContext context) {
        // In paid state, cancel issues a refund and transitions to cancelled
        context.setState(new CancelledOrderState());
    }
}
