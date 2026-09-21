package lab.designpatterns.state;

import java.util.Objects;

/** Context holding the active state and delegating lifecycle actions. */
public class OrderContext {

    private final String orderId;
    private OrderState currentState;

    public OrderContext(String orderId) {
        this.orderId = Objects.requireNonNull(orderId, "orderId must not be null");
        this.currentState = new DraftOrderState();
    }

    public void setState(OrderState state) {
        this.currentState = Objects.requireNonNull(state, "state must not be null");
    }

    public OrderState getCurrentState() {
        return currentState;
    }

    public String getOrderId() {
        return orderId;
    }

    public void pay() {
        currentState.pay(this);
    }

    public void ship() {
        currentState.ship(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }
}
