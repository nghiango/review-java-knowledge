package lab.designpatterns.state;

public class DraftOrderState implements OrderState {

    @Override
    public String getStateName() {
        return "DRAFT";
    }

    @Override
    public void pay(OrderContext context) {
        context.setState(new PaidOrderState());
    }

    @Override
    public void ship(OrderContext context) {
        throw new IllegalStateException(
                "Cannot ship an unpaid draft order: " + context.getOrderId());
    }

    @Override
    public void cancel(OrderContext context) {
        context.setState(new CancelledOrderState());
    }
}
