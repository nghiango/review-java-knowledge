package lab.designpatterns.state;

/** State Pattern interface for order lifecycle transitions. */
public interface OrderState {

    String getStateName();

    void pay(OrderContext context);

    void ship(OrderContext context);

    void cancel(OrderContext context);
}
