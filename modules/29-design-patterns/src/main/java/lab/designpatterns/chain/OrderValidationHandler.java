package lab.designpatterns.chain;

/**
 * Handler in Chain of Responsibility. Dispatches checks sequentially and supports short-circuiting
 * when validation fails.
 */
public abstract class OrderValidationHandler {

    private OrderValidationHandler next;

    public OrderValidationHandler setNext(OrderValidationHandler next) {
        this.next = next;
        return next;
    }

    public void handle(OrderValidationContext context) {
        doHandle(context);
        if (context.isValid() && next != null) {
            next.handle(context);
        }
    }

    protected abstract void doHandle(OrderValidationContext context);
}
