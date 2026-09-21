package lab.designpatterns.decorator;

import java.util.Objects;

/** Decorator enforcing security and role-based access control before query execution. */
public class AuthorizingOrderQueryDecorator implements OrderQueryPort {

    private final OrderQueryPort delegate;

    public AuthorizingOrderQueryDecorator(OrderQueryPort delegate) {
        this.delegate = Objects.requireNonNull(delegate, "Delegate must not be null");
    }

    @Override
    public OrderSummary getOrderSummary(String orderId) {
        String role = SecurityContext.getRole();
        if (orderId.contains("CONFIDENTIAL") && !"ADMIN".equals(role)) {
            throw new SecurityException("Access denied to confidential order: " + orderId);
        }
        return delegate.getOrderSummary(orderId);
    }
}
